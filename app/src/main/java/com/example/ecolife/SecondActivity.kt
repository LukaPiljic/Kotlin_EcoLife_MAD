package com.example.ecolife

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ecolife.room.AppDatabase
import com.example.ecolife.room.CoordinateEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SecondActivity : AppCompatActivity() {

    private val TAG = "EcoLifeSecond"
    private var coordinates: List<CoordinateEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: SecondActivity created.")
        setContentView(R.layout.activity_second)

        val tvLogContents: TextView = findViewById(R.id.tvLogContents)

        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            coordinates = db.coordinateDao().getAll()
            if (coordinates.isEmpty()) {
                tvLogContents.text = getString(R.string.log_empty)
                Log.d(TAG, "onCreate: No coordinates found in DB.")
            } else {
                val sdf = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
                val lines = coordinates.reversed().map { c ->
                    val time = sdf.format(Date(c.timestamp))
                    "$time  ${String.format("%.4f", c.latitude)}  ${String.format("%.4f", c.longitude)}  ${String.format("%.2f", c.altitude)}m"
                }
                tvLogContents.text = "TIMESTAMP            LAT        LON        ALT\n" +
                        "─".repeat(52) + "\n" +
                        lines.joinToString("\n")
                Log.d(TAG, "onCreate: Loaded ${coordinates.size} coordinate entries.")
            }
        }

        findViewById<Button>(R.id.btnViewLatest).setOnClickListener {
            Log.d(TAG, "onClick: Navigating to ThirdActivity with latest entry.")
            val latest = coordinates.lastOrNull()
            val intent = Intent(this, ThirdActivity::class.java)
            if (latest != null) {
                intent.putExtra("timestamp", latest.timestamp.toString())
                intent.putExtra("latitude", String.format("%.4f", latest.latitude))
                intent.putExtra("longitude", String.format("%.4f", latest.longitude))
                intent.putExtra("altitude", String.format("%.2f", latest.altitude))
            }
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            Log.d(TAG, "onClick: Back button pressed.")
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: SecondActivity resumed.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: SecondActivity destroyed.")
    }
}
