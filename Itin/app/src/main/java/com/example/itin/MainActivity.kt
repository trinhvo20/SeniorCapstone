package com.example.itin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var tripAdapter : TripAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initiate a new object of class TripAdapter, pass in a empty mutable list
        tripAdapter = TripAdapter(mutableListOf())

        // attach tripAdapter to our recyclerView
        rvTripList.adapter = tripAdapter

        // determine how items are arrange in our list
        rvTripList.layoutManager = LinearLayoutManager(this)

        // what happen when click on AddTodo button (call the CreateTrip activity)
        btAddTrip.setOnClickListener() {
            Intent(this, CreateTripActivity::class.java).also {
                startActivity(it)
            }
        }
    }
}