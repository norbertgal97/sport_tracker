package hu.bme.aut.sporttracker.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import hu.bme.aut.sporttracker.MainActivity
import hu.bme.aut.sporttracker.R
import hu.bme.aut.sporttracker.location.LocationHelper
import android.widget.Chronometer
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.ActivityRecognition


class LocationService : Service(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    companion object {
        const val BR_NEW_LOCATION = "BR_NEW_LOCATION"
        const val KEY_DISTANCE = "KEY_DISTANCE"
        const val KEY_CALORIES = "KEY_CALORIES"
        private const val NOTIFICATION_ID = 101
        var isRunning = false
        var elapsedTime: Long = -1L
    }

    private var locationHelper: LocationHelper? = null
    private var distanceWalked: Float = 0f
    private var caloriesBurned: Float = 0f
    private var durationFirebase: Long = 0L
    var distanceSum: Float = 0f
    var caloriesSum: Float = 0f
    var speed: Float = 0f
    var timeStartCalories: Long = 0L

    var lastLocation: Location? = null
        private set
    lateinit var mChronometer: Chronometer
    private var mApiClient: GoogleApiClient? = null
    private var currentActivity: String = "UNKNOWN"

    private val activityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            currentActivity = intent.getStringExtra(ActivityRecognitionService.KEY_ACTIVITY)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        LocalBroadcastManager.getInstance(applicationContext)
            .registerReceiver(activityReceiver, IntentFilter(ActivityRecognitionService.BR_NEW_ACTIVITY))
        mApiClient = GoogleApiClient.Builder(this)
            .addApi(ActivityRecognition.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()
        mApiClient?.connect()

        elapsedTime = SystemClock.elapsedRealtime()
        mChronometer = Chronometer(this)
        mChronometer.base = elapsedTime
        timeStartCalories = mChronometer.base
        mChronometer.start()

        if (locationHelper == null) {
            val helper = LocationHelper(applicationContext, LocationServiceCallback())
            helper.startLocationMonitoring()
            locationHelper = helper
            loadData()
        }

        startForeground(NOTIFICATION_ID, createNotification("Starting location service..."))
        isRunning = true

        return Service.START_STICKY
    }

    override fun onDestroy() {
        locationHelper?.stopLocationMonitoring()
        saveDistance(distanceSum, distanceWalked)
        saveCalories(caloriesSum, caloriesBurned)
        saveDuration(SystemClock.elapsedRealtime() - elapsedTime, durationFirebase)
        mChronometer.stop()
        elapsedTime = -1
        isRunning = false

        LocalBroadcastManager.getInstance(applicationContext)
            .unregisterReceiver(activityReceiver)
        mApiClient?.disconnect()

        super.onDestroy()
    }

    inner class LocationServiceCallback : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val distance = lastLocation?.distanceTo(location)
            if (distance != null)
                distanceSum += distance

            updateNotification("Distance: $distanceSum")
            lastLocation = location

            val savedElapsedTime = SystemClock.elapsedRealtime()
            if (distance != null)
                speed = calculateSpeed(timeStartCalories, savedElapsedTime, distance)

            val calories = calculateCalories(timeStartCalories, savedElapsedTime)
            caloriesSum += calories

            timeStartCalories = savedElapsedTime

            val intent = Intent()
            intent.action = BR_NEW_LOCATION
            intent.putExtra(KEY_DISTANCE, distanceSum.toLong())
            intent.putExtra(KEY_CALORIES, caloriesSum.toLong())
            LocalBroadcastManager.getInstance(this@LocationService).sendBroadcast(intent)
        }

        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            updateNotification("Location available: ${locationAvailability.isLocationAvailable}")
        }
    }


    private fun saveDuration(duration: Long, durationFirebase: Long) {
        val reference = FirebaseDatabase.getInstance()
            .getReference("users/" + FirebaseAuth.getInstance().currentUser!!.uid + "/Activity")

        reference.child("duration").setValue(duration + durationFirebase)
    }

    private fun saveDistance(distance: Float, distanceFirebase: Float) {
        val reference = FirebaseDatabase.getInstance()
            .getReference("users/" + FirebaseAuth.getInstance().currentUser!!.uid + "/Activity")

        reference.child("distance").setValue((distance + distanceFirebase).toLong())
    }

    private fun saveCalories(calories: Float, caloriesFirebase: Float) {
        val reference = FirebaseDatabase.getInstance()
            .getReference("users/" + FirebaseAuth.getInstance().currentUser!!.uid + "/Activity")

        reference.child("calories").setValue((calories + caloriesFirebase).toLong())
    }

    private fun loadData() {
        val databaseUser: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")
        databaseUser.child(FirebaseAuth.getInstance().currentUser!!.uid).child("Activity")
            .addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        distanceWalked = dataSnapshot.child("distance").value.toString().toFloat()
                        caloriesBurned = dataSnapshot.child("calories").value.toString().toFloat()
                        durationFirebase = dataSnapshot.child("duration").value.toString().toLong()
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(applicationContext, databaseError.message, Toast.LENGTH_LONG).show()
                    }
                })
    }

    private fun calculateCalories(timeStart: Long, timeEnd: Long): Float {
        val timeInHour = (timeEnd.toFloat() - timeStart.toFloat()) / 1000f / 60f / 60f
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        val weight = try {
            sharedPreferences.getString("weight", "0")!!.toInt()
        } catch (e: NullPointerException) {
            0
        }
        return weight * calculateMET(speed) * timeInHour
    }

    private fun calculateSpeed(timeStart: Long, timeEnd: Long, distance: Float): Float {
        return distance / ((timeEnd - timeStart).toFloat() / 1000f)
    }

    private fun calculateMET(speedMpS: Float): Float {
        val speedKpH = speedMpS * 3.6f
        var MET: Float = 0f
        Log.e("calculateMET", currentActivity)
        when (currentActivity) {
            "ON_FOOT" -> MET = 2.3f
            "WALKING" -> when {
                speedKpH >= 4.8f -> MET = 3.3f
                speedKpH >= 4f -> MET = 2.9f
                speedKpH >= 2.7f -> MET = 2.3f
            }
            "RUNNING" -> when {
                speedKpH > 16f -> MET = 16f
                speedKpH >= 11f -> MET = 11.2f
                speedKpH >= 9f -> MET = 8.8f
                speedKpH > 6.4f -> MET = 6f
            }
            else -> MET = 0f
        }
        return MET
    }

    private fun createNotification(text: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK

        val contentIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            notificationIntent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )

        return NotificationCompat.Builder(this, "default")
            .setContentTitle("Location Service")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(contentIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onConnected(p0: Bundle?) {
        val intent = Intent(this, ActivityRecognitionService::class.java)
        val pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, 3000, pendingIntent)
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.e("ActivityRecogition", "Connection Suspended")
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.e("ActivityRecogition", "Connection Failed")
    }
}