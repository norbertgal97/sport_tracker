package hu.bme.aut.sporttracker

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import hu.bme.aut.sporttracker.data.Activity
import hu.bme.aut.sporttracker.data.User
import kotlinx.android.synthetic.main.activity_login.*
import java.text.DateFormat
import java.util.*


class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    lateinit var databaseUser: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        login_layout_constraint.background.alpha = 255

        auth = FirebaseAuth.getInstance()
        databaseUser = FirebaseDatabase.getInstance().getReference("users")

        btnLogin.setOnClickListener { loginAccount() }
        btnRegister.setOnClickListener { createAccount() }
    }

    private fun validateForm(): Boolean {
        var valid = true

        val email = etEmail.text.toString()
        if (TextUtils.isEmpty(email)) {
            etEmail.error = "Required."
            valid = false
        } else {
            etEmail.error = null
        }

        val password = etPassword.text.toString()
        if (TextUtils.isEmpty(password)) {
            etPassword.error = "Required."
            valid = false
        } else {
            etPassword.error = null
        }
        return valid
    }

    override fun onStart() {
        super.onStart()
        login_layout.translationY = -1000f
        login_layout.animate().translationYBy(1000f).duration = 750
        if (auth.currentUser != null) {
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()
        }
    }

    private fun createAccount() {
        if (!validateForm()) {
            return
        }

        auth.createUserWithEmailAndPassword(etEmail.text.toString(), etPassword.text.toString())
            .addOnSuccessListener { result ->
                val firebaseUser = result.user

                val profileChangeRequest = UserProfileChangeRequest.Builder()
                    .setDisplayName(firebaseUser.email?.substringBefore('@'))
                    .build()
                firebaseUser.updateProfile(profileChangeRequest)

                val user = User(
                    FirebaseAuth.getInstance().currentUser!!.uid,
                    false,
                    10,
                    18,
                    "Male",
                    170,
                    70
                )

                val activity = Activity(
                    0f,
                    0f,
                    0f,
                    0L,
                    DateFormat.getDateInstance(DateFormat.SHORT).format(Calendar.getInstance().time)
                )

                databaseUser.child(FirebaseAuth.getInstance().currentUser!!.uid).child("Settings").setValue(user)
                databaseUser.child(FirebaseAuth.getInstance().currentUser!!.uid).child("Activity").setValue(activity)
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_LONG).show()
                sendEmailVerification()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, exception.message, Toast.LENGTH_LONG).show()
            }
    }

    private fun loginAccount() {
        if (!validateForm()) {
            return
        }

        auth.signInWithEmailAndPassword(etEmail.text.toString(), etPassword.text.toString())
            .addOnSuccessListener {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, exception.localizedMessage, Toast.LENGTH_LONG).show()
            }
    }

    private fun sendEmailVerification() {
        val user = auth.currentUser
        user?.sendEmailVerification()
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        baseContext,
                        "Verification email sent to ${user.email} ",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Log.e("EmailVerification", "sendEmailVerification", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Failed to send verification email.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}
