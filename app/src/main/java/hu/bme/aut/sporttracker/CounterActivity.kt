package hu.bme.aut.sporttracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.content.LocalBroadcastManager
import hu.bme.aut.sporttracker.service.CounterService
import hu.bme.aut.sporttracker.service.LocationService
import kotlinx.android.synthetic.main.activity_counter.*

class CounterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_counter)
        counter_layout.background.alpha = 60

        if (LocationService.isRunning) {
            start_stop_service.text = "STOP"
        } else {
            start_stop_service.text = "START"
        }

        if (LocationService.elapsedTime != -1L) {
            cTime.base = LocationService.elapsedTime
            cTime.start()
        }

        val intent1 = Intent(applicationContext, LocationService::class.java)
        val intent2 = Intent(applicationContext, CounterService::class.java)

        start_stop_service.setOnClickListener {
            if (LocationService.isRunning) {
                applicationContext.stopService(intent1)
                applicationContext.stopService(intent2)
                start_stop_service.text = "START"
                cTime.stop()
            } else {
                applicationContext.startService(intent1)
                applicationContext.startService(intent2)
                start_stop_service.text = "STOP"
                cTime.base = SystemClock.elapsedRealtime()
                cTime.start()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(applicationContext)
            .registerReceiver(locationReceiver, IntentFilter(LocationService.BR_NEW_LOCATION))
        LocalBroadcastManager.getInstance(applicationContext)
            .registerReceiver(stepReceiver, IntentFilter(CounterService.BR_NEW_STEP))
    }

    override fun onStop() {
        LocalBroadcastManager.getInstance(applicationContext)
            .unregisterReceiver(locationReceiver)
        LocalBroadcastManager.getInstance(applicationContext)
            .unregisterReceiver(stepReceiver)
        super.onStop()
    }

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val currentDistance = intent.getLongExtra(LocationService.KEY_DISTANCE, 0L)
            tvDistanceCounter.text = "$currentDistance m"
            val currentCalories = intent.getLongExtra(LocationService.KEY_CALORIES, 0L)
            tvCalorieCounter.text = "$currentCalories kcal"
        }
    }

    private val stepReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val currentSteps = intent.getFloatExtra(CounterService.KEY_STEPS, 0f)
            tvStepCounter.text = currentSteps.toString()
        }
    }
}