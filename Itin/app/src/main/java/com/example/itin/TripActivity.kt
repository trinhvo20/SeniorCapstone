package com.example.itin

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
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
import com.example.itin.adapters.TripAdapter
import com.example.itin.classes.Activity
import com.example.itin.classes.Day
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
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_trip.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import android.content.Context

// Toggle Debugging
const val DEBUG_TOGGLE: Boolean = true

class TripActivity : AppCompatActivity(), TripAdapter.OnItemClickListener {

    private lateinit var tripAdapter: TripAdapter
    private lateinit var trips: MutableList<Trip>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var user : User
    private var tripCount : Int = 0
    private lateinit var uid : String
    private lateinit var curUser: DatabaseReference
    private lateinit var curTrips: DatabaseReference
    private lateinit var masterTripList: DatabaseReference
    private lateinit var name: String
    private lateinit var startdate : LocalDate
    private lateinit var formatter : DateTimeFormatter
    private lateinit var startDateObj : LocalDate
    private var pending = 0

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip)

        firebaseAuth = FirebaseAuth.getInstance()

        //create a reminder notification channel
        createNotificationChannel()

        // functions to retrieve FCM token (for notifications)
        // it is here so that no redundant code happens
        // (i.e. everyone is logged in at the point of ItineraryActivity)
        checkToken()

        // check to see if any notifications and set bell accordingly
        isNotifPresent()

        formatter = DateTimeFormatter.ofPattern("M/d/yyyy")

        // set trip list
        trips = mutableListOf()

        // initiate a new object of class TripAdapter, pass in trips list as parameter
        tripAdapter = TripAdapter(this, trips, this)

        // assign adapter for our RecyclerView
        rvTripList.adapter = tripAdapter

        // determine how items are arrange in our list
        rvTripList.layoutManager = LinearLayoutManager(this)
        createTestTrip()

        // what happen when click on AddTodo button -> call the addTrip function
        btAddTrip.setOnClickListener { addTrip() }

        // what happen when click on notfication button -> call the notification activity
        notificationButton.setOnClickListener {
            Intent(this, NotificationListActivity::class.java).also {
                startActivity(it)
            }
        }

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // This here checks the value in the database to overwrite the initial value of 0
        masterTripList = FirebaseDatabase.getInstance().getReference("masterTripList")
        masterTripList.get().addOnSuccessListener {
            if (it.exists()) {
                // Try to grab the value from the DB for tripCount, if it doesn't exist, create the child
                try {
                    tripCount = it.child("tripCount").value.toString().toInt()
                    // check if user is there, then add in previous trips from database
                    checkUser(tripCount)
                } catch (e: NumberFormatException) {
                    masterTripList.child("tripCount").setValue(0)
                }
            } else {
                Log.d("TripActivity", "There is no MasterTripList")
            }
        }

        // This following codes handle Pull-to-Refresh the Days RecyclerView
        // It will clear the days list and load all days from the DB again
        tripsSwipeContainer.setOnRefreshListener {
            tripAdapter.clear()
            createTestTrip()
            readData(tripCount)
            tripsSwipeContainer.isRefreshing = false
        }
        // Configure the refreshing colors
        tripsSwipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light);

        // make the bottom navigation bar
        bottomNavBarSetup()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRestart() {
        super.onRestart()
        tripAdapter.clear()
        createTestTrip()
        readData(tripCount)
    }

    // This function handles RecyclerView that lead you to TripDetails page
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onItemClick(position: Int) {
        // destroy this current iteration of Trip Activity
        // this will let the DB reload from whatever is added on a trip
        //finish()
        // jump to ItineraryActivity
        if(trips[position].pending == 0) {
            Intent(this, ItineraryActivity::class.java).also {
                // pass the current trip object between activities
                it.putExtra("EXTRA_TRIP", trips[position])
                // start ItineraryActivity
                startActivity(it)
            }
        }
    }

    // This function will be called when you click the AddTrip button,
    // a dialog will show up for you to create a new trip
    @RequiresApi(Build.VERSION_CODES.O)
    private fun addTrip() {
        val view = LayoutInflater.from(this).inflate(R.layout.create_trip, null)

        var location = ""
        val etName = view.findViewById<EditText>(R.id.etName)
        val etStartDate = view.findViewById<TextView>(R.id.etStartDate)
        val etEndDate = view.findViewById<TextView>(R.id.etEndDate)
        var endYear = 0
        var endMonth = 0
        var endDay = 0

        // Handle AutoComplete Places Search from GoogleAPI
        if (!Places.isInitialized()) {
            Places.initialize(this,getString(R.string.API_KEY))
        }
        val placesClient = Places.createClient(this)
        val autocompleteFragment = supportFragmentManager.findFragmentById(R.id.etLocation1) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS))
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                location = place.name
                Log.i("Places", "Place: ${place.name}, ${place.id}")
            }
            override fun onError(status: Status) {
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
                    endYear = mYear
                    endMonth = mMonth
                    endDay = mDay
                    Log.d("DATE","$endYear $endMonth $endDay")
                }, year, month, day
            )
            datePickerDialog.datePicker.minDate = startDateObj.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            datePickerDialog.show()
        }

        val newDialog = AlertDialog.Builder(this,R.style.popup_Theme)
        newDialog.setView(view)

        newDialog.setPositiveButton("Add") { dialog, _ ->
            val startDate = etStartDate.text.toString()
            val endDate = etEndDate.text.toString()
            val active: Boolean

            if (location.isBlank() || startDate.isBlank() || endDate.isBlank()) {
                supportFragmentManager.beginTransaction().remove(autocompleteFragment).commit()
                Toast.makeText(this, "Location & Dates are required", Toast.LENGTH_LONG).show()
            } else {
                name = etName.text.toString().ifBlank {
                    "Trip to $location"
                }

                // This event listener waits for a change in its child (tripCount) then records the updated version
                // We must add 1 to this because it records the previous iterations' value
                masterTripList.get().addOnSuccessListener {
                    if (it.exists()) {
                        tripCount = it.child("tripCount").value.toString().toInt() + 1
                    }
                }
                // check for dayInterval to set the trip 'active' status
                val today = LocalDate.now()
                val endDateObj = LocalDate.parse(endDate, formatter)
                val dayInterval = ChronoUnit.DAYS.between(endDateObj, today).toInt()
                active = dayInterval <= 0

                // get the epoch time for the start of the trip
                val tripEpoch = Calendar.getInstance()
                tripEpoch.set(endYear,endMonth,endDay,23,59)
                Log.d("TIME","${tripEpoch.timeInMillis}")
                // Grab the initial values for database manipulation
                val trip = Trip(
                    name,
                    location,
                    startDate,
                    endDate,
                    deleted = false,
                    active,
                    tripID = tripCount,
                    days = mutableListOf(),
                    viewers = mutableMapOf(),
                    epoch = tripEpoch.timeInMillis,
                    pending = 0
                )

                // Write to the database, then increment tripCount in the database
                sendToDB(trip, tripCount)
                tripCount += 1
                //Log.d("TripActivity", "tripCount updated: $tripCount")
                masterTripList.child("tripCount").setValue(tripCount)
                if (active) {
                    trips.add(trip)
                    tripsort(trips)
                    tripAdapter.notifyDataSetChanged()
                }

                supportFragmentManager.beginTransaction().remove(autocompleteFragment).commit()
                scheduleNotification(year,month,day,"Trip Reminder", "$name")
                Toast.makeText(this, "Added a new trip", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        newDialog.setNegativeButton("Cancel") { dialog, _ ->
            supportFragmentManager.beginTransaction().remove(autocompleteFragment).commit()
            dialog.dismiss()
            Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show()
        }
        newDialog.setOnCancelListener {
            supportFragmentManager.beginTransaction().remove(autocompleteFragment).commit()
        }
        newDialog.create()
        newDialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkUser(count: Int) {
        val firebaseUser = firebaseAuth.currentUser
        // If the use is not current logged in:
        if (firebaseUser == null) {
            startActivity(Intent(this, GoogleLogin::class.java))
        } else {
            uid = firebaseUser.uid
            curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
            curTrips = curUser.child("trips")
            // for realtime database, find the current user by its uid
            readData(count)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun readData(count: Int) {

        for (i in 0 until count) {
            curTrips.child("Trip $i").get().addOnSuccessListener {
                if (it.exists()) {
                    accessMasterTripList(i)
                }
            }.addOnCanceledListener {
                Log.d("print", "Failed to fetch the user")
            }
        }
    }

    // function used to access the masterTripList
    @RequiresApi(Build.VERSION_CODES.O)
    private fun accessMasterTripList(i: Int) {

        masterTripList.child("$i").get().addOnSuccessListener {
            if (it.exists()) {
                val tripInstance = masterTripList.child("$i")
                val name = it.child("Name").value.toString()
                val location = it.child("Location").value.toString()
                val stDate = it.child("Start Date").value.toString()
                val endDate = it.child("End Date").value.toString()
                val deleted = it.child("Deleted").value.toString()
                var active = it.child("Active").value.toString()
                var tripId = it.child("ID").value.toString().toInt()
                val epoch = it.child("Epoch").value.toString().toLong()

                // get current time
                val calendar = Calendar.getInstance()
                val calendarTime = calendar.timeInMillis
                if(epoch-calendarTime < 0 && active != false.toString()){
                    active = false.toString()
                    tripInstance.child("Active").setValue("false")
                }

                startdate = LocalDate.parse(stDate, formatter)

                val trip = Trip(
                    name,
                    location,
                    stDate,
                    endDate,
                    stringToBoolean(deleted),
                    stringToBoolean(active),
                    tripId,
                    days = mutableListOf(),
                )
                checkpending(trip)
                if (trip.deleted == stringToBoolean("false") && trip.active == stringToBoolean("true")) {
                    trips.add(trip)
                    readDays(tripInstance, trip)
                    readViewers(tripInstance,trip)
                }
            }
        }
    }

    private fun checkpending(trip: Trip){
        curUser.child("pending trips").get().addOnSuccessListener {
            if(it.child("Trip ${trip.tripID}").exists()){
                trip.pending = 1
                Log.d("pending trip","${trip.tripID} is pending")
            }
        }
        Log.d("pending trip","${trip.tripID} : $pending")
    }

    private fun readViewers(tripInstance: DatabaseReference, trip: Trip) {
        tripInstance.child("Viewers").get().addOnSuccessListener {
            if (it.exists()) {
                // will cycle through the amount of days that we have
                for (i in it.children){
                    if (i.child("uid").value != null && i.child("Perm").value != null){
                    // will make the day classes
                    trip.viewers[i.child("uid").value.toString()] = i.child("Perm").value.toString().toInt()
                    }
                }
            }
        }
    }

    // load days from database
    // do this in trip activity?? then just manipulate the trip directly
    @RequiresApi(Build.VERSION_CODES.O)
    private fun readDays(tripInstance: DatabaseReference, trip: Trip) {
        tripInstance.child("Days").get().addOnSuccessListener {
            if (it.exists()) {
                // will cycle through the amount of days that we have
                for (i in 0 until it.child("DayCount").value.toString().toInt()){
                    // will make the day classes
                    val dayInstance = tripInstance.child("Days").child(i.toString())
                    readDaysHelper(dayInstance, trip)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun readDaysHelper(dayInstance: DatabaseReference, trip: Trip) {
        dayInstance.get().addOnSuccessListener {
            if (it.exists()) {
                // obtain dayNumber, dayInt, and tripID
                var dayNumber = it.child("Day Number").value.toString()
                val dayInt = dayNumber.toInt()
                dayNumber = dayNumber + ": " + startdate.plusDays(dayNumber.toLong()-1).format(formatter).toString()
                val tripID = it.child("TripID").value.toString().toInt()
                //val actCount = it.child("ActivityCount").value.toString().toInt()
                val actList : MutableList<Activity?> = mutableListOf()
                // pull the activity from the DB
                for (i in it.children ) {
                    val name = i.child("name").value.toString()
                    if (name == "null") {break}
                    val location = i.child("location").value.toString()
                    val time = i.child("time").value.toString()
                    val cost = i.child("cost").value.toString()
                    val notes = i.child("notes").value.toString()
                    var tripID = i.child("tripID").value.toString().toInt()
                    var activityID = i.child("actID").value.toString()

                    val activity = Activity(name, time, location, cost, notes, tripID, activityID)
                    actList.add(activity)
                }

                val day = Day(dayNumber,actList,dayInt,tripID)
                trip.days.add(day)
                tripsort(trips)
                tripAdapter.notifyDataSetChanged()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendToDB(trip: Trip, id: Int) {

        // Navigates to the correct directory (masterTripList)
        val tripInstance = masterTripList.child(id.toString())

        tripInstance.child("Name").setValue(trip.name)
        tripInstance.child("Location").setValue(trip.location)
        tripInstance.child("Start Date").setValue(trip.startDate)
        tripInstance.child("End Date").setValue(trip.endDate)
        tripInstance.child("Deleted").setValue(trip.deleted)
        tripInstance.child("Active").setValue(trip.active)
        tripInstance.child("ID").setValue(trip.tripID)
        tripInstance.child("Epoch").setValue(trip.epoch)

        // create days folder
        // will be accessed later in itinerary activity
        val itineraryInstance = tripInstance.child("Days")
        var formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
        var startdate = LocalDate.parse(trip.startDate, formatter)
        var enddate = LocalDate.parse(trip.endDate, formatter)
        val dayNum = ChronoUnit.DAYS.between(startdate, enddate)
        for (i in 0 until dayNum+1) {
            makeDayInstance(itineraryInstance,i.toInt(), trip)
        }

        //create Viewers folder
        tripInstance.child("Viewers").child(uid).child("uid").setValue(uid)
        tripInstance.child("Viewers").child(uid).child("Perm").setValue(1)

        // Record trips in the individual user
        curTrips.child("Trip $id").setValue(id)

    }

    private fun makeDayInstance(itineraryInstance: DatabaseReference, dayNum: Int, trip: Trip) {
        // log the day Count
        itineraryInstance.child("DayCount").setValue(dayNum+1)
        val dayInstance = itineraryInstance.child(dayNum.toString())
        dayInstance.child("Day Number").setValue(dayNum + 1)
        dayInstance.child("TripID").setValue(trip.tripID)
        dayInstance.child("ActivityCount").setValue(0)
    }

    // function to set up the bottom navigation bar
    private fun bottomNavBarSetup() {
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
                R.id.ic_friends -> {
                    Intent(this, FriendActivity::class.java).also {
                        startActivity(it)
                    }
                }
            }
            true
        }
    }

    // convert a string to a boolean
    private fun stringToBoolean(str: String): Boolean {
        if (str == "false") {
            return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    // function to sort the activities on each of the day, it is a modified Insertion sort
    private fun tripsort (trips: MutableList<Trip>){
        var formatter = DateTimeFormatter.ofPattern("M/d/yyyy")

        for (i in 0 until trips.size) {
            val key = trips[i]

            var j = i - 1

            if (key != null) {
                while (j >= 0 && LocalDate.parse(trips[j].startDate, formatter).isAfter(LocalDate.parse(key.startDate, formatter))){
                    trips[j + 1] = trips[j]
                    j--
                }
            }
            trips[j + 1] = key
        }
    }

    private fun checkToken() {
        FirebaseService.sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        // get to the current user in the DB
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val uid = firebaseUser!!.uid
        curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
        // retrieve the token
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            // set the token of the user
            FirebaseService.token = it
            curUser.child("userInfo").child("token").setValue(it)
        }
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)
    }

    // function for creating the reminder notification
    @RequiresApi(Build.VERSION_CODES.M)
    private fun scheduleNotification(year:Int, month:Int, day:Int, title:String, tripName:String)
    {
        val message = "$tripName is happening today!"
        val intent = Intent(applicationContext, ReminderNotification::class.java)
        intent.putExtra(titleExtra, title)
        intent.putExtra(messageExtra, message)

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notificationID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, 0, 0)
        val remindTime = calendar.timeInMillis
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            remindTime,
            pendingIntent
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel()
    {
        val name = "Notif Channel"
        val desc = "A Description of the Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelID, name, importance)
        channel.description = desc
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    //Creating Testing Trip ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private fun createTestTrip() {
        if (DEBUG_TOGGLE) {
            val day1 = Day("1", mutableListOf(),1,-1)
            val day2 = Day("2", mutableListOf(),2,-1)
            val daylist = mutableListOf<Day>(day1,day2)
            val trip = Trip(
                "Trip to TEST",
                "TEST",
                "1/1/2022",
                "1/2/2022",
                deleted = false,
                active = true,
                tripID = -1,
                days = daylist,
                viewers = mutableMapOf("CNIyURFyEhRrb1sZNLJo47yMF4o2" to 1,"LW4U6jdzqqcdLvqMMdw7tt1M9b73" to 2,"dwJLMqs0Y5M65fmvS4lIJS5xFgf1" to 2,"eZuf0wlulMe64K6ZXgFPBXTlFJs1" to 2,"JFn2cxxk1xWl83eXDWsXf5fSwvu1" to 2,"uSWyidP8E2axSFnBf1WZgGlcUgF3" to 2)
            )
            trips.add(trip)
            tripAdapter.notifyDataSetChanged()
        }
    }

    private fun isNotifPresent(){
        val firebaseUser = firebaseAuth.currentUser
        if(firebaseUser != null){
            uid = firebaseUser.uid
            curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
            curUser.get().addOnSuccessListener {
                if (it.hasChild("notifications")) {
                    notificationButton.setImageResource(R.drawable.ic_new_notification)
                }
                else {
                    notificationButton.setImageResource(R.drawable.ic_notifications)
                }
            }
        }
    }
}