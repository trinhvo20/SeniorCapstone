package com.example.itin

import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import kotlinx.android.synthetic.main.activity_itinerary.tvName
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// for notifications / FCM
const val TOPIC = "/topics/myTopic2"

class ItineraryActivity : AppCompatActivity(), ActivityAdapter.OnItemClickListener {
    private lateinit var dayAdapter : DayAdapter
    lateinit var days: MutableList<Day>
    private lateinit var trip : Trip
    private lateinit var uid : String
    private lateinit var startdate : LocalDate
    private lateinit var formatter : DateTimeFormatter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var masterTripList: DatabaseReference

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerary)

        firebaseAuth = FirebaseAuth.getInstance()
        masterTripList = FirebaseDatabase.getInstance().getReference("masterTripList")
        databaseReference = FirebaseDatabase.getInstance().reference

        // get the trip object from MainActivity
        trip = intent.getSerializableExtra("EXTRA_TRIP") as Trip

        tvName.text = trip.name
        tvTripLocation.text = trip.location
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

        chatBoxBtn.setOnClickListener {
            Intent(this, ChatActivity::class.java).also {
                it.putExtra("trip", trip)
                startActivity(it)
            }
        }

        // This following codes handle Pull-to-Refresh the Days RecyclerView
        // It will clear the days list and load all days from the DB again
        swipeContainer.setOnRefreshListener {
            dayAdapter.clear()
            loadDaysFromDB()
            swipeContainer.isRefreshing = false
        }
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light);
    }

    override fun onItemClick(position: Int, daypos: Int) {
        Intent(this, DetailsActivity::class.java).also {
            // pass the current trip object between activities
            it.putExtra("ACTIVITY", days[daypos][position])
            it.putExtra("DAY_ID", days[daypos].dayInt)
            startActivity(it)
        }
    }

    // This function helps the Pull-to-Refresh feature
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadDaysFromDB() {
        val tripID = trip.tripID.toString()
        databaseReference.child("masterTripList").child(tripID).child("Days")
            .get().addOnSuccessListener {
                if (it.exists()) {
                    // will cycle through the amount of days that we have
                    for (i in 0 until it.child("DayCount").value.toString().toInt()){
                        // will make the day classes
                        val dayInstance = databaseReference.child("masterTripList").child(tripID).child("Days").child(i.toString())
                        loadActivitiesFromDB(dayInstance, trip)
                    }
                }
            }
    }
    // This function helps the Pull-to-Refresh feature
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadActivitiesFromDB(dayInstance: DatabaseReference, trip: Trip) {
        dayInstance.get().addOnSuccessListener {
            if (it.exists()) {
                // obtain dayNumber, dayInt, and tripID
                var dayNumber = it.child("Day Number").value.toString()
                val dayInt = dayNumber.toInt()
                dayNumber = dayNumber + ": " + startdate.plusDays(dayNumber.toLong()-1).format(formatter).toString()
                val tripID = it.child("TripID").value.toString().toInt()
                val actList : MutableList<Activity?> = mutableListOf()
                // pull the activity from the DB
                for (i in it.children ) {
                    val name = i.child("name").value.toString()
                    if (name == "null") {break}
                    val location = i.child("location").value.toString()
                    val time = i.child("time").value.toString()
                    val cost = i.child("cost").value.toString()
                    val notes = i.child("notes").value.toString()
                    var tripID = i.child("tripID").value.toString().toInt()
                    var activityID = i.child("actID").value.toString()

                    val activity = Activity(name, time, location, cost, notes, tripID, activityID)
                    actList.add(activity)
                }

                val day = Day(dayNumber,actList,dayInt,tripID)
                days.add(day)
                dayAdapter.notifyDataSetChanged()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    // function to sort the activities on each of the day, it is a modified Insertion sort
    private fun activitySort (tempDays : MutableList<Day>){
        var formatter = DateTimeFormatter.ofPattern("h:mm a")

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
}