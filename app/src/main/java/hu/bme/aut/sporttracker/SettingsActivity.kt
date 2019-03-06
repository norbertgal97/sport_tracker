package hu.bme.aut.sporttracker

import android.content.SharedPreferences
import android.os.Bundle

import android.preference.PreferenceManager
import android.preference.SwitchPreference
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat




class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var settingsFragment: FragmentSettingsBasic
    private var KEYS = arrayOf("goal", "name", "weight", "height", "gender", "age", "notificationSwitch")
    lateinit var databaseUser: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsFragment = FragmentSettingsBasic()
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, settingsFragment)
            .commit()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        databaseUser = FirebaseDatabase.getInstance().getReference("users")

        databaseUser.child(FirebaseAuth.getInstance().currentUser!!.uid).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (key in KEYS)
                        if (key != "name" && key != "notificationSwitch")
                            sharedPreferences.edit().putString(key, dataSnapshot.child(key).value.toString())
                                .apply()

                    sharedPreferences.edit()
                        .putBoolean("notificationSwitch", dataSnapshot.child("notificationSwitch").value as Boolean)
                        .apply()

                    val noti =
                        settingsFragment.findPreference("notificationSwitch") as android.support.v14.preference.SwitchPreference
                    noti.isChecked = dataSnapshot.child("notificationSwitch").value as Boolean
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(applicationContext, databaseError.message, Toast.LENGTH_LONG).show()
                }
            })
    }

    class FragmentSettingsBasic : PreferenceFragmentCompat() {
        override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            // additional setup
        }
    }

    override fun onStart() {
        super.onStart()

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        sharedPreferences.edit().putString("name", FirebaseAuth.getInstance().currentUser?.displayName.toString())
            .apply()
    }

    override fun onResume() {
        super.onResume()

        for (key in KEYS)
            initSum(PreferenceManager.getDefaultSharedPreferences(applicationContext), key)
    }

    override fun onStop() {
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)

        super.onStop()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val reference =
            FirebaseDatabase.getInstance().getReference("users/" + FirebaseAuth.getInstance().currentUser!!.uid)
        when {
            key == "gender" -> reference.child(key).setValue(sharedPreferences?.getString(key, ""))
            key == "notificationSwitch" -> reference.child(key).setValue(sharedPreferences?.getBoolean(key, false))
            key != null && key != "name" -> reference.child(key).setValue(sharedPreferences?.getString(key, "0")?.toInt()
            )
        }
        initSum(sharedPreferences, key)
    }

    private fun initSum(sharedPreferences: SharedPreferences?, key: String?) {
        if (sharedPreferences != null && key != "notificationSwitch") {
            val value: String = when (key) {
                "goal" -> " steps"
                "height" -> " cm"
                "weight" -> " kg"
                "age" -> " years"
                else -> {
                    " "
                }
            }
            val string: String = try {
                sharedPreferences.getString(key, "")!!
            } catch (e: NullPointerException) {
                ""
            }
            settingsFragment.findPreference(key).summary = string + value
        }
    }
}