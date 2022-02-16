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
    private lateinit var masterTripList: DatabaseReference
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
            masterTripList = FirebaseDatabase.getInstance().getReference("masterTripList")
            masterTripList.get().addOnSuccessListener {
                if (it.exists()) {
                    // Try to grab the value from the DB for tripCount, if it doesn't exist, create the child
                    try {
                        tripCount = it.child("tripCount").value.toString().toInt()
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this,"Current trip list is empty", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d("PreviousTripActivity", "There is no MasterTripList")
                }
            }
            curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
            curUser.get().addOnSuccessListener {
                if (it.exists()) {
                    // Try to get the previousTripCount value, if it is not exist, create a new one
                    try {
                        previousTripCount = it.child("previousTripCount").value.toString().toInt()
                    } catch (e: NumberFormatException) {
                        curUser.child("previousTripCount").setValue(0)
                    }
                    readData(tripCount)
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
    private fun accessMasterTripList(i: Int) {

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
                    previousTripCount = previousTrips.size
                    curUser.child("previousTripCount").setValue(previousTripCount)
                }
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
