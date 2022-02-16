package com.example.itin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.classes.Activity
import com.example.itin.classes.Day
import com.example.itin.classes.Trip
import kotlinx.android.synthetic.main.activity_itinerary.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


class ItineraryActivity : AppCompatActivity(), ActivityAdapter.OnItemClickListener {
    private lateinit var dayAdapter : DayAdapter
    lateinit var days: MutableList<Day>
    private lateinit var trip : Trip
    @RequiresApi(Build.VERSION_CODES.O)
    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerary)

        // get the trip object from MainActivity
        trip = intent.getSerializableExtra("EXTRA_TRIP") as Trip

        tvName.text = trip.name
        tvDateRange.text = "From: ${trip.startDate}     To: ${trip.endDate}"

        days = trip.days

        // initiate a new object of class TripAdapter, pass in trips list as parameter
        dayAdapter = DayAdapter(this,days,this)

        // assign adapter for our RecyclerView
        rvActivityList.adapter = dayAdapter

        // determine how items are arrange in our list
        rvActivityList.layoutManager = LinearLayoutManager(this)



        var formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
        var startdate = LocalDate.parse(trip.startDate, formatter)
        var enddate = LocalDate.parse(trip.endDate, formatter)

        val tempdays = mutableListOf<Day>()
        val dayNum = ChronoUnit.DAYS.between(startdate, enddate) + 1
        val actnum = 6
        for (i in 1..dayNum) {
            val day = Day(i.toString() + ": " + startdate.plusDays(i-1).format(formatter).toString())
            for (i in 1..actnum){
                val activity = Activity("activity $i","$i:00PM", "test")
                day.activities.add(activity)
            }
            days.add(day)
        }

        activitysort(days)

        dayAdapter.notifyDataSetChanged()

        backBtn.setOnClickListener { finish() }
    }

    override fun onItemClick(position: Int, daypos: Int) {
        Toast.makeText(this, "Day: $daypos \nActivity: $position", Toast.LENGTH_SHORT).show()
        Intent(this, DetailsActivity::class.java).also {
            // pass the current trip object between activities
            it.putExtra("ACTIVITY", days[daypos][position])
            // start ItineraryActivity
            startActivity(it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    // function to sort the activities on each of the day, it is a modified Insertion sort
    private fun activitysort (tempdays : MutableList<Day>){
        var formatter = DateTimeFormatter.ofPattern("h:ma")

            for(day in tempdays) {
                for (i in 0 until day.activities.size) {
                    val key = day.activities[i]

//                    if (key != null) {
//                        println(key.time)
//                    }

                    var j = i - 1

                    if (key != null) {
                        while (j >= 0 && LocalTime.parse(day.activities[j]?.time, formatter).isAfter(LocalTime.parse(key.time, formatter))){
                            day.activities[j + 1] = day.activities[j]
                            j--
                        }
                    }
                    day.activities[j + 1] = key
                }
            }
    }

}