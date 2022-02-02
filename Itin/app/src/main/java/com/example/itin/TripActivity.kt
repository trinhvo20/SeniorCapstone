package com.example.itin

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.classes.Activity
import com.example.itin.classes.Day
import com.example.itin.classes.Trip
import com.example.itin.classes.User
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.create_trip.*
import kotlinx.android.synthetic.main.activity_trip.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

// Toggle Debugging
const val DEBUG_TOGGLE : Boolean = true

class TripActivity : AppCompatActivity(), TripAdapter.OnItemClickListener {

    private lateinit var tripAdapter : TripAdapter
    private lateinit var trips : MutableList<Trip>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var user : User

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip)

        // set trip list
        trips = mutableListOf()

        // initiate a new object of class TripAdapter, pass in trips list as parameter
        tripAdapter = TripAdapter(this, trips, this)

        // assign adapter for our RecyclerView
        rvTripList.adapter = tripAdapter

        // determine how items are arrange in our list
        rvTripList.layoutManager = LinearLayoutManager(this)

        // what happen when click on AddTodo button -> call the addTrip function
        btAddTrip.setOnClickListener { addTrip() }

        //Creating Testing Trip ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        if(DEBUG_TOGGLE) {
            val trip = Trip("Trip to TEST", "TEST", "1/1/2022", "1/2/2022")
            trips.add(trip)
            tripAdapter.notifyDataSetChanged()
        }
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // make the bottom navigation bar
        bottomNavBarSetup()

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
    @RequiresApi(Build.VERSION_CODES.O)
    private fun addTrip() {
        val view = LayoutInflater.from(this).inflate(R.layout.create_trip, null)

        val etName = view.findViewById<EditText>(R.id.etName)
        val etLocation = view.findViewById<EditText>(R.id.etLocation)
        val etStartDate = view.findViewById<EditText>(R.id.etStartDate)
        val etEndDate = view.findViewById<EditText>(R.id.etEndDate)

        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)+1
        val day = c.get(Calendar.DAY_OF_MONTH)

        val ivPickStartDate = view.findViewById<ImageView>(R.id.ivPickStartDate)
        val ivPickEndDate = view.findViewById<ImageView>(R.id.ivPickEndDate)

        ivPickStartDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener{_, mYear, mMonth, mDay ->
                etStartDate.setText(""+mMonth+"/"+mDay+"/"+mYear)
            }, year, month, day)
            datePickerDialog.show()
        }

        ivPickEndDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener{_, mYear, mMonth, mDay ->
                etEndDate.setText(""+mMonth+"/"+mDay+"/"+mYear)
            }, year, month, day)
            datePickerDialog.show()
        }

        val newDialog = AlertDialog.Builder(this)
        newDialog.setView(view)

        newDialog.setPositiveButton("Add") { dialog, _ ->
            val name: String
            val location = etLocation.text.toString()
            val startDate = etStartDate.text.toString()
            val endDate = etEndDate.text.toString()

            name = if (etName.text.toString().isEmpty()) {
                "Trip to $location"
            } else {
                etName.text.toString()
            }

            val trip = Trip(name, location, startDate, endDate)
            SendToDB(trip)
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

    // function to set up the bottom navigation bar
    private fun bottomNavBarSetup(){
        // makes the bottom navigation bar

        var bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavView_Bar)

        // makes the icons light up
        var menu = bottomNavigationView.menu
        var menuItem = menu.getItem(0)
        menuItem.setChecked(true)

        // what causes the activities to actually switch
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.ic_trips -> {

                }
                // go to profile
                R.id.ic_profile -> {
                    Intent(this, ProfileScreen::class.java).also {
                        startActivity(it)
                    }
                }
                // go to settings
                R.id.ic_settings -> {
                    Intent(this, Settings::class.java).also {
                        startActivity(it)
                    }
                }
            }
            true
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun SendToDB(trip : Trip){
        var formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
        var startDate = LocalDate.parse(trip.startDate, formatter)
        var endDate = LocalDate.parse(trip.endDate, formatter)

        val uid = firebaseAuth.currentUser?.uid.toString() // get uid from Google
        val curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
        val curTrip = curUser.child("trips")

        curTrip.child("Location").setValue(trip.location)

    }
}