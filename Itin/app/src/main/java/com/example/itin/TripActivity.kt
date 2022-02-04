package com.example.itin

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.classes.Trip
import com.example.itin.classes.User
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_trip.*
import kotlinx.android.synthetic.main.create_trip.*
import java.util.*

// Toggle Debugging
const val DEBUG_TOGGLE : Boolean = true

class TripActivity : AppCompatActivity(), TripAdapter.OnItemClickListener {

    private lateinit var tripAdapter : TripAdapter
    private lateinit var trips : MutableList<Trip>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var user : User
    private var tripCount : Int = 0

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip)

        firebaseAuth = FirebaseAuth.getInstance()

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

        // set up proper tripCount
        val uid = firebaseAuth.currentUser?.uid.toString()

        // This here checks the value in the database to overwrite the initial value of 0
        val curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
        curUser.get().addOnSuccessListener {
            if (it.exists()) {
                // Try to grab the value from the DB for tripCount, if it doesn't exist, create the child
                try {
                    tripCount = it.child("tripCount").value.toString().toInt()
                }
                catch (e: NumberFormatException){
                    curUser.child("tripCount").setValue(0)
                }
            }
        }


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
        val etStartDate = view.findViewById<TextView>(R.id.etStartDate)
        val etEndDate = view.findViewById<TextView>(R.id.etEndDate)

        Places.initialize(this,getString(R.string.API_KEY))
        val placesClient = Places.createClient(this)

        // Initialize the AutocompleteSupportFragment.
        val autocompleteFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS))

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                // TODO: Get info about the selected place.
                Log.i("Places", "Place: ${place.address}, ${place.id}")
            }

            override fun onError(status: Status) {
                // TODO: Handle the error.
                Log.i("Places", "An error occurred: $status")
            }
        })

        // Calendar Date Picker
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val ivPickStartDate = view.findViewById<ImageView>(R.id.ivPickStartDate)
        val ivPickEndDate = view.findViewById<ImageView>(R.id.ivPickEndDate)

        ivPickStartDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener{_, mYear, mMonth, mDay ->
                etStartDate.text = "${mMonth+1}/$mDay/$mYear"
            }, year, month, day)
            datePickerDialog.show()
        }

        ivPickEndDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener{_, mYear, mMonth, mDay ->
                etEndDate.text = "${mMonth+1}/$mDay/$mYear"
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

            // Grab the initial values for database manipulation
            val trip = Trip(name, location, startDate, endDate)
            val uid = firebaseAuth.currentUser?.uid.toString()

            val curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
            val curTrip = curUser.child("trips")

            // This event listener waits for a change in its child (tripCount) then records the updated version
            // We must add 1 to this because it records the previous iterations' value
            curUser.get().addOnSuccessListener {
                if(it.exists()){
                    tripCount = it.child("tripCount").value.toString().toInt() + 1
                }
            }

            // Write to the database, then increment tripCount in the database
            SendToDB(trip,curTrip,tripCount)
            trips.add(trip)
            tripCount += 1
            curUser.child("tripCount").setValue(tripCount)
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
    private fun SendToDB(trip : Trip, curTrip : DatabaseReference, id : Int){

        // Navigates to the correct directory
        val tripInstance = curTrip.child(id.toString())

        tripInstance.child("Name").setValue(trip.name)
        tripInstance.child("Location").setValue(trip.location)
        tripInstance.child("Start Date").setValue(trip.startDate)
        tripInstance.child("End Date").setValue(trip.endDate)
    }
}