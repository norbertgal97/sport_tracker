package hu.bme.aut.sporttracker.service


import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import hu.bme.aut.sporttracker.MainActivity
import hu.bme.aut.sporttracker.R


class CounterService : Service(), SensorEventListener {
    companion object {
        const val BR_NEW_STEP = "BR_NEW_STEP"
        const val KEY_STEPS = "KEY_STEPS"
        private const val NOTIFICATION_ID = 102
    }

    private lateinit var sensorManager: SensorManager
    private var initialStep: Float = -1f
    private var steps: Float = 0f
    private var stepsTakenFirebase: Float = 0f

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val countSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_NORMAL)
        startForeground(NOTIFICATION_ID, createNotification("Starting counter service..."))
        loadSteps()

        return Service.START_STICKY
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        saveSteps(steps, stepsTakenFirebase)
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (initialStep == -1f)
            initialStep = event!!.values[0]

        try {
            steps = event!!.values[0] - initialStep
            val intent = Intent()
            intent.action = BR_NEW_STEP
            intent.putExtra(KEY_STEPS, steps)
            LocalBroadcastManager.getInstance(this@CounterService).sendBroadcast(intent)
            updateNotification("Steps: $steps")
        } catch (e: NullPointerException) {
            Log.e("SensorEvent", "SensorEvent is null")
        }
    }

    private fun saveSteps(steps: Float, stepsTaken: Float) {
        val reference = FirebaseDatabase.getInstance()
            .getReference("users/" + FirebaseAuth.getInstance().currentUser!!.uid + "/Activity")

        reference.child("steps").setValue(steps + stepsTaken)
    }

    private fun loadSteps() {
        val databaseUser: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")
        databaseUser.child(FirebaseAuth.getInstance().currentUser!!.uid).child("Activity")
            .addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        stepsTakenFirebase = dataSnapshot.child("steps").value.toString().toFloat()
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(applicationContext, databaseError.message, Toast.LENGTH_LONG).show()
                    }
                })
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
            .setContentTitle("Counter Service")
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

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}