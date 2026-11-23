package com.example.ecolife

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ecolife.room.AppDatabase
import kotlinx.coroutines.launch

class ListViewActivity : AppCompatActivity() {

    private val TAG = "EcoLifeListView"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ListViewActivity started.")
        setContentView(R.layout.activity_list_view)

        val lvObservations = findViewById<ListView>(R.id.lvObservations)
        val tvEmpty = findViewById<TextView>(R.id.tvObsEmpty)

        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            val entities = db.observationDao().getAll()  // already sorted DESC
            val items = entities.map { e ->
                ObservationItem(
                    timestamp = e.timestamp.toString(),
                    type      = e.type,
                    value     = e.value,
                    note      = e.note,
                    lat       = e.lat,
                    lon       = e.lon
                )
            }

            if (items.isEmpty()) {
                lvObservations.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
                Log.d(TAG, "onCreate: No observations found.")
            } else {
                lvObservations.visibility = View.VISIBLE
                tvEmpty.visibility = View.GONE
                val adapter = CoordinatesAdapter(this@ListViewActivity, items)
                lvObservations.adapter = adapter

                lvObservations.setOnItemClickListener { _, _, position, _ ->
                    val item = adapter.getItem(position) ?: return@setOnItemClickListener
                    Log.d(TAG, "onItemClick: Opening detail for ${item.type} = ${item.value}")
                    val intent = Intent(this@ListViewActivity, ThirdActivity::class.java).apply {
                        putExtra("timestamp", item.timestamp)
                        putExtra("latitude",  item.type)
                        putExtra("longitude", item.value)
                        putExtra("altitude",  item.note)
                        putExtra("isObservation", true)
                    }
                    startActivity(intent)
                }
                Log.d(TAG, "onCreate: Loaded ${items.size} observations.")
            }
        }

        findViewById<Button>(R.id.btnObsBack).setOnClickListener {
            Log.d(TAG, "onClick: Back.")
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ListViewActivity resumed.")
    }
}
