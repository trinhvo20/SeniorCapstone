package com.example.itin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.example.itin.classes.Trip


class TripViewer : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_viewer)

        val myIntent = intent
        val Current_trip = myIntent.getSerializableExtra("Selected trip") as Trip
        val datebox = findViewById<TextView>(R.id.textView2)
        val Back = findViewById<Button>(R.id.BackButton)

        supportActionBar!!.title = Current_trip.name
        datebox.text = Current_trip.startDate

        Back?.setOnClickListener() {
            // Toast for testing button
            //Toast.makeText(this@MainActivity, "something", Toast.LENGTH_LONG).show()
            finish()
        }

    }
}