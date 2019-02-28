package hu.bme.aut.sporttracker

import android.content.SharedPreferences
import android.os.Bundle

import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceFragmentCompat
import com.google.firebase.auth.FirebaseAuth


class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var settingsFragment: FragmentSettingsBasic
    private var KEYS = arrayOf("goal", "name", "weight", "height", "gender", "age")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsFragment = FragmentSettingsBasic()
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, settingsFragment)
            .commit()
    }

    class FragmentSettingsBasic : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, key: String?) {
            addPreferencesFromResource(R.xml.preferences)
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