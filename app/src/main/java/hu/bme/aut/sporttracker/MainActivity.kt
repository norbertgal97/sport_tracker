package hu.bme.aut.sporttracker

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.text.DateFormat
import java.util.*
import java.text.SimpleDateFormat


class MainActivity : AppCompatActivity() {
    private lateinit var chartStep: PieChart
    private var goal: Float = 500f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        main_layout.background.alpha = 60

        chartStep = findViewById(R.id.chartSteps)
        chartStep.description.isEnabled = false

        loadData()
        fab.setOnClickListener {
            val intent = Intent(this, CounterActivity::class.java)
            startActivity(intent)
        }

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        goal = try {
            sharedPreferences.getString("goal", "500")!!.toFloat()
        } catch (e: NullPointerException) {
            500f
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
        if (!FirebaseAuth.getInstance().currentUser!!.isEmailVerified)
            Toast.makeText(this, "Verify your account!", Toast.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                val intentSettings = Intent(this, SettingsActivity::class.java)
                startActivity(intentSettings)
            }
            R.id.sign_out -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
        return true
    }

    private fun loadStepsData(steps: Float) {
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

    private fun loadData() {
        val reference = FirebaseDatabase.getInstance()
            .getReference("users/" + FirebaseAuth.getInstance().currentUser!!.uid + "/Activity")
        val databaseUser: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")

        databaseUser.child(FirebaseAuth.getInstance().currentUser!!.uid).child("Activity")
            .addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val date = DateFormat.getDateInstance(DateFormat.SHORT).format(Calendar.getInstance().time)

                        if (dataSnapshot.child("date").value.toString() != date) {
                            reference.child("date").setValue(date)
                            reference.child("calories").setValue(0)
                            reference.child("distance").setValue(0)
                            reference.child("duration").setValue(0)
                            reference.child("steps").setValue(0)
                            tvSummaryDate.text = date
                            tvDistanceCounter.text = getString(R.string.zero)
                            tvCalorieCounter.text = getString(R.string.zero)
                            tvTimeDuration.text = getString(R.string.zero)
                            loadStepsData(0f)
                        } else {
                            val formatter = SimpleDateFormat("HH:mm:ss", Locale.US)
                            formatter.timeZone = TimeZone.getTimeZone("UTC+1")
                            tvTimeDuration.text =
                                formatter.format(Date(dataSnapshot.child("duration").value.toString().toLong()))
                            loadStepsData(dataSnapshot.child("steps").value.toString().toFloat())
                            tvSummaryDate.text = dataSnapshot.child("date").value.toString()
                            tvDistanceCounter.text = dataSnapshot.child("distance").value.toString()
                            tvCalorieCounter.text = dataSnapshot.child("calories").value.toString()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(applicationContext, databaseError.message, Toast.LENGTH_LONG).show()
                    }
                })
    }
}