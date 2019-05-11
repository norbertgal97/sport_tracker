package hu.bme.aut.sporttracker

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_weekly_summary.*
import java.util.*

class WeeklySummaryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekly_summary)
        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val reference = FirebaseDatabase.getInstance()
            .getReference("users/" + FirebaseAuth.getInstance().currentUser!!.uid + "/Week")
        val databaseUser: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")

        databaseUser.child(FirebaseAuth.getInstance().currentUser!!.uid).child("Week")
            .addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {

                        val calendar = Calendar.getInstance()

                        if (dataSnapshot.child("week").value.toString().toInt() < calendar.get(Calendar.WEEK_OF_YEAR) ||
                            calendar.get(Calendar.YEAR) > dataSnapshot.child("year").value.toString().toInt()
                        ) {
                            reference.child("monday").setValue(0)
                            reference.child("tuesday").setValue(0)
                            reference.child("wednesday").setValue(0)
                            reference.child("thursday").setValue(0)
                            reference.child("friday").setValue(0)
                            reference.child("saturday").setValue(0)
                            reference.child("sunday").setValue(0)

                            tvMondayStep.text = getString(R.string.zero)
                            tvTuesdayStep.text = getString(R.string.zero)
                            tvWednesdayStep.text = getString(R.string.zero)
                            tvThursdayStep.text = getString(R.string.zero)
                            tvFridayStep.text = getString(R.string.zero)
                            tvSaturdayStep.text = getString(R.string.zero)
                            tvSundayStep.text = getString(R.string.zero)
                            tvWeeklySummaryWeek.text = getString(R.string.zero)
                            loadStepsData(0, 0, 0, 0, 0, 0, 0)
                        } else {
                            tvMondayStep.text = dataSnapshot.child("monday").value.toString() + " steps"
                            tvTuesdayStep.text = dataSnapshot.child("tuesday").value.toString() + " steps"
                            tvWednesdayStep.text = dataSnapshot.child("wednesday").value.toString() + " steps"
                            tvThursdayStep.text = dataSnapshot.child("thursday").value.toString() + " steps"
                            tvFridayStep.text = dataSnapshot.child("friday").value.toString() + " steps"
                            tvSaturdayStep.text = dataSnapshot.child("saturday").value.toString() + " steps"
                            tvSundayStep.text = dataSnapshot.child("sunday").value.toString() + " steps"
                            tvWeeklySummaryWeek.text = dataSnapshot.child("week").value.toString() + ". week"

                            loadStepsData(
                                dataSnapshot.child("monday").value.toString().toInt(),
                                dataSnapshot.child("tuesday").value.toString().toInt(),
                                dataSnapshot.child("wednesday").value.toString().toInt(),
                                dataSnapshot.child("thursday").value.toString().toInt(),
                                dataSnapshot.child("friday").value.toString().toInt(),
                                dataSnapshot.child("saturday").value.toString().toInt(),
                                dataSnapshot.child("sunday").value.toString().toInt()
                            )
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(applicationContext, databaseError.message, Toast.LENGTH_LONG).show()
                    }
                })
    }

    private fun loadStepsData(m: Int, t: Int, w: Int, th: Int, f: Int, sa: Int, su: Int) {
        val entries: MutableList<BarEntry> = mutableListOf()

        entries.add(BarEntry(0f, m.toFloat()))
        entries.add(BarEntry(1f, t.toFloat()))
        entries.add(BarEntry(2f, w.toFloat()))
        entries.add(BarEntry(3f, th.toFloat()))
        entries.add(BarEntry(4f, f.toFloat()))
        entries.add(BarEntry(5f, sa.toFloat()))
        entries.add(BarEntry(6f, su.toFloat()))


        val dataSet = BarDataSet(entries, "Steps")

        val data = BarData(dataSet)
        data.setDrawValues(false)

        val days = mutableListOf("M", "T", "W", "T", "F", "S", "S")
        chartWeeklySteps.data = data
        chartWeeklySteps.description.isEnabled = false

        val xAxis: XAxis = chartWeeklySteps.xAxis
        xAxis.valueFormatter = MyXAxisValueFormatter(days)
        chartWeeklySteps.axisRight.isEnabled = false
        chartWeeklySteps.invalidate()
    }

    class MyXAxisValueFormatter(values: MutableList<String>) : IAxisValueFormatter {
        private var mValues: MutableList<String> = values

        override fun getFormattedValue(value: Float, axis: AxisBase?): String {
            return mValues[value.toInt()]
        }
    }
}
