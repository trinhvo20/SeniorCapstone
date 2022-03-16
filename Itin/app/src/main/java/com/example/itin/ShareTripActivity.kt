package com.example.itin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_share_trip.*

class ShareTripActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_trip)

        val tripID = intent.getStringExtra("TRIP_ID")?.toInt()
        textView.text = tripID.toString()
    }
}