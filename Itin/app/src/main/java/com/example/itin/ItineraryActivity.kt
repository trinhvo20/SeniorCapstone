package com.example.itin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_itinerary.*


class ItineraryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerary)

        // get the trip object from MainActivity
        val trip = intent.getSerializableExtra("EXTRA_TRIP") as Trip

        tvName.text = trip.name
        tvDateRange.text = "From: ${trip.startDate}\nTo: ${trip.endDate}"

        bEditTrip?.setOnClickListener{
            editTrip(trip)
        }

    }

    // repurposed addTrip to edit trip info
    private fun editTrip(trip : Trip) {
        val view = LayoutInflater.from(this).inflate(R.layout.create_trip, null)

        val etLocation = view.findViewById<EditText>(R.id.etLocation)
        val etStartDate = view.findViewById<EditText>(R.id.etStartDate)
        val etEndDate = view.findViewById<EditText>(R.id.etEndDate)

        // auto fill dialog boxes
        etLocation.setText(trip.location)
        etStartDate.setText(trip.startDate)
        etEndDate.setText(trip.endDate)

        val newDialog = AlertDialog.Builder(this)
        newDialog.setView(view)

        newDialog.setPositiveButton("Update") { dialog, _ ->
            trip.name = "Trip to " + etLocation.text.toString()
            trip.location = etLocation.text.toString()
            trip.startDate = etStartDate.text.toString()
            trip.endDate = etEndDate.text.toString()

            Toast.makeText(this, "Trip Updated", Toast.LENGTH_SHORT).show()
            dialog.dismiss()

            // reload activity to update text
            finish()
            overridePendingTransition( 0, 0)
            startActivity(intent)
            overridePendingTransition( 0, 0)
        }

        newDialog.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
            Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show()
        }

        newDialog.create()
        newDialog.show()
    }

}