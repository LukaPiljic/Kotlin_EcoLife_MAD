package com.example.ecolife

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ThirdActivity : AppCompatActivity() {

    private val TAG = "EcoLifeThird"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ThirdActivity created.")
        enableEdgeToEdge()
        setContentView(R.layout.activity_third)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }



        val timestamp = intent.getStringExtra("timestamp") ?: ""
        val latitude  = intent.getStringExtra("latitude")  ?: "—"
        val longitude = intent.getStringExtra("longitude") ?: "—"
        val altitude  = intent.getStringExtra("altitude")  ?: "—"
        val isObs     = intent.getBooleanExtra("isObservation", false)

        Log.d(TAG, "onCreate: isObs=$isObs, field1=$latitude, field2=$longitude, field3=$altitude, ts=$timestamp")

        val formattedTime = if (timestamp.isNotEmpty()) {
            val millis = timestamp.toLongOrNull() ?: 0L
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.format(Date(millis))
        } else "—"

        if (isObs) {
            findViewById<TextView>(R.id.tvThirdTitle).text    = getString(R.string.observation_detail_title)
            findViewById<TextView>(R.id.labelLatitude).text   = getString(R.string.label_type)
            findViewById<TextView>(R.id.labelLongitude).text  = getString(R.string.label_value)
            findViewById<TextView>(R.id.labelAltitude).text   = getString(R.string.label_note)
            findViewById<TextView>(R.id.tvTimestamp).text     = formattedTime
            findViewById<TextView>(R.id.tvLatitude).text      = latitude
            findViewById<TextView>(R.id.tvLongitude).text     = longitude
            findViewById<TextView>(R.id.tvAltitude).text      = altitude.ifBlank { "—" }
        } else {
            findViewById<TextView>(R.id.tvTimestamp).text     = formattedTime
            findViewById<TextView>(R.id.tvLatitude).text      = latitude
            findViewById<TextView>(R.id.tvLongitude).text     = longitude
            findViewById<TextView>(R.id.tvAltitude).text      = "$altitude m"
        }

        val btnBack: Button = findViewById(R.id.btnBackToLog)
        btnBack.setOnClickListener {
            Log.d(TAG, "onClick: Back button pressed.")
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ThirdActivity resumed.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ThirdActivity destroyed.")
    }
}
