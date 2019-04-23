package hu.bme.aut.sporttracker.service

import android.content.Intent
import android.app.IntentService
import android.support.v4.content.LocalBroadcastManager
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import android.util.Log


class ActivityRecognitionService(name: String?) : IntentService("ActivityRecognition") {
    constructor() : this("Recogniton")

    companion object {
        const val BR_NEW_ACTIVITY = "BR_NEW_ACTIVITY"
        const val KEY_ACTIVITY = "KEY_ACTIVITY"
    }

    override fun onHandleIntent(intent: Intent?) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent)
            val activity = handleDetectedActivities(result.probableActivities)
            val activityIntent = Intent()
            activityIntent.action = BR_NEW_ACTIVITY
            activityIntent.putExtra(KEY_ACTIVITY, activity)
            LocalBroadcastManager.getInstance(this@ActivityRecognitionService).sendBroadcast(activityIntent)
        }
    }

    private fun handleDetectedActivities(probableActivities: List<DetectedActivity>): String {
        for (activity in probableActivities) {
            when (activity.type) {
                DetectedActivity.IN_VEHICLE -> {
                    Log.e("ActivityRecogition", "In Vehicle: " + activity.confidence)
                    if (activity.confidence >= 75)
                        return "IN_VEHICLE"
                }
                DetectedActivity.ON_BICYCLE -> {
                    Log.e("ActivityRecogition", "On Bicycle: " + activity.confidence)
                    if (activity.confidence >= 75)
                        return "ON_BICYCLE"
                }
                DetectedActivity.ON_FOOT -> {
                    Log.e("ActivityRecogition", "On Foot: " + activity.confidence)
                    if (activity.confidence >= 75)
                        return "ON_FOOT"
                }
                DetectedActivity.RUNNING -> {
                    Log.e("ActivityRecogition", "Running: " + activity.confidence)
                    if (activity.confidence >= 75)
                        return "RUNNING"
                }
                DetectedActivity.STILL -> {
                    Log.e("ActivityRecogition", "Still: " + activity.confidence)
                    if (activity.confidence >= 75)
                        return "STILL"
                }
                DetectedActivity.TILTING -> {
                    Log.e("ActivityRecogition", "Tilting: " + activity.confidence)
                    if (activity.confidence >= 75)
                        return "TILTING"
                }
                DetectedActivity.WALKING -> {
                    Log.e("ActivityRecogition", "Walking: " + activity.confidence)
                    if (activity.confidence >= 75)
                        return "WALKING"
                }
                DetectedActivity.UNKNOWN -> {
                    Log.e("ActivityRecogition", "Unknown: " + activity.confidence)
                    if (activity.confidence >= 75)
                        return "UNKNOWN"
                }
            }
        }
        return "UNKNOWN"
    }
}