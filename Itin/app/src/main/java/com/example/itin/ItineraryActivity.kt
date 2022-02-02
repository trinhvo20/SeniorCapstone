package com.example.itin

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.classes.Activity
import com.example.itin.classes.Day
import com.example.itin.classes.Trip
import kotlinx.android.synthetic.main.activity_friend.*
import kotlinx.android.synthetic.main.activity_itinerary.*
import kotlinx.android.synthetic.main.activity_itinerary.tvName
import kotlinx.android.synthetic.main.activity_itinerary.homeBtn
import kotlinx.android.synthetic.main.trip_day_item.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


class ItineraryActivity : AppCompatActivity(), ActivityAdapter.OnItemClickListener {
    private lateinit var dayAdapter : DayAdapter
    lateinit var days: MutableList<Day>
    private lateinit var trip : Trip
    @RequiresApi(Build.VERSION_CODES.O)
    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerary)

        // get the trip object from MainActivity
        trip = intent.getSerializableExtra("EXTRA_TRIP") as Trip

        tvName.text = trip.name
        tvDateRange.text = "From: ${trip.startDate}     To: ${trip.endDate}"

        days = trip.days

        // initiate a new object of class TripAdapter, pass in trips list as parameter
        dayAdapter = DayAdapter(this,days,this)

        // assign adapter for our RecyclerView
        rvActivityList.adapter = dayAdapter

        // determine how items are arrange in our list
        rvActivityList.layoutManager = LinearLayoutManager(this)



        var formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
        var startdate = LocalDate.parse(trip.startDate, formatter)
        var enddate = LocalDate.parse(trip.endDate, formatter)


        val dayNum = ChronoUnit.DAYS.between(startdate, enddate) + 1
        val actnum = 6
        for (i in 1..dayNum) {
            val day = Day(i.toString())
            for (i in 1..actnum){
                val activity = Activity("activity $i","$i:00", "test")
                day.activities.add(activity)
            }
            days.add(day)
        }



        dayAdapter.notifyDataSetChanged()

        homeBtn.setOnClickListener { finish() }
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

    fun addActivity(view : View, context : Context, curday : Day) {


        val view = LayoutInflater.from(context).inflate(R.layout.edit_activity, null)

        val etName = view.findViewById<EditText>(R.id.etName)
        val etLocation = view.findViewById<EditText>(R.id.etLocation)
        val etCost = view.findViewById<EditText>(R.id.etCost)
        val etNotes = view.findViewById<EditText>(R.id.etNotes)


        val newDialog = AlertDialog.Builder(context)
        newDialog.setView(view)

        newDialog.setPositiveButton("Edit") { dialog, _ ->
            val location = etLocation.text.toString()
            val cost = etCost.text.toString()
            val notes = etNotes.text.toString()

            val name = if (etName.text.toString().isEmpty()) {
                "$location"
            } else {
                etName.text.toString()
            }

            val activity = Activity(name, "1:00", location, cost, notes)
            curday.activities.add(activity)

            dayAdapter.notifyDataSetChanged()

            Toast.makeText(context, "Activity Edited", Toast.LENGTH_SHORT).show()
            dialog.dismiss()

        }

        newDialog.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
            Toast.makeText(context, "Canceled", Toast.LENGTH_SHORT).show()
        }

        newDialog.create()
        newDialog.show()
    }
}