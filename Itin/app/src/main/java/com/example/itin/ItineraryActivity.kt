package com.example.itin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_itinerary.*


class ItineraryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerary)

        // get the trip object from MainActivity
        val trip = intent.getSerializableExtra("EXTRA_TRIP") as Trip

        tvName.text = trip.name
        tvDateRange.text = "From: ${trip.startDate}\nTo: ${trip.endDate}"
    }
}