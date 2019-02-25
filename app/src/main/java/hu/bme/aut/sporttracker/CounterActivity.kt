package hu.bme.aut.sporttracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import com.github.mikephil.charting.data.PieEntry
import android.support.v4.content.ContextCompat
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.charts.PieChart

class CounterActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var chartStep: PieChart
    private var goal: Float = 500f
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_counter)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        chartStep = findViewById(R.id.chartSteps)
        chartStep.description.isEnabled = false

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        goal = try {
            sharedPreferences.getString("goal", "500")!!.toFloat()
        } catch (e: NullPointerException) {
            500f
        }
    }

    override fun onResume() {
        super.onResume()
        val countSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        try {
            loadData(event!!.values[0])
        } catch (e: NullPointerException) {
            Log.e("SensorEvent", "SensorEvent is null")
        }
    }

    private fun loadData(steps: Float) {
        val entries: MutableList<PieEntry> = mutableListOf()
        entries.add(PieEntry(steps, "Steps"))

        val diff: Float = goal - steps
        if (diff > 0) {
            entries.add(PieEntry(goal - steps, "Remaining"))
        } else {
            entries.add(PieEntry(0f, "Remaining"))
        }


        val dataSet = PieDataSet(entries, "steps")
        dataSet.colors = mutableListOf(
            ContextCompat.getColor(this, R.color.colorPrimary),
            ContextCompat.getColor(this, R.color.colorAccent)
        )

        val data = PieData(dataSet)
        data.setDrawValues(false)

        chartStep.data = data
        chartStep.centerText = steps.toInt().toString() + "\n" + "/" + goal.toInt().toString() + " steps"
        chartStep.invalidate()
    }
}