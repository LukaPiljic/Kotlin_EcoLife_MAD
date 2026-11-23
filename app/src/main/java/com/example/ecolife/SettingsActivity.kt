package com.example.ecolife

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ecolife.room.AppDatabase
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private val TAG = "EcoLifeSettings"
    private val prefs by lazy { getSharedPreferences("EcoLifePrefs", Context.MODE_PRIVATE) }
    private val appPrefs by lazy { getSharedPreferences("AppPreferences", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: SettingsActivity started.")
        setContentView(R.layout.activity_settings)

        refreshUI()

        findViewById<Button>(R.id.btnChangeNickname).setOnClickListener {
            Log.d(TAG, "onClick: Change nickname.")
            showChangeNicknameDialog()
        }

        findViewById<Button>(R.id.btnResetNickname).setOnClickListener {
            Log.d(TAG, "onClick: Reset nickname.")
            prefs.edit().remove("userNickname").apply()
            Toast.makeText(this, getString(R.string.nickname_reset_toast), Toast.LENGTH_SHORT).show()
            refreshUI()
        }

        // API Key save button
        val etApiKey = findViewById<EditText>(R.id.etApiKey)
        etApiKey.setText(appPrefs.getString("API_KEY", ""))

        findViewById<Button>(R.id.btnSaveApiKey).setOnClickListener {
            val key = etApiKey.text.toString().trim()
            if (key.isNotBlank()) {
                appPrefs.edit().putString("API_KEY", key).apply()
                Toast.makeText(this, getString(R.string.api_key_saved), Toast.LENGTH_SHORT).show()
                Log.d(TAG, "onClick: API key saved.")
            } else {
                Toast.makeText(this, getString(R.string.api_key_empty), Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnSettingsBack).setOnClickListener {
            Log.d(TAG, "onClick: Back.")
            finish()
        }
    }

    private fun refreshUI() {
        val nickname = prefs.getString("userNickname", null)
        findViewById<TextView>(R.id.tvCurrentNickname).text = nickname ?: getString(R.string.no_nickname)

        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            val count = db.observationDao().getCount()
            findViewById<TextView>(R.id.tvObsCount).text = count.toString()
            Log.d(TAG, "refreshUI: nickname=$nickname, observations=$count")
        }
    }

    private fun showChangeNicknameDialog() {
        val input = EditText(this)
        input.hint = getString(R.string.nickname_dialog_hint)
        prefs.getString("userNickname", null)?.let { input.setText(it) }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.nickname_dialog_title))
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val userInput = input.text.toString().trim()
                if (userInput.isNotBlank()) {
                    prefs.edit().putString("userNickname", userInput).apply()
                    Toast.makeText(this, getString(R.string.nickname_saved, userInput), Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "showChangeNicknameDialog: Nickname updated to '$userInput'")
                    refreshUI()
                } else {
                    Toast.makeText(this, getString(R.string.nickname_empty_error), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: SettingsActivity resumed.")
        refreshUI()
    }
}
