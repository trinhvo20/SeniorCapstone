package com.example.itin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.create_trip.*
import kotlinx.android.synthetic.main.activity_main.*

// Toggle Debugging
const val DEBUG_TOGGLE : Boolean = true

class MainActivity : AppCompatActivity(), TripAdapter.OnItemClickListener {

    private lateinit var tripAdapter : TripAdapter
    private lateinit var trips : MutableList<Trip>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // set trip list
        trips = mutableListOf()

        // initiate a new object of class TripAdapter, pass in trips list as parameter
        tripAdapter = TripAdapter(trips, this)

        // assign adapter for our RecyclerView
        rvTripList.adapter = tripAdapter

        // determine how items are arrange in our list
        rvTripList.layoutManager = LinearLayoutManager(this)

        // what happen when click on AddTodo button -> call the addTrip function
        btAddTrip.setOnClickListener { addTrip() }

        //Creating Testing Trip ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        if(DEBUG_TOGGLE) {
            val trip = Trip("TEST", "TEST", "1/1/1", "2/2/2")
            trips.add(trip)
            tripAdapter.notifyDataSetChanged()
        }
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    }

    // This function handles RecyclerView that lead you to TripDetails page
    override fun onItemClick(position: Int) {
        // intent with ItineraryActivity
        Intent(this, ItineraryActivity::class.java).also {
            // pass the current trip object between activities
            it.putExtra("EXTRA_TRIP", trips[position])
            // start ItineraryActivity
            startActivity(it)
        }
    }

    // This function will be called when you click the AddTrip button,
    // a dialog will show up for you to create a new trip
    private fun addTrip() {
        val view = LayoutInflater.from(this).inflate(R.layout.create_trip, null)

        val etLocation = view.findViewById<EditText>(R.id.etLocation)
        val etStartDate = view.findViewById<EditText>(R.id.etStartDate)
        val etEndDate = view.findViewById<EditText>(R.id.etEndDate)

        val newDialog = AlertDialog.Builder(this)
        newDialog.setView(view)

        newDialog.setPositiveButton("Add") { dialog, _ ->
            val name = "Trip to " + etLocation.text.toString()
            val location = etLocation.text.toString()
            val startDate = etStartDate.text.toString()
            val endDate = etEndDate.text.toString()

            val trip = Trip(name, location, startDate, endDate)
            trips.add(trip)
            tripAdapter.notifyDataSetChanged()

            Toast.makeText(this, "Added a new trip", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        newDialog.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
            Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show()
        }

        newDialog.create()
        newDialog.show()
    }


}