package hu.bme.aut.sporttracker

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener {
            val intent = Intent(this, CounterActivity::class.java)
            startActivity(intent)
        }

        button.setOnClickListener {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            Toast.makeText(this, sharedPreferences.getString("name", "DEFAULT"), Toast.LENGTH_LONG).show()
        }
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

    override fun onResume() {
        super.onResume()
        if(!FirebaseAuth.getInstance().currentUser!!.isEmailVerified)
            Toast.makeText(this,"Verify your account!",Toast.LENGTH_LONG).show()
    }
}