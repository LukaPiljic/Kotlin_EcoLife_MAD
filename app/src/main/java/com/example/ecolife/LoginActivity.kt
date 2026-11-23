package com.example.ecolife

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private val TAG = "EcoLifeLogin"
    private lateinit var auth: FirebaseAuth

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.onSignInResult(res)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: LoginActivity started.")

        auth = FirebaseAuth.getInstance()

        // If already signed in, go straight to MainActivity
        if (auth.currentUser != null) {
            Log.d(TAG, "onCreate: User already signed in, going to MainActivity.")
            goToMain()
            return
        }

        setContentView(R.layout.activity_login)
        launchSignInFlow()
    }

    private fun launchSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setTheme(R.style.Theme_EcoLife)
            .build()
            
        signInLauncher.launch(signInIntent)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            val user = FirebaseAuth.getInstance().currentUser
            Log.i(TAG, "onActivityResult: Signed in as ${user?.displayName}")
            Toast.makeText(this, getString(R.string.signed_in), Toast.LENGTH_SHORT).show()
            goToMain()
        } else {
            Log.e(TAG, "onActivityResult: Sign in failed: ${response?.error?.errorCode}")
            Toast.makeText(this, getString(R.string.signed_in_cancelled), Toast.LENGTH_SHORT).show()
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
