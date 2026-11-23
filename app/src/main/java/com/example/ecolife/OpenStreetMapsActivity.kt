package com.example.ecolife

import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class OpenStreetMapsActivity : AppCompatActivity() {

    private val TAG = "EcoLifeMap"
    private lateinit var map: MapView

    // Eco-friendly green spaces in Madrid (near UPM Ciudad Universitaria)
    private val ecoPOICoords = listOf(
        GeoPoint(40.4308, -3.7196),  // Parque del Oeste
        GeoPoint(40.4239, -3.7297),  // Casa de Campo (North entrance)
        GeoPoint(40.4150, -3.7350),  // Casa de Campo (Center)
        GeoPoint(40.4628, -3.7336),  // Dehesa de la Villa
        GeoPoint(40.4493, -3.7278)   // Jardín Botánico UCM
    )

    private val ecoPOINames = listOf(
        "Parque del Oeste",
        "Casa de Campo (North)",
        "Casa de Campo (Center)",
        "Dehesa de la Villa",
        "Jardín Botánico UCM"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: OpenStreetMapsActivity starting.")
        enableEdgeToEdge()

        // Configure OSM user agent before setContentView
        Configuration.getInstance().userAgentValue = "com.example.ecolife"
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osm", MODE_PRIVATE))

        setContentView(R.layout.activity_open_street_maps)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Read location passed from MainActivity
        val bundle = intent.getBundleExtra("locationBundle")
        val location: Location? = bundle?.let { androidx.core.os.BundleCompat.getParcelable(it, "location", Location::class.java) }

        val startPoint = if (location != null) {
            Log.d(TAG, "onCreate: Location received [${location.altitude}][${location.latitude}][${location.longitude}]")
            GeoPoint(location.latitude, location.longitude)
        } else {
            Log.d(TAG, "onCreate: No location received, using default UPM coordinates.")
            GeoPoint(40.4493, -3.7278) // Jardín Botánico UCM as default
        }

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.controller.setZoom(13.0)
        map.controller.setCenter(startPoint)
        map.setMultiTouchControls(true)

        // Add user location marker
        addUserLocationMarker(startPoint, location != null)

        // Add eco-POI markers
        addEcoPOIMarkers()

        // Add eco-trail polyline connecting all POIs
        addEcoTrailPolyline()

        Log.d(TAG, "onCreate: Map set up with ${ecoPOICoords.size} eco-POIs.")
    }

    private fun addUserLocationMarker(position: GeoPoint, isRealLocation: Boolean) {
        val marker = Marker(map)
        marker.position = position
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation)
        marker.title = if (isRealLocation) getString(R.string.map_my_location) else "Default location (UPM)"
        marker.snippet = "Lat: ${String.format("%.4f", position.latitude)}, Lon: ${String.format("%.4f", position.longitude)}"
        map.overlays.add(marker)
        Log.d(TAG, "addUserLocationMarker: Added at ${position.latitude}, ${position.longitude}")
    }

    private fun addEcoPOIMarkers() {
        for (i in ecoPOICoords.indices) {
            val marker = Marker(map)
            marker.position = ecoPOICoords[i]
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_compass)
            marker.title = ecoPOINames[i]
            marker.snippet = "Eco-friendly green space"
            map.overlays.add(marker)
            Log.d(TAG, "addEcoPOIMarkers: Added '${ecoPOINames[i]}' at ${ecoPOICoords[i].latitude}, ${ecoPOICoords[i].longitude}")
        }
    }

    private fun addEcoTrailPolyline() {
        val polyline = Polyline()
        polyline.setPoints(ecoPOICoords)
        polyline.outlinePaint.color = 0xFF2E7D32.toInt() // eco_green_dark
        polyline.outlinePaint.strokeWidth = 5f
        map.overlays.add(polyline)
        Log.d(TAG, "addEcoTrailPolyline: Eco-trail connecting ${ecoPOICoords.size} POIs added.")
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        Log.d(TAG, "onResume: Map resumed.")
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        Log.d(TAG, "onPause: Map paused.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: OpenStreetMapsActivity destroyed.")
    }
}
