package com.example.itin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.adapters.ShareAdapter
import kotlinx.android.synthetic.main.activity_share_trip.*

class ShareTripActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_trip)

        val tripID = intent.getStringExtra("TRIP_ID")?.toInt()
        textView.text = tripID.toString()

        val Friends = mutableListOf<String>("test1","test2","test3")

        // initiate a new object of class ShareAdapter, pass in friends list as parameter
        var shareAdapter = ShareAdapter(this, Friends)

        // assign adapter for our RecyclerView
        rvShareFriends.adapter = shareAdapter

        // determine how items are arrange in our list
        rvShareFriends.layoutManager = LinearLayoutManager(this)

        shareAdapter.notifyDataSetChanged()
    }
}