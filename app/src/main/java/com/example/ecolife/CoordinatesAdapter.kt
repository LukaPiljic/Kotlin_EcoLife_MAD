package com.example.ecolife

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ObservationItem(
    val timestamp: String,
    val type: String,
    val value: String,
    val note: String,
    val lat: String,
    val lon: String
)

class CoordinatesAdapter(context: Context, private val items: List<ObservationItem>) :
    ArrayAdapter<ObservationItem>(context, R.layout.listview_item, items) {

    private val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.listview_item, parent, false)

        val item = items[position]

        val formattedTime = try {
            sdf.format(Date(item.timestamp.toLong()))
        } catch (e: Exception) {
            item.timestamp
        }

        view.findViewById<TextView>(R.id.tvItemTime).text = formattedTime
        view.findViewById<TextView>(R.id.tvItemType).text = item.type
        view.findViewById<TextView>(R.id.tvItemValue).text = item.value

        return view
    }
}
