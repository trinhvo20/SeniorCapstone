package com.example.itin

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.adapters.PreviousTripAdapter
import com.example.itin.classes.Activity
import com.example.itin.classes.Day
import com.example.itin.classes.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_previous_trip.*
import kotlinx.android.synthetic.main.activity_previous_trip.prevTripsSwipeContainer
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PreviousTripActivity : AppCompatActivity(), PreviousTripAdapter.OnItemClickListener {

    private lateinit var previousTripAdapter : PreviousTripAdapter
    private lateinit var previousTrips : MutableList<Trip>
    private lateinit var uid : String
    private lateinit var curUser: DatabaseReference
    private lateinit var masterTripList: DatabaseReference
    private var tripCount : Int = 0
    private lateinit var startdate : LocalDate
    private lateinit var formatter : DateTimeFormatter

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_previous_trip)

        previousTrips = mutableListOf()
        previousTripAdapter = PreviousTripAdapter(this, previousTrips, this)
        rvPreviousTrip.adapter = previousTripAdapter
        rvPreviousTrip.layoutManager = LinearLayoutManager(this)

        checkUser()

        // This following codes handle Pull-to-Refresh the Days RecyclerView
        // It will clear the days list and load all days from the DB again
        prevTripsSwipeContainer.setOnRefreshListener {
            previousTripAdapter.clear()
            readData(tripCount)
            prevTripsSwipeContainer.isRefreshing = false
        }
        // Configure the refreshing colors
        prevTripsSwipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light);

        backBtn.setOnClickListener { finish() }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRestart() {
        super.onRestart()
        previousTripAdapter.clear()
        readData(tripCount)
    }

    override fun onItemClick(position: Int) {
        finish()
        // intent with ItineraryActivity
        Intent(this, ItineraryActivity::class.java).also {
            it.putExtra("EXTRA_TRIP", previousTrips[position])
            startActivity(it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkUser(){
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        // If the use is not current logged in:
        if(firebaseUser == null) {
            startActivity(Intent(this, GoogleLogin::class.java))
        }
        else{
            uid = firebaseUser.uid
            Log.d("PreviousTripActivity", "uid: $uid")
            curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
            masterTripList = FirebaseDatabase.getInstance().getReference("masterTripList")
            masterTripList.get().addOnSuccessListener {
                if (it.exists()) {
                    // Try to grab the value from the DB for tripCount, if it doesn't exist, create the child
                    try {
                        tripCount = it.child("tripCount").value.toString().toInt()
                        readData(tripCount)
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this,"Current trip list is empty", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d("PreviousTripActivity", "There is no MasterTripList")
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun readData(count: Int) {
        val curTrips = curUser.child("trips")

        for(i in 0 until count) {
            curTrips.child("Trip $i").get().addOnSuccessListener {
                if (it.exists()) {
                    accessMasterTripList(i)
                }
                else {
                    Log.d("print", "Trip $i does not exist")
                }
            }.addOnCanceledListener {
                Log.d("print", "Failed to fetch trip $i")
            }
        }
    }

    // function used to access the masterTripList
    @RequiresApi(Build.VERSION_CODES.O)
    private fun accessMasterTripList2(i: Int) {

        masterTripList.child("$i").get().addOnSuccessListener {
            if (it.exists()) {
                val name = it.child("Name").value.toString()
                val location = it.child("Location").value.toString()
                val stDate = it.child("Start Date").value.toString()
                val endDate = it.child("End Date").value.toString()
                val deleted = it.child("Deleted").value.toString()
                var active = it.child("Active").value.toString()

                val trip = Trip(
                    name,
                    location,
                    stDate,
                    endDate,
                    stringToBoolean(deleted),
                    stringToBoolean(active),
                    tripID = i
                )

                if (deleted == "false" && active == "false") {
                    previousTrips.add(trip)
                    previousTripAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    // function used to access the masterTripList
    @RequiresApi(Build.VERSION_CODES.O)
    private fun accessMasterTripList(i: Int) {

        masterTripList.child("$i").get().addOnSuccessListener {
            if (it.exists()) {
                val tripInstance = masterTripList.child("$i")
                val name = it.child("Name").value.toString()
                val location = it.child("Location").value.toString()
                val stDate = it.child("Start Date").value.toString()
                val endDate = it.child("End Date").value.toString()
                val deleted = it.child("Deleted").value.toString()
                var active = it.child("Active").value.toString()
                var tripId = it.child("ID").value.toString().toInt()
                var endEpoch = it.child("EpochEnd").value.toString().toLong()

                formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
                startdate = LocalDate.parse(stDate, formatter)

                val trip = Trip(
                    name,
                    location,
                    stDate,
                    endDate,
                    stringToBoolean(deleted),
                    stringToBoolean(active),
                    tripId,
                    days = mutableListOf(),
                    epochEnd = endEpoch
                )
                if (deleted == "false" && active == "false") {
                    previousTrips.add(trip)
                    readDays(tripInstance, trip)
                }
            }
        }
    }

    // load days from database
    // do this in trip activity?? then just manipulate the trip directly
    @RequiresApi(Build.VERSION_CODES.O)
    private fun readDays(tripInstance: DatabaseReference, trip: Trip) {
        tripInstance.child("Days").get().addOnSuccessListener {
            if (it.exists()) {
                // will cycle through the amount of days that we have
                for (i in 0 until it.child("DayCount").value.toString().toInt()){
                    // will make the day classes
                    val dayInstance = tripInstance.child("Days").child(i.toString())
                    readDaysHelper(dayInstance, trip)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun readDaysHelper(dayInstance: DatabaseReference, trip: Trip) {
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
                    if (name == "null") {break}
                    val location = it.child(i.toString()).child("Location").value.toString()
                    val time = it.child(i.toString()).child("Time").value.toString()
                    val cost = it.child(i.toString()).child("Cost").value.toString()
                    val notes = it.child(i.toString()).child("Notes").value.toString()
                    var tripID = it.child(i.toString()).child("TripID").value.toString().toInt()
                    var activityID = it.child(i.toString()).child("ActivityID").value.toString()

                    val activity = Activity(name, time, location, cost, notes, tripID, activityID)
                    actList.add(activity)
                }

                val day = Day(dayNumber,actList,dayInt,tripID)
                trip.days.add(day)
                tripsort(previousTrips)
                previousTripAdapter.notifyDataSetChanged()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    // function to sort the activities on each of the day, it is a modified Insertion sort
    private fun tripsort (trips: MutableList<Trip>){
        var formatter = DateTimeFormatter.ofPattern("M/d/yyyy")

        for (i in 0 until trips.size) {
            val key = trips[i]

            if (key != null) {
                println(key.startDate)
            }

            var j = i - 1

            if (key != null) {
                while (j >= 0 && LocalDate.parse(trips[j].startDate, formatter).isAfter(LocalDate.parse(key.startDate, formatter))){
                    trips[j + 1] = trips[j]
                    j--
                }
            }
            trips[j + 1] = key
        }
    }

    // convert a string to a boolean
    private fun stringToBoolean(str : String): Boolean {
        if (str == "false"){
            return false
        }
        return true
    }

}
