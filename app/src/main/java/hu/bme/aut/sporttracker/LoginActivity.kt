package hu.bme.aut.sporttracker

import android.app.ProgressDialog
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.android.synthetic.main.activity_login.*
import java.lang.NullPointerException


class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setSupportActionBar(toolbar)
        auth = FirebaseAuth.getInstance()
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


    private fun showProgressDialog() {
        if (progressDialog != null) {
            return
        }

        progressDialog = ProgressDialog(this).apply {
            setCancelable(false)
            setMessage("Please wait...")
            show()
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.let { dialog ->
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }
        progressDialog = null
    }


    private fun createAccount() {
        if (!validateForm()) {
            return
        }

        showProgressDialog()

        auth.createUserWithEmailAndPassword(etEmail.text.toString(), etPassword.text.toString())
            .addOnSuccessListener { result ->
                val firebaseUser = result.user

                val profileChangeRequest = UserProfileChangeRequest.Builder()
                    .setDisplayName(firebaseUser.email?.substringBefore('@'))
                    .build()
                firebaseUser.updateProfile(profileChangeRequest)

                Toast.makeText(this, "Registration successful!", Toast.LENGTH_LONG).show()
                sendEmailVerification()

            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, exception.message, Toast.LENGTH_LONG).show()
            }
        hideProgressDialog()
    }

    private fun loginAccount() {
        if (!validateForm()) {
            return
        }
        showProgressDialog()

                showProgressDialog()
                auth.signInWithEmailAndPassword(etEmail.text.toString(), etPassword.text.toString())
                    .addOnSuccessListener {
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, exception.localizedMessage, Toast.LENGTH_LONG).show()
                        hideProgressDialog()
                    }
                hideProgressDialog()


        hideProgressDialog()
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
