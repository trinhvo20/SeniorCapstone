package com.example.itin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.classes.Activity
import com.example.itin.classes.Day
import com.example.itin.classes.Trip
import kotlinx.android.synthetic.main.activity_itinerary.*
import kotlinx.android.synthetic.main.activity_itinerary.tvName
import kotlinx.android.synthetic.main.trip_day_item.*


class ItineraryActivity : AppCompatActivity(), ActivityAdapter.OnItemClickListener {
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
        dayAdapter = DayAdapter(this,days,this)

        // assign adapter for our RecyclerView
        rvActivityList.adapter = dayAdapter

        // determine how items are arrange in our list
        rvActivityList.layoutManager = LinearLayoutManager(this)

        val dayNum = 4
        val actnum = 6
        for (i in 1..dayNum) {
            val day = Day(i.toString())
            for (i in 1..actnum){
                val activity = Activity("activity $i","$i:00")
                day.activities.add(activity)
            }
            days.add(day)
        }

        dayAdapter.notifyDataSetChanged()

    }

    override fun onItemClick(position: Int) {
        Toast.makeText(this, "pos $position", Toast.LENGTH_SHORT).show()
    }

}