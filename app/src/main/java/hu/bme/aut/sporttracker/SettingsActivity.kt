package hu.bme.aut.sporttracker

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    lateinit var settingsFragment: FragmentSettingsBasic
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
    }

    override fun onStop() {
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
        super.onStop()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (sharedPreferences != null && key != "notificationSwitch") {
            val value: String = when (key) {
                "goal" -> " steps"
                "height" -> " cm"
                "weight" -> " kg"
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


