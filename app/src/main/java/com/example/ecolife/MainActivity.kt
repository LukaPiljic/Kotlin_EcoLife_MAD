package com.example.ecolife

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.ecolife.network.AirPollutionResponse
import com.example.ecolife.network.WeatherApiService
import com.example.ecolife.network.WeatherResponse
import com.example.ecolife.room.AppDatabase
import com.example.ecolife.room.CoordinateEntity
import com.example.ecolife.room.ObservationEntity
import com.firebase.ui.auth.AuthUI
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity(), LocationListener {

    private val TAG = "EcoLifeMain"
    private lateinit var locationManager: LocationManager
    private lateinit var locationSwitch: SwitchCompat
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var tvWeather: TextView
    private val locationPermissionCode = 2
    private var latestLocation: Location? = null

    private val observationTypes = arrayOf("Temperature (°C)", "Noise Level (dB)", "Air Quality (AQI)")
    private val prefs by lazy { getSharedPreferences("EcoLifePrefs", Context.MODE_PRIVATE) }

    private var weatherLine = ""
    private var aqiLine = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: EcoLife is starting up.")
        setContentView(R.layout.activity_main)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        tvWeather = findViewById(R.id.tvWeather)

        // Toolbar
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Navigation Drawer
        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        val navView: NavigationView = findViewById(R.id.navView)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.nav_open, R.string.nav_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { /* already here */ }
                R.id.nav_map  -> startActivity(buildMapIntent())
                R.id.nav_log  -> startActivity(Intent(this, ListViewActivity::class.java))
                R.id.nav_buddy -> startActivity(Intent(this, EcoBuddyActivity::class.java))
                R.id.nav_ranking -> startActivity(Intent(this, RankingActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            }
            drawerLayout.closeDrawers()
            true
        }

        // Bottom Navigation
        bottomNav = findViewById(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_map  -> { startActivity(buildMapIntent()); true }
                R.id.nav_log  -> { startActivity(Intent(this, ListViewActivity::class.java)); true }
                R.id.nav_buddy -> { startActivity(Intent(this, EcoBuddyActivity::class.java)); true }
                R.id.nav_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
                else -> false
            }
        }

        // Nickname — prefer Firebase display name, fall back to local nickname
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val displayName = firebaseUser?.displayName
        if (!displayName.isNullOrBlank()) {
            updateNicknameGreeting(displayName)
        } else {
            val nickname = getNickname()
            if (nickname == null) {
                showNicknameDialog()
            } else {
                updateNicknameGreeting(nickname)
            }
        }

        locationSwitch = findViewById(R.id.locationSwitch)
        locationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Log.d(TAG, "locationSwitch: ON — starting location updates.")
                locationSwitch.text = "Disable location"
                startLocationUpdates()
            } else {
                Log.d(TAG, "locationSwitch: OFF — stopping location updates.")
                locationSwitch.text = "Enable location"
                stopLocationUpdates()
            }
        }

        findViewById<Button>(R.id.btnViewLog).setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }

        findViewById<Button>(R.id.btnOpenMap).setOnClickListener {
            startActivity(buildMapIntent())
        }

        findViewById<Button>(R.id.btnLogObservation).setOnClickListener {
            showAddObservationDialog()
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun buildMapIntent(): Intent {
        val intent = Intent(this, OpenStreetMapsActivity::class.java)
        if (latestLocation != null) {
            val bundle = Bundle()
            bundle.putParcelable("location", latestLocation)
            intent.putExtra("locationBundle", bundle)
        }
        return intent
    }

    // ── Toolbar menu (Logout) ────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> { logout(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        AuthUI.getInstance().signOut(this).addOnCompleteListener {
            Toast.makeText(this, getString(R.string.signed_out), Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // ── Nickname helpers ──────────────────────────────────────────────────────

    private fun getNickname(): String? = prefs.getString("userNickname", null)

    private fun saveNickname(nickname: String) {
        prefs.edit().putString("userNickname", nickname).apply()
    }

    private fun updateNicknameGreeting(nickname: String) {
        val tvNickname: TextView = findViewById(R.id.tvNickname)
        tvNickname.text = getString(R.string.greeting_prefix) + nickname + "!"
    }

    private fun showNicknameDialog() {
        val input = EditText(this)
        input.hint = getString(R.string.nickname_dialog_hint)
        getNickname()?.let { input.setText(it) }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.nickname_dialog_title))
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val userInput = input.text.toString().trim()
                if (userInput.isNotBlank()) {
                    saveNickname(userInput)
                    updateNicknameGreeting(userInput)
                    Toast.makeText(this, getString(R.string.nickname_saved, userInput), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.nickname_empty_error), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(this, "Thanks and goodbye!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    // ── Observation dialog ────────────────────────────────────────────────────

    private fun showAddObservationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_observation, null)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerObservationType)
        val etValue = dialogView.findViewById<EditText>(R.id.etObservationValue)
        val etNote  = dialogView.findViewById<EditText>(R.id.etObservationNote)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, observationTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.observation_dialog_title))
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val type  = spinner.selectedItem.toString()
                val value = etValue.text.toString().trim()
                val note  = etNote.text.toString().trim()

                if (value.isNotBlank()) {
                    saveObservationToDatabase(type, value, note)
                    Toast.makeText(this, getString(R.string.observation_saved), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.observation_value_empty), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveObservationToDatabase(type: String, value: String, note: String) {
        val timestamp = System.currentTimeMillis()
        val lat = if (latestLocation != null) String.format("%.4f", latestLocation!!.latitude) else "0.0000"
        val lon = if (latestLocation != null) String.format("%.4f", latestLocation!!.longitude) else "0.0000"
        val entity = ObservationEntity(
            timestamp = timestamp,
            type = type,
            value = value,
            note = note,
            lat = lat,
            lon = lon
        )
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            db.observationDao().insert(entity)
            Log.d(TAG, "saveObservationToDatabase: saved $type=$value")
        }

        // Sync to Firebase Realtime Database
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val report = mapOf(
                "userId" to user.uid,
                "timestamp" to timestamp,
                "type" to type,
                "value" to value,
                "note" to note,
                "latitude" to lat,
                "longitude" to lon
            )
            FirebaseDatabase.getInstance().reference
                .child("observations")
                .child(user.uid)
                .push()
                .setValue(report)
                .addOnSuccessListener { Log.d(TAG, "saveObservationToDatabase: synced to Firebase") }
                .addOnFailureListener { e -> Log.e(TAG, "saveObservationToDatabase: Firebase sync failed: ${e.message}") }
        }
    }

    // ── Location helpers ──────────────────────────────────────────────────────

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                locationPermissionCode
            )
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
        }
    }

    private fun stopLocationUpdates() {
        locationManager.removeUpdates(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
                }
            } else {
                locationSwitch.isChecked = false
                findViewById<TextView>(R.id.tvLocation).text = getString(R.string.permission_required)
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        latestLocation = location
        val lat = String.format("%.4f", location.latitude)
        val lon = String.format("%.4f", location.longitude)
        val alt = String.format("%.2f", location.altitude)
        Log.d(TAG, "onLocationChanged: Lat=$lat, Lon=$lon, Alt=$alt")
        findViewById<TextView>(R.id.tvLocation).text = "Latitude:  $lat\nLongitude: $lon\nAltitude:  $alt m"
        saveCoordinatesToDatabase(location.latitude, location.longitude, location.altitude, System.currentTimeMillis())
        fetchWeather(location.latitude, location.longitude)
    }

    private fun saveCoordinatesToDatabase(latitude: Double, longitude: Double, altitude: Double, timestamp: Long) {
        val entity = CoordinateEntity(
            timestamp = timestamp,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude
        )
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            db.coordinateDao().insert(entity)
            Log.d(TAG, "saveCoordinatesToDatabase: $timestamp;$latitude;$longitude;$altitude")
        }
    }

    // ── Weather ───────────────────────────────────────────────────────────────

    private fun getApiKey(): String? =
        getSharedPreferences("AppPreferences", Context.MODE_PRIVATE).getString("API_KEY", null)

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val cap = cm.getNetworkCapabilities(network) ?: return false
        return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank()) {
            tvWeather.text = getString(R.string.weather_no_key)
            return
        }
        if (!isNetworkAvailable()) {
            tvWeather.text = getString(R.string.weather_unavailable)
            return
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(WeatherApiService::class.java)

        service.getCurrentWeather(lat, lon, "metric", apiKey).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    val temp = String.format("%.1f", body.main.temp)
                    val desc = body.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: ""
                    weatherLine = "Temp: $temp°C   Humidity: ${body.main.humidity}%   $desc"
                    Log.d(TAG, "fetchWeather: $weatherLine")
                    updateWeatherCard()

                    // Load weather icon with Glide
                    val iconCode = body.weather.firstOrNull()?.icon
                    if (iconCode != null) {
                        val iconUrl = "https://openweathermap.org/img/wn/${iconCode}@2x.png"
                        val ivWeatherIcon = findViewById<ImageView>(R.id.ivWeatherIcon)
                        Glide.with(this@MainActivity).load(iconUrl).into(ivWeatherIcon)
                    }
                } else {
                    Log.e(TAG, "fetchWeather error: ${response.code()}")
                }
            }
            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e(TAG, "fetchWeather failure: ${t.message}")
                tvWeather.text = getString(R.string.weather_unavailable)
            }
        })

        service.getAirPollution(lat, lon, apiKey).enqueue(object : Callback<AirPollutionResponse> {
            override fun onResponse(call: Call<AirPollutionResponse>, response: Response<AirPollutionResponse>) {
                val aqi = response.body()?.list?.firstOrNull()?.main?.aqi
                if (response.isSuccessful && aqi != null) {
                    val label = when (aqi) {
                        1 -> getString(R.string.aqi_good)
                        2 -> getString(R.string.aqi_fair)
                        3 -> getString(R.string.aqi_moderate)
                        4 -> getString(R.string.aqi_poor)
                        else -> getString(R.string.aqi_very_poor)
                    }
                    aqiLine = "AQI: $aqi ($label)"
                    Log.d(TAG, "fetchAirPollution: $aqiLine")
                    updateWeatherCard()
                }
            }
            override fun onFailure(call: Call<AirPollutionResponse>, t: Throwable) {
                Log.e(TAG, "fetchAirPollution failure: ${t.message}")
            }
        })
    }

    private fun updateWeatherCard() {
        val lines = listOfNotNull(
            weatherLine.ifBlank { null },
            aqiLine.ifBlank { null }
        )
        tvWeather.text = if (lines.isEmpty()) getString(R.string.weather_loading) else lines.joinToString("\n")
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        val fbName = FirebaseAuth.getInstance().currentUser?.displayName
        if (!fbName.isNullOrBlank()) {
            updateNicknameGreeting(fbName)
        } else {
            getNickname()?.let { updateNicknameGreeting(it) }
        }
        if (locationSwitch.isChecked) startLocationUpdates()
        bottomNav.selectedItemId = R.id.nav_home

        // Refresh weather with latest known location (or default Madrid coords)
        val lat = latestLocation?.latitude ?: 40.4168
        val lon = latestLocation?.longitude ?: -3.7038
        fetchWeather(lat, lon)
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }
}
