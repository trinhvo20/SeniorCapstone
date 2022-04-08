package com.example.itin

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.adapters.ActivityAdapter
import com.example.itin.classes.Activity
import com.example.itin.classes.Day
import com.example.itin.classes.Trip
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_friend.*
import kotlinx.android.synthetic.main.activity_itinerary.*
import kotlinx.android.synthetic.main.activity_itinerary.backBtn
import kotlinx.android.synthetic.main.activity_itinerary.btExpandMenu
import kotlinx.android.synthetic.main.activity_itinerary.tvName
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

// for notifications / FCM
const val TOPIC = "/topics/myTopic2"

class ItineraryActivity : AppCompatActivity(), ActivityAdapter.OnItemClickListener {
    private lateinit var dayAdapter: DayAdapter
    lateinit var days: MutableList<Day>
    private lateinit var trip: Trip
    private lateinit var uid: String
    private lateinit var startdate: LocalDate
    private lateinit var formatter: DateTimeFormatter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var masterTripList: DatabaseReference
    private lateinit var startDateObj : LocalDate

    // Variables for floating button animations
    private val rotateOpen: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.rotate_open_anim
        )
    }
    private val rotateClose: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.rotate_close_anim
        )
    }
    private val fromBottom: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.from_bottom_anim
        )
    }
    private val toBottom: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.to_bottom_anim
        )
    }
    private var clicked = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerary)

        firebaseAuth = FirebaseAuth.getInstance()
        masterTripList = FirebaseDatabase.getInstance().getReference("masterTripList")
        databaseReference = FirebaseDatabase.getInstance().reference

        // get the trip object from MainActivity
        trip = intent.getSerializableExtra("EXTRA_TRIP") as Trip

        tvName.text = trip.name
        tvTripLocation.text = trip.location
        tvDateRange.text = "From: ${trip.startDate}     To: ${trip.endDate}"

        days = trip.days

        // initiate a new object of class DayAdapter, pass in days list as parameter
        dayAdapter = DayAdapter(this, days, this)

        // assign adapter for our RecyclerView
        rvActivityList.adapter = dayAdapter

        // determine how items are arrange in our list
        rvActivityList.layoutManager = LinearLayoutManager(this)

        // need these for proper formatting from the DB
        formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
        startdate = LocalDate.parse(trip.startDate, formatter)
        var enddate = LocalDate.parse(trip.endDate, formatter)

        activitySort(days)
        dayAdapter.notifyDataSetChanged()

        btExpandMenu.setOnClickListener { onExpandButtonClicked() }
        shareBtn.setOnClickListener { onShareClicked() }
        editBtn.setOnClickListener { editTrip(trip) }

        backBtn.setOnClickListener {
            finish()
            //if active go to TripActivity, if not active go to Previous trips
            if (trip.active) {
                Intent(this, TripActivity::class.java).also {
                    // start TripActivity
                    startActivity(it)
                }
            } else {
                Intent(this, PreviousTripActivity::class.java).also {
                    // start PreviousTripActivity
                    startActivity(it)
                }
            }
        }

        chatBoxBtn.setOnClickListener {
            Intent(this, ChatActivity::class.java).also {
                it.putExtra("trip", trip)
                startActivity(it)
            }
        }

        // This following codes handle Pull-to-Refresh the Days RecyclerView
        // It will clear the days list and load all days from the DB again
        swipeContainer.setOnRefreshListener {
            dayAdapter.clear()
            loadDaysFromDB()
            swipeContainer.isRefreshing = false
        }
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );
    }

    override fun onItemClick(position: Int, daypos: Int) {
        Intent(this, DetailsActivity::class.java).also {
            // pass the current trip object between activities
            it.putExtra("ACTIVITY", days[daypos][position])
            it.putExtra("DAY_ID", days[daypos].dayInt)
            startActivity(it)
        }
    }

    // This function helps the Pull-to-Refresh feature
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadDaysFromDB() {
        val tripID = trip.tripID.toString()
        databaseReference.child("masterTripList").child(tripID).child("Days")
            .get().addOnSuccessListener {
                if (it.exists()) {
                    // will cycle through the amount of days that we have
                    for (i in 0 until it.child("DayCount").value.toString().toInt()) {
                        // will make the day classes
                        val dayInstance =
                            databaseReference.child("masterTripList").child(tripID).child("Days")
                                .child(i.toString())
                        loadActivitiesFromDB(dayInstance, trip)
                    }
                }
            }
    }

    // This function helps the Pull-to-Refresh feature
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadActivitiesFromDB(dayInstance: DatabaseReference, trip: Trip) {
        dayInstance.get().addOnSuccessListener {
            if (it.exists()) {
                // obtain dayNumber, dayInt, and tripID
                var dayNumber = it.child("Day Number").value.toString()
                val dayInt = dayNumber.toInt()
                dayNumber =
                    dayNumber + ": " + startdate.plusDays(dayNumber.toLong() - 1).format(formatter)
                        .toString()
                val tripID = it.child("TripID").value.toString().toInt()
                val actList: MutableList<Activity?> = mutableListOf()
                // pull the activity from the DB
                for (i in it.children) {
                    val name = i.child("name").value.toString()
                    if (name == "null") {
                        break
                    }
                    val location = i.child("location").value.toString()
                    val time = i.child("time").value.toString()
                    val cost = i.child("cost").value.toString()
                    val notes = i.child("notes").value.toString()
                    var tripID = i.child("tripID").value.toString().toInt()
                    var activityID = i.child("actID").value.toString()

                    val activity = Activity(name, time, location, cost, notes, tripID, activityID)
                    actList.add(activity)
                }

                val day = Day(dayNumber, actList, dayInt, tripID)
                days.add(day)
                dayAdapter.notifyDataSetChanged()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    // function to sort the activities on each of the day, it is a modified Insertion sort
    private fun activitySort(tempDays: MutableList<Day>) {
        var formatter = DateTimeFormatter.ofPattern("h:mm a")

        for (day in tempDays) {
            for (i in 0 until day.activities.size) {
                val key = day.activities[i]

                var j = i - 1

                if (key != null) {
                    while (j >= 0 && LocalTime.parse(day.activities[j]?.time, formatter)
                            .isAfter(LocalTime.parse(key.time, formatter))
                    ) {
                        day.activities[j + 1] = day.activities[j]
                        j--
                    }
                }
                day.activities[j + 1] = key
            }
        }
    }

    private fun onExpandButtonClicked() {
        setUsability(clicked)
        setAnimation(clicked)
        clicked = !clicked
    }

//    private fun visibilityToggle(button: FloatingActionButton, visibility: Boolean) {
//        if (visible == true) {
//            button.startAnimation(fromBottom)
//            btExpandMenu.startAnimation(rotateOpen)
//        }
//        else {
//            button.startAnimation(toBottom)
//            btExpandMenu.startAnimation(rotateClose)
//        }
//    }

    private fun setAnimation(clicked: Boolean) {
        if (!clicked) {
            chatBoxBtn.startAnimation(fromBottom)
            shareBtn.startAnimation(fromBottom)
            editBtn.startAnimation(fromBottom)
            btExpandMenu.startAnimation(rotateOpen)
        } else {
            chatBoxBtn.startAnimation(toBottom)
            shareBtn.startAnimation(toBottom)
            editBtn.startAnimation(toBottom)
            btExpandMenu.startAnimation(rotateClose)
        }
    }

    private fun setUsability(clicked: Boolean) {
        if (!clicked) {
            chatBoxBtn.visibility = View.VISIBLE
            shareBtn.visibility = View.VISIBLE
            editBtn.visibility = View.VISIBLE

            chatBoxBtn.isClickable = true
            shareBtn.isClickable = true
            editBtn.isClickable = true

        } else {
            chatBoxBtn.visibility = View.INVISIBLE
            shareBtn.visibility = View.INVISIBLE
            editBtn.visibility = View.INVISIBLE

            chatBoxBtn.isClickable = false
            shareBtn.isClickable = false
            editBtn.isClickable = false
        }
    }

    private fun onShareClicked() {
        val nit = Intent(this, ShareTripActivity::class.java).apply {
            putExtra("TRIP_ID", trip.tripID.toString())
        }
        Toast.makeText(this, trip.tripID.toString(), Toast.LENGTH_SHORT).show()
        this.startActivity(nit)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun editTrip(curTrip: Trip) {
        if (curTrip.viewers[uid] == 1){
            val view = LayoutInflater.from(this).inflate(R.layout.edit_trip, null)

            val etName = view.findViewById<EditText>(R.id.etName)
            val etStartDate = view.findViewById<TextView>(R.id.etStartDate)
            val etEndDate = view.findViewById<TextView>(R.id.etEndDate)

            var location = curTrip.location
            etName.setText(curTrip.name)
            etStartDate.text = curTrip.startDate
            etEndDate.text = curTrip.endDate
            startDateObj = LocalDate.parse(curTrip.startDate, formatter)

            // Handle AutoComplete Places Search from GoogleAPI
            if (!Places.isInitialized()) {
                Places.initialize(this, this.getString(R.string.API_KEY))
            }
            val placesClient = Places.createClient(this)
            val autocompleteFragment =
                (this as AppCompatActivity).supportFragmentManager.findFragmentById(R.id.etLocation2) as AutocompleteSupportFragment
            Log.d("Places", this.supportFragmentManager.findFragmentById(R.id.etLocation2).toString())
            autocompleteFragment.setPlaceFields(
                listOf(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.ADDRESS
                )
            )
            autocompleteFragment.setText(location)
            autocompleteFragment.setOnPlaceSelectedListener(object :
                PlaceSelectionListener {
                override fun onPlaceSelected(place: Place) {
                    location = place.name
                    Log.i("Places", "Place: ${place.address}, ${place.id}")
                }

                override fun onError(status: Status) {
                    Log.i("Places", "An error occurred: $status")
                }
            })

            // Calendar Picker
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val ivPickStartDate = view.findViewById<ImageView>(R.id.ivPickStartDate)
            val ivPickEndDate = view.findViewById<ImageView>(R.id.ivPickEndDate)

            ivPickStartDate.setOnClickListener {
                val datePickerDialog = DatePickerDialog(
                    this,
                    { _, mYear, mMonth, mDay ->
                        etStartDate.text = "" + (mMonth + 1) + "/" + mDay + "/" + mYear
                        startDateObj = LocalDate.parse(etStartDate.text.toString(), formatter)
                    }, year, month, day
                )
                datePickerDialog.datePicker.minDate = c.timeInMillis
                datePickerDialog.show()
            }

            ivPickEndDate.setOnClickListener {
                val datePickerDialog = DatePickerDialog(
                    this,
                    { _, mYear, mMonth, mDay ->
                        etEndDate.text = "" + (mMonth + 1) + "/" + mDay + "/" + mYear
                    }, year, month, day
                )
                datePickerDialog.datePicker.minDate =
                    startDateObj.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                datePickerDialog.show()
            }

            val dialog = AlertDialog.Builder(this)
            dialog.setView(view)
                .setPositiveButton("OK") { dialog, _ ->
                    val name = etName.text.toString()
                    val startDate = etStartDate.text.toString()
                    val endDate = etEndDate.text.toString()

                    if (name.isBlank()) {
                        if (location != curTrip.location) {
                            curTrip.name = "Trip to $location"
                        }
                    } else {
                        curTrip.name = name
                    }
                    if (location != curTrip.location) {
                        curTrip.location = location
                    }
                    if (startDate != curTrip.startDate) {
                        curTrip.startDate = startDate
                    }
                    if (endDate != curTrip.endDate) {
                        curTrip.endDate = endDate
                        // check for dayInterval to set the trip 'active' status
                        var formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
                        val today = LocalDate.now()
                        val endDateObj = LocalDate.parse(endDate, formatter)
                        val dayInterval =
                            ChronoUnit.DAYS.between(endDateObj, today).toInt()
                        curTrip.active = dayInterval <= 0
                    }
                    curTrip.sendToDB()
                    finish();
                    overridePendingTransition(0, 0);
                    startActivity(intent);
                    overridePendingTransition(0, 0);

                    this.supportFragmentManager.beginTransaction().remove(autocompleteFragment).commit()
                    Toast.makeText(this, "Successfully Edited", Toast.LENGTH_SHORT)
                        .show()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    this.supportFragmentManager.beginTransaction().remove(autocompleteFragment).commit()
                    Log.d(
                        "Places",
                        this.supportFragmentManager.findFragmentById(R.id.etLocation2).toString()
                    )
                    dialog.dismiss()
                }
                .create()
                .show()
            }

        else {
            Toast.makeText(context, "You do not have permission to preform this action", Toast.LENGTH_SHORT).show()
        }
    }
}