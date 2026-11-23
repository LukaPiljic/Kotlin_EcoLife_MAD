package com.example.ecolife

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ecolife.network.WeatherApiService
import com.example.ecolife.network.WeatherResponse
import com.example.ecolife.network.AirPollutionResponse
import com.example.ecolife.room.AppDatabase
import com.example.ecolife.room.EcoBuddyEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class EcoBuddyActivity : AppCompatActivity() {

    private val TAG = "EcoLifeBuddy"
    private var currentBuddy: EcoBuddyEntity? = null
    private val db by lazy { AppDatabase.getDatabase(this) }

    // Weather state
    private var currentTemp: Double? = null
    private var currentHumidity: Int? = null
    private var currentAqi: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: EcoBuddyActivity started.")
        setContentView(R.layout.activity_ecobuddy)

        findViewById<Button>(R.id.btnBuddyBack).setOnClickListener { finish() }

        loadBuddy()
    }

    private fun loadBuddy() {
        lifecycleScope.launch {
            currentBuddy = db.ecoBuddyDao().getActiveBuddy()
            if (currentBuddy == null) {
                showCreateBuddyDialog()
            } else {
                updateUI()
                fetchEnvironmentData()
            }
        }
    }

    private fun showCreateBuddyDialog() {
        val input = EditText(this)
        input.hint = getString(R.string.buddy_name_hint)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.buddy_create_title))
            .setMessage(getString(R.string.buddy_create_message))
            .setView(input)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.buddy_create_btn)) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotBlank()) {
                    createBuddy(name)
                } else {
                    createBuddy("EcoBuddy")
                }
            }
            .setNegativeButton(getString(R.string.btn_back)) { _, _ -> finish() }
            .show()
    }

    private fun createBuddy(name: String) {
        val buddy = EcoBuddyEntity(
            name = name,
            createdAt = System.currentTimeMillis(),
            hp = 100.0,
            lastCheckTimestamp = System.currentTimeMillis(),
            isAlive = true
        )
        lifecycleScope.launch {
            db.ecoBuddyDao().insert(buddy)
            currentBuddy = db.ecoBuddyDao().getActiveBuddy()
            Log.d(TAG, "createBuddy: Created '$name'")
            Toast.makeText(this@EcoBuddyActivity, getString(R.string.buddy_created, name), Toast.LENGTH_SHORT).show()
            updateUI()
            fetchEnvironmentData()
        }
    }

    private fun updateUI() {
        val buddy = currentBuddy ?: return

        findViewById<TextView>(R.id.tvBuddyName).text = buddy.name
        findViewById<TextView>(R.id.tvBuddyHp).text = getString(R.string.buddy_hp, buddy.hp.toInt())

        val ageDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - buddy.createdAt)
        findViewById<TextView>(R.id.tvBuddyAge).text = getString(R.string.buddy_age, ageDays)

        // Lifecycle stage
        val stage = when {
            !buddy.isAlive || buddy.hp <= 0 -> getString(R.string.buddy_stage_withered)
            ageDays < 2 -> getString(R.string.buddy_stage_egg)
            ageDays < 5 -> getString(R.string.buddy_stage_sprout)
            ageDays < 10 -> getString(R.string.buddy_stage_growing)
            else -> getString(R.string.buddy_stage_mature)
        }
        findViewById<TextView>(R.id.tvBuddyStage).text = stage

        // HP progress bar
        val progressBar = findViewById<ProgressBar>(R.id.pbBuddyHp)
        progressBar.progress = buddy.hp.toInt().coerceIn(0, 100)

        // Stage emoji/icon
        val stageIcon = when {
            !buddy.isAlive || buddy.hp <= 0 -> "🥀"
            ageDays < 2 -> "🥚"
            ageDays < 5 -> "🌱"
            ageDays < 10 -> "🌿"
            else -> "🌳"
        }
        findViewById<TextView>(R.id.tvBuddyIcon).text = stageIcon

        updateRecommendations()
    }

    private fun fetchEnvironmentData() {
        val apiKey = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            .getString("API_KEY", null)
        if (apiKey.isNullOrBlank()) {
            findViewById<TextView>(R.id.tvBuddyWeather).text = getString(R.string.weather_no_key)
            return
        }

        // Use default Madrid coords or last known
        val lat = 40.4168
        val lon = -3.7038

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(WeatherApiService::class.java)

        service.getCurrentWeather(lat, lon, "metric", apiKey).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    currentTemp = body.main.temp
                    currentHumidity = body.main.humidity
                    val desc = body.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: ""
                    findViewById<TextView>(R.id.tvBuddyWeather).text =
                        "🌡 ${String.format("%.1f", body.main.temp)}°C  💧 ${body.main.humidity}%  $desc"
                    applyEnvironmentEffects()
                }
            }
            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e(TAG, "fetchWeather failure: ${t.message}")
            }
        })

        service.getAirPollution(lat, lon, apiKey).enqueue(object : Callback<AirPollutionResponse> {
            override fun onResponse(call: Call<AirPollutionResponse>, response: Response<AirPollutionResponse>) {
                val aqi = response.body()?.list?.firstOrNull()?.main?.aqi
                if (response.isSuccessful && aqi != null) {
                    currentAqi = aqi
                    val label = when (aqi) {
                        1 -> getString(R.string.aqi_good)
                        2 -> getString(R.string.aqi_fair)
                        3 -> getString(R.string.aqi_moderate)
                        4 -> getString(R.string.aqi_poor)
                        else -> getString(R.string.aqi_very_poor)
                    }
                    val currentText = findViewById<TextView>(R.id.tvBuddyWeather).text
                    findViewById<TextView>(R.id.tvBuddyWeather).text = "$currentText\n🌬 AQI: $aqi ($label)"
                    applyEnvironmentEffects()
                }
            }
            override fun onFailure(call: Call<AirPollutionResponse>, t: Throwable) {
                Log.e(TAG, "fetchAirPollution failure: ${t.message}")
            }
        })
    }

    private fun applyEnvironmentEffects() {
        val buddy = currentBuddy ?: return
        if (!buddy.isAlive) return

        var hpDelta = 0.0
        val temp = currentTemp
        val humidity = currentHumidity
        val aqi = currentAqi

        // Temperature rules
        if (temp != null) {
            if (temp < 0 || temp > 35) hpDelta -= 1.0
        }

        // AQI rules
        if (aqi != null) {
            when {
                aqi >= 5 -> hpDelta -= 2.0
                aqi >= 4 -> hpDelta -= 1.0
            }
        }

        // Humidity rules
        if (humidity != null) {
            if (humidity < 30 || humidity > 80) hpDelta -= 0.5
        }

        // Healthy conditions recovery
        if (hpDelta == 0.0) hpDelta = 0.5

        val newHp = (buddy.hp + hpDelta).coerceIn(0.0, 100.0)
        val isAlive = newHp > 0

        val updated = buddy.copy(
            hp = newHp,
            lastCheckTimestamp = System.currentTimeMillis(),
            isAlive = isAlive
        )

        lifecycleScope.launch {
            db.ecoBuddyDao().update(updated)
            currentBuddy = updated
            updateUI()
            syncScoreToFirebase(updated)
            Log.d(TAG, "applyEnvironmentEffects: HP ${buddy.hp} -> $newHp (delta=$hpDelta)")
        }
    }

    private fun updateRecommendations() {
        val recommendations = mutableListOf<String>()
        val temp = currentTemp
        val humidity = currentHumidity
        val aqi = currentAqi

        if (temp != null) {
            if (temp > 35) recommendations.add(getString(R.string.rec_temp_high))
            if (temp < 0) recommendations.add(getString(R.string.rec_temp_low))
        }

        if (aqi != null) {
            if (aqi >= 4) recommendations.add(getString(R.string.rec_aqi_poor))
        }

        if (humidity != null) {
            if (humidity < 30) recommendations.add(getString(R.string.rec_humidity_low))
            if (humidity > 80) recommendations.add(getString(R.string.rec_humidity_high))
        }

        if (currentBuddy?.isAlive == false || (currentBuddy?.hp ?: 100.0) <= 0) {
            recommendations.clear()
            recommendations.add(getString(R.string.rec_buddy_dead))
        }

        if (recommendations.isEmpty()) {
            recommendations.add(getString(R.string.rec_all_good))
        }

        val tvRec = findViewById<TextView>(R.id.tvBuddyRecommendations)
        tvRec.text = recommendations.joinToString("\n\n")
    }

    private fun syncScoreToFirebase(buddy: EcoBuddyEntity) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val ref = FirebaseDatabase.getInstance().reference
            .child("rankings")
            .child(user.uid)

        val data = mapOf(
            "userId" to user.uid,
            "displayName" to (user.displayName ?: "Anonymous"),
            "buddyName" to buddy.name,
            "score" to buddy.hp.toInt(),
            "lastUpdated" to System.currentTimeMillis()
        )

        ref.setValue(data)
            .addOnSuccessListener { Log.d(TAG, "syncScoreToFirebase: Score synced.") }
            .addOnFailureListener { e -> Log.e(TAG, "syncScoreToFirebase: Failed: ${e.message}") }
    }
}
