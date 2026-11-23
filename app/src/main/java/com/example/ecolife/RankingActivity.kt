package com.example.ecolife

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class RankingActivity : AppCompatActivity() {

    private val TAG = "EcoLifeRanking"
    private lateinit var listView: ListView
    private lateinit var tvEmpty: TextView
    private val rankingList = mutableListOf<RankingItem>()
    private lateinit var adapter: RankingAdapter

    data class RankingItem(
        val displayName: String = "",
        val buddyName: String = "",
        val score: Int = 0,
        val lastUpdated: Long = 0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: RankingActivity started.")
        setContentView(R.layout.activity_ranking)

        listView = findViewById(R.id.lvRanking)
        tvEmpty = findViewById(R.id.tvRankingEmpty)
        adapter = RankingAdapter()
        listView.adapter = adapter

        findViewById<Button>(R.id.btnRankingBack).setOnClickListener { finish() }

        loadRankings()
    }

    private fun loadRankings() {
        val ref = FirebaseDatabase.getInstance().reference.child("rankings")
        ref.orderByChild("score").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rankingList.clear()
                for (child in snapshot.children) {
                    val name = child.child("displayName").getValue(String::class.java) ?: "Anonymous"
                    val buddy = child.child("buddyName").getValue(String::class.java) ?: "—"
                    val score = child.child("score").getValue(Int::class.java) ?: 0
                    val updated = child.child("lastUpdated").getValue(Long::class.java) ?: 0
                    rankingList.add(RankingItem(name, buddy, score, updated))
                }
                // Sort descending by score
                rankingList.sortByDescending { it.score }
                adapter.notifyDataSetChanged()

                tvEmpty.visibility = if (rankingList.isEmpty()) View.VISIBLE else View.GONE
                listView.visibility = if (rankingList.isEmpty()) View.GONE else View.VISIBLE
                Log.d(TAG, "loadRankings: Loaded ${rankingList.size} entries.")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "loadRankings: Failed: ${error.message}")
            }
        })
    }

    inner class RankingAdapter : BaseAdapter() {
        override fun getCount() = rankingList.size
        override fun getItem(position: Int) = rankingList[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@RankingActivity)
                .inflate(R.layout.ranking_item, parent, false)

            val item = rankingList[position]
            view.findViewById<TextView>(R.id.tvRankPosition).text = "#${position + 1}"
            view.findViewById<TextView>(R.id.tvRankName).text = item.displayName
            view.findViewById<TextView>(R.id.tvRankBuddy).text = item.buddyName
            view.findViewById<TextView>(R.id.tvRankScore).text = "${item.score} HP"

            return view
        }
    }
}
