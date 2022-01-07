package com.example.itin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_create_trip.*

class CreateTripActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_trip)

        // what happen when click Cancel button (go back to the main page)
        btCancel.setOnClickListener() {
            finish()
        }

        // what happen when click Add button
        btAdd.setOnClickListener() {
            // get the user input
            val name = etName.text.toString()
            val location = etLocation.text.toString()
            val startDate = etStartDate.text.toString()
            val endDate = etEndDate.text.toString()

            // create an object of Trip class
            val trip = Trip(name, location,startDate, endDate)

            // pass this object to trip_item view or the TripAdapter

        }
    }
}