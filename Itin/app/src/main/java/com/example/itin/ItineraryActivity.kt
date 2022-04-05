package com.example.itin

import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.adapters.ActivityAdapter
import com.example.itin.classes.Activity
import com.example.itin.classes.Day
import com.example.itin.classes.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_itinerary.*
import kotlinx.android.synthetic.main.activity_profile_screen.*
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter


// for notifications / FCM
const val TOPIC = "/topics/myTopic2"

class ItineraryActivity : AppCompatActivity(), ActivityAdapter.OnItemClickListener {
    private lateinit var dayAdapter : DayAdapter
    lateinit var days: MutableList<Day>
    private lateinit var trip : Trip
    private lateinit var uid : String
    private lateinit var startdate : LocalDate
    private lateinit var formatter : DateTimeFormatter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var masterTripList: DatabaseReference
    private val PICK_IMAGE = 100
    private lateinit var storageReference: StorageReference
    private lateinit var imageUri: Uri

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

        // get the trip object from MainActivity
        trip = intent.getSerializableExtra("EXTRA_TRIP") as Trip

        tvName.text = trip.name
        tvTripLocation.text = trip.location
        tvDateRange.text = "From: ${trip.startDate}     To: ${trip.endDate}"
        Log.d("Itinerary","TripID: ${trip.tripID}")
        getTripImage()

        days = trip.days

        // initiate a new object of class DayAdapter, pass in days list as parameter
        dayAdapter = DayAdapter(this,days,this)
        rvActivityList.adapter = dayAdapter
        rvActivityList.layoutManager = LinearLayoutManager(this)

        // need these for proper formatting from the DB
        formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
        startdate = LocalDate.parse(trip.startDate, formatter)
        var enddate = LocalDate.parse(trip.endDate, formatter)

        activitySort(days)
        dayAdapter.notifyDataSetChanged()

        backBtn.setOnClickListener {
            finish()
            //if active go to TripActivity, if not active go to Previous trips
            if(trip.active) {// start TripActivity
                Intent(this, TripActivity::class.java).also {
                    startActivity(it)
                }
            }else{// start PreviousTripActivity
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

        // This following codes handle Pull-to-Refresh the Days RecyclerView
        // It will clear the days list and load all days from the DB again
        swipeContainer.setOnRefreshListener {
            dayAdapter.clear()
            loadDaysFromDB()
            swipeContainer.isRefreshing = false
        }
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light);
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
                    for (i in 0 until it.child("DayCount").value.toString().toInt()){
                        // will make the day classes
                        val dayInstance = databaseReference.child("masterTripList").child(tripID).child("Days").child(i.toString())
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
                dayNumber = dayNumber + ": " + startdate.plusDays(dayNumber.toLong()-1).format(formatter).toString()
                val tripID = it.child("TripID").value.toString().toInt()
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
                days.add(day)
                dayAdapter.notifyDataSetChanged()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    // function to sort the activities on each of the day, it is a modified Insertion sort
    private fun activitySort (tempDays : MutableList<Day>){
        var formatter = DateTimeFormatter.ofPattern("h:mm a")

        for(day in tempDays) {
            for (i in 0 until day.activities.size) {
                val key = day.activities[i]

                var j = i - 1

                if (key != null) {
                    while (j >= 0 && LocalTime.parse(day.activities[j]?.time, formatter).isAfter(LocalTime.parse(key.time, formatter))){
                        day.activities[j + 1] = day.activities[j]
                        j--
                    }
                }
                day.activities[j + 1] = key
            }
        }
    }

    private fun openGallery() {
        val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(gallery, PICK_IMAGE)
    }

    // handle the profile_picture change
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE){
            if (data != null) {
                imageUri = data.data!!
                Log.d("Itinerary",imageUri.toString())
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
            Toast.makeText(this@ItineraryActivity,"Profile successfully updated", Toast.LENGTH_SHORT).show()
            getTripImage()
        }.addOnFailureListener {
            Toast.makeText(this@ItineraryActivity,"Failed to upload image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getTripImage() {
        Log.d("ItineraryImage","Getting Trip Image from DB")
        val tripId = trip.tripID.toString()
        storageReference = FirebaseStorage.getInstance().getReference("Trips/$tripId.jpg")
        val localFile = File.createTempFile("tempImage","jpg")
        storageReference.getFile(localFile).addOnSuccessListener {
            val d = Drawable.createFromPath(localFile.absolutePath)
            tripHeader.background = d
        }.addOnFailureListener {
            Log.d("ItineraryImage","Failed to retrieve image")
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
}