package com.example.itin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.adapters.ActivityAdapter
import com.example.itin.classes.Activity
import com.example.itin.classes.Day
import com.example.itin.classes.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_itinerary.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter


class ItineraryActivity : AppCompatActivity(), ActivityAdapter.OnItemClickListener {
    private lateinit var dayAdapter : DayAdapter
    lateinit var days: MutableList<Day>
    private lateinit var trip : Trip
    private lateinit var uid : String
    private lateinit var startdate : LocalDate
    private lateinit var formatter : DateTimeFormatter
    private lateinit var curUser: DatabaseReference
    private lateinit var curTrips: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var masterTripList: DatabaseReference
    @RequiresApi(Build.VERSION_CODES.O)
    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerary)

        firebaseAuth = FirebaseAuth.getInstance()
        masterTripList = FirebaseDatabase.getInstance().getReference("masterTripList")

        // get the trip object from MainActivity
        trip = intent.getSerializableExtra("EXTRA_TRIP") as Trip

        tvName.text = trip.name
        tvDateRange.text = "From: ${trip.startDate}     To: ${trip.endDate}"

        days = trip.days

        // initiate a new object of class DayAdapter, pass in days list as parameter
        dayAdapter = DayAdapter(this,days,this)

        // assign adapter for our RecyclerView
        rvActivityList.adapter = dayAdapter

        // determine how items are arrange in our list
        rvActivityList.layoutManager = LinearLayoutManager(this)

        // need these for proper formatting from the DB
        formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
        startdate = LocalDate.parse(trip.startDate, formatter)
        var enddate = LocalDate.parse(trip.endDate, formatter)

        // Navigates to the correct directory (masterTripList)
        val tripInstance = masterTripList.child(trip.tripID.toString())

        // read days from DB
        // may seem redundant considering that a lot of this code is implemented in tripActivity
            // but it does catch some cases that tripActivity wouldn't
        //readDays(tripInstance)
        activitySort(days)
        dayAdapter.notifyDataSetChanged()

        backBtn.setOnClickListener {
            finish()
            //if active go to TripActivity, if not active go to Previous trips
            if(trip.active) {
                Intent(this, TripActivity::class.java).also {
                    // start TripActivity
                    startActivity(it)
                }
            }else{
                Intent(this, PreviousTripActivity::class.java).also {
                    // start PreviousTripActivity
                    startActivity(it)
                }
            }
        }
        // open chat box
        chatBoxBtn.setOnClickListener {
            finish()
            Intent(this, ChatActivity::class.java).also {
                it.putExtra("trip", trip)
                startActivity(it)
            }
        }
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
    private fun activitySort (tempDays : MutableList<Day>){
        var formatter = DateTimeFormatter.ofPattern("h:ma")

            for(day in tempDays) {
                for (i in 0 until day.activities.size) {
                    val key = day.activities[i]

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

    // load days from database
    // dont think we need this anymore ... but will keep it here until we are sure
    @RequiresApi(Build.VERSION_CODES.O)
    private fun readDays(tripInstance: DatabaseReference) {
        tripInstance.child("Days").get().addOnSuccessListener {
            if (it.exists()) {
                // will cycle through the amount of days that we have
                for (i in 0 until it.child("DayCount").value.toString().toInt()){
                    // will make the day classes
                    val dayInstance = tripInstance.child("Days").child(i.toString())
                    readDaysHelper(dayInstance)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun readDaysHelper(dayInstance: DatabaseReference) {
        dayInstance.get().addOnSuccessListener {
            if (it.exists()) {
                // obtain dayNumber, dayInt, and tripID
                var dayNumber = it.child("Day Number").value.toString()
                val dayInt = dayNumber.toInt()
                dayNumber = dayNumber + ": " + startdate.plusDays(dayNumber.toLong()-1).format(formatter).toString()
                val tripID = it.child("TripID").value.toString().toInt()
                val actCount = it.child("ActivityCount").value.toString().toInt()
                val actList : MutableList<Activity?> = mutableListOf()
                // pull the activity from the DB
                for (i in 0 until actCount ) {
                    val name = it.child(i.toString()).child("Name").value.toString()
                    val location = it.child(i.toString()).child("Location").value.toString()
                    val time = it.child(i.toString()).child("Time").value.toString()
                    val cost = it.child(i.toString()).child("Cost").value.toString()
                    val notes = it.child(i.toString()).child("Notes").value.toString()
                    var tripID = it.child(i.toString()).child("TripID").value.toString().toInt()
                    var activityID = it.child(i.toString()).child("ActivityID").value.toString().toInt()

                    val activity = Activity(name, time, location, cost, notes, tripID, activityID)
                    actList.add(activity)
                }
                val day = Day(dayNumber,actList,dayInt,tripID)
                trip.days.add(day)
                days = trip.days

                Log.d("READ:", trip.days.size.toString())
                // not the most efficient... but itll have to do
                activitySort(days)
                dayAdapter.notifyDataSetChanged()
            }
        }
    }

}