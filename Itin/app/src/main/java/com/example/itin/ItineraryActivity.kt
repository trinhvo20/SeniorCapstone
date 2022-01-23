package com.example.itin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.classes.Day
import com.example.itin.classes.Trip
import kotlinx.android.synthetic.main.activity_itinerary.*


class ItineraryActivity : AppCompatActivity() {
    private lateinit var dayAdapter : DayAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerary)

        // get the trip object from MainActivity
        val trip = intent.getSerializableExtra("EXTRA_TRIP") as Trip

        tvName.text = trip.name
        tvDateRange.text = "From: ${trip.startDate}     To: ${trip.endDate}"

        var days = trip.days

        // initiate a new object of class TripAdapter, pass in trips list as parameter
        dayAdapter = DayAdapter(days)

        // assign adapter for our RecyclerView
        rvActivityList.adapter = dayAdapter

        // determine how items are arrange in our list
        rvActivityList.layoutManager = LinearLayoutManager(this)

        val dayNum = 2
        for (i in 1..dayNum) {
            val day = Day(i.toString())
            days.add(day)
        }

        dayAdapter.notifyDataSetChanged()

    }

}