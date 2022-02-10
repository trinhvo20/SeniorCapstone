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
import com.example.itin.classes.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_previous_trip.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class PreviousTripActivity : AppCompatActivity(), PreviousTripAdapter.OnItemClickListener {

    private lateinit var previousTripAdapter : PreviousTripAdapter
    private lateinit var previousTrips : MutableList<Trip>
    private lateinit var uid : String
    private lateinit var curUser: DatabaseReference
    private var previousTripCount : Int = 0
    private var tripCount : Int = 0

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_previous_trip)

        previousTrips = mutableListOf()
        previousTripAdapter = PreviousTripAdapter(this, previousTrips, this)
        rvPreviousTrip.adapter = previousTripAdapter
        rvPreviousTrip.layoutManager = LinearLayoutManager(this)

        checkUser()

        backBtn.setOnClickListener { finish() }
    }

    override fun onItemClick(position: Int) {
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
            curUser.get().addOnSuccessListener {
                if (it.exists()) {
                    // Try to grab the value from the DB for tripCount, if it doesn't exist, notify user
                    try {
                        tripCount = it.child("tripCount").value.toString().toInt()
                        // Try to get the previousTripCount value, if it is not exist, create a new one
                        try {
                            previousTripCount = it.child("previousTripCount").value.toString().toInt()
                        } catch (e: NumberFormatException) {
                            curUser.child("previousTripCount").setValue(0)
                        }
                        readData(tripCount)
                    }
                    catch (e: NumberFormatException){
                        Toast.makeText(this,"Current trip list is empty", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun readData(count: Int) {
        var formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
        val today = LocalDate.now()

        val checkTrips = curUser.child("trips")

        for(i in 0 until count) {
            checkTrips.child("$i").get().addOnSuccessListener { it ->
                if (it.exists()) {
                    val name = it.child("Name").value.toString()
                    val location = it.child("Location").value.toString()
                    val stDate = it.child("Start Date").value.toString()
                    val endDate = it.child("End Date").value.toString()
                    val deleted = it.child("Deleted").value.toString()
                    val active = it.child("Active").value.toString()

                    // add trip as long as it is not deleted, it is active, and endDate is before today
                    // compare now to trip's endDate, add to 'previousTrips', remove from 'trips'
                    if(deleted == "false" && active == "true") {
                        // check the day interval from trip's endDate to today
                        val endDateObj = LocalDate.parse(endDate, formatter)
                        val dayInterval = ChronoUnit.DAYS.between(endDateObj, today).toInt()
                        if (dayInterval > 0) {
                            val trip = Trip(name, location, stDate, endDate, stringToBoolean(deleted), stringToBoolean(active),tripID=i)
                            previousTrips.add(trip)
                            previousTripAdapter.notifyDataSetChanged()
                            previousTripCount = previousTrips.size
                            curUser.child("previousTripCount").setValue(previousTripCount)
                        }
                    }
                }
                else {
                    Log.d("print", "Trip $i does not exist")
                }
            }.addOnCanceledListener {
                Log.d("print", "Failed to fetch trip $i")
            }
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