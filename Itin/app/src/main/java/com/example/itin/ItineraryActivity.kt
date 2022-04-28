package com.example.itin

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_details.*
import kotlinx.android.synthetic.main.activity_itinerary.*
import kotlinx.android.synthetic.main.activity_itinerary.backBtn
import kotlinx.android.synthetic.main.activity_itinerary.tvName
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
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
    private val PICK_IMAGE = 100
    private lateinit var storageReference: StorageReference
    private lateinit var imageUri: Uri
    private lateinit var startDateObj : LocalDate
    private var totalCountViewers : Int = 0

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            )
        }

        firebaseAuth = FirebaseAuth.getInstance()
        masterTripList = FirebaseDatabase.getInstance().getReference("masterTripList")
        databaseReference = FirebaseDatabase.getInstance().reference
        firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser = firebaseAuth.currentUser
        uid = firebaseUser!!.uid

        // get the trip object from MainActivity
        trip = intent.getSerializableExtra("EXTRA_TRIP") as Trip

        tvName.text = trip.name
        tvTripLocation.text = trip.location
        tvDateRange.text = "From: ${trip.startDate}     To: ${trip.endDate}"
        Log.d("Itinerary", "TripID: ${trip.tripID}")
        getTripImage()

        days = trip.days
        dayAdapter = DayAdapter(this, days, this, trip.viewers)
        rvActivityList.adapter = dayAdapter
        rvActivityList.layoutManager = LinearLayoutManager(this)

        // get the total number of viewers in this trip from the database
        val tripID = trip.tripID.toString()
        databaseReference.child("masterTripList").child(tripID).child("Viewers")
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(data: DataSnapshot) {
                    totalCountViewers = data.childrenCount.toInt()
                }
                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        // need these for proper formatting from the DB
        formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
        startdate = LocalDate.parse(trip.startDate, formatter)
        var enddate = LocalDate.parse(trip.endDate, formatter)

        activitySort(days)
        loadDaysFromDB()
        dayAdapter.notifyDataSetChanged()

        btExpandMenu.setOnClickListener { onExpandButtonClicked() }
        shareBtn.setOnClickListener { onShareClicked() }
        editBtn.setOnClickListener { editTrip(trip) }

        backBtn.setOnClickListener {
            finish()
            //if active go to TripActivity, if not active go to Previous trips
            if (trip.active) {// start TripActivity
                Intent(this, TripActivity::class.java).also {
                    startActivity(it)
                }
            } else {// start PreviousTripActivity
                Intent(this, PreviousTripActivity::class.java).also {
                    startActivity(it)
                }
            }
        }

        photoLibraryBtn.setOnClickListener { openGallery() }

        chatBoxBtn.setOnClickListener {
            Intent(this, ChatActivity::class.java).also {
                it.putExtra("trip", trip)
                startActivity(it)
            }
        }

        viewerBtn.setOnClickListener {
            Intent(this, ViewerActivity::class.java).also {
                it.putExtra("trip", trip)
                it.putExtra("uid", uid)
                startActivity(it)
            }
        }

        // This following codes handle Pull-to-Refresh the Days RecyclerView
        // It will clear the days list and load all days from the DB again
        swipeContainer.setOnRefreshListener {
            dayAdapter.clear()
            loadDaysFromDB()
            dayAdapter.notifyDataSetChanged()
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRestart() {
        super.onRestart()
        dayAdapter.clear()
        loadDaysFromDB()
    }

    override fun onItemClick(position: Int, daypos: Int) {
        Intent(this, DetailsActivity::class.java).also {
            // pass the current trip object between activities
            it.putExtra("ACTIVITY", days[daypos][position])
            it.putExtra("DAY_ID", days[daypos].dayInt)
            it.putExtra("CUR_VIEWER", trip.viewers[uid])
            it.putExtra("VIEWER_LIST", totalCountViewers)
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
                activitySort(days)
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

    private fun openGallery() {
        if (trip.viewers[uid] == 1) {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, PICK_IMAGE)
        }
        else {
            Toast.makeText(
                this,
                "You do not have permission to preform this action",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // handle the profile_picture change
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            if (data != null) {
                imageUri = data.data!!
                Log.d("Itinerary", imageUri.toString())
                val file = File(getRealPathFromURI(imageUri))
                if (file.exists()) {
                    val d = Drawable.createFromPath(file.absolutePath)
                    tripHeader.background = d
                }
                uploadTripPic()
            }
        }
    }

    private fun uploadTripPic() {
        val tripId = trip.tripID.toString()
        storageReference = FirebaseStorage.getInstance().getReference("Trips/$tripId.jpg")
        storageReference.putFile(imageUri).addOnSuccessListener {
            Toast.makeText(
                this@ItineraryActivity,
                "Profile successfully updated",
                Toast.LENGTH_SHORT
            ).show()
            getTripImage()
        }.addOnFailureListener {
            Toast.makeText(this@ItineraryActivity, "Failed to upload image", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun getTripImage() {
        Log.d("ItineraryImage", "Getting Trip Image from DB")
        val tripId = trip.tripID.toString()
        storageReference = FirebaseStorage.getInstance().getReference("Trips/$tripId.jpg")
        val localFile = File.createTempFile("tempImage", "jpg")
        storageReference.getFile(localFile).addOnSuccessListener {
            val d = Drawable.createFromPath(localFile.absolutePath)
            tripHeader.background = d
        }.addOnFailureListener {
            Log.d("ItineraryImage", "Failed to retrieve image")
        }
    }

    private fun getRealPathFromURI(contentURI: Uri): String? {
        val cursor: Cursor? = contentResolver.query(contentURI, null, null, null, null)
        return if (cursor == null) { // Source is Dropbox or other similar local file path
            contentURI.path
        } else {
            cursor.moveToFirst()
            val idx: Int = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            cursor.getString(idx)
        }
    }

    private fun onExpandButtonClicked() {
        setUsability(clicked)
        setAnimation(clicked)
        clicked = !clicked
    }

    private fun setAnimation(clicked: Boolean) {
        if (!clicked) {
            chatBoxBtn.startAnimation(fromBottom)
            shareBtn.startAnimation(fromBottom)
            editBtn.startAnimation(fromBottom)
            viewerBtn.startAnimation(fromBottom)
            btExpandMenu.startAnimation(rotateOpen)
        } else {
            chatBoxBtn.startAnimation(toBottom)
            shareBtn.startAnimation(toBottom)
            editBtn.startAnimation(toBottom)
            viewerBtn.startAnimation(toBottom)
            btExpandMenu.startAnimation(rotateClose)
        }
    }

    private fun setUsability(clicked: Boolean) {
        if (!clicked) {
            chatBoxBtn.visibility = View.VISIBLE
            shareBtn.visibility = View.VISIBLE
            editBtn.visibility = View.VISIBLE
            viewerBtn.visibility = View.VISIBLE

            chatBoxBtn.isClickable = true
            shareBtn.isClickable = true
            editBtn.isClickable = true
            viewerBtn.isClickable = true

        } else {
            chatBoxBtn.visibility = View.INVISIBLE
            shareBtn.visibility = View.INVISIBLE
            editBtn.visibility = View.INVISIBLE
            viewerBtn.visibility = View.INVISIBLE

            chatBoxBtn.isClickable = false
            shareBtn.isClickable = false
            editBtn.isClickable = false
            viewerBtn.isClickable = false
        }
    }

    private fun onShareClicked() {
        val nit = Intent(this, ShareTripActivity::class.java).apply {
            putExtra("TRIP", trip)
        }
        //Toast.makeText(this, trip.tripID.toString(), Toast.LENGTH_SHORT).show()
        this.startActivity(nit)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun editTrip(curTrip: Trip) {
            //TODO: "Add epoch restraints for previous trip to current trip"
            if (curTrip.viewers[uid] == 1) {
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
                Log.d(
                    "Places",
                    this.supportFragmentManager.findFragmentById(R.id.etLocation2).toString()
                )
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

                val dialog = AlertDialog.Builder(this,R.style.popup_Theme)
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

                        this.supportFragmentManager.beginTransaction().remove(autocompleteFragment)
                            .commit()
                        Toast.makeText(this, "Successfully Edited", Toast.LENGTH_SHORT)
                            .show()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        this.supportFragmentManager.beginTransaction().remove(autocompleteFragment)
                            .commit()
                        dialog.dismiss()
                    }
                    .setOnCancelListener {
                        this.supportFragmentManager.beginTransaction().remove(autocompleteFragment).commit()
                    }
                    .create()
                    .show()
            } else {
                Toast.makeText(
                    this,
                    "You do not have permission to perform this action",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}