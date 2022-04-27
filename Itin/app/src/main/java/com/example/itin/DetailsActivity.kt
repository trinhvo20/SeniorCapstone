package com.example.itin

import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.adapters.CheckInAdapter
import com.example.itin.classes.Activity
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_details.*


class DetailsActivity : AppCompatActivity() {
    private lateinit var databaseReference: DatabaseReference
    private var dayID : Int = 0
    private lateinit var checkInAdapter: CheckInAdapter
    private lateinit var checkInList: MutableList<String>
    private lateinit var uid : String
    private lateinit var activity : Activity
    private var countCheckIn : Int = 0
    private var countViewers : Int = 0
    private var cur_viewer:Int = 2

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        databaseReference = FirebaseDatabase.getInstance().reference
        checkUser()

        dayID = intent.getIntExtra("DAY_ID", 0) - 1
        activity = intent.getSerializableExtra("ACTIVITY") as Activity
        cur_viewer = intent.getIntExtra("CUR_VIEWER",2)
        countViewers = intent.getIntExtra("VIEWER_LIST", 0)
        countTotal.text = countViewers.toString()

        //filling in information
        tvName.text = activity.name
        tvTime.text = activity.time
        tvLocation.text = activity.location.substringBefore("\n")
        tvAddress.text = activity.location.substringAfter("\n")
        tvCost.text = activity.cost
        tvNotes.text = activity.notes
        tvAddress.paintFlags = tvAddress.paintFlags or Paint.UNDERLINE_TEXT_FLAG


        checkInList = mutableListOf()
        checkInAdapter = CheckInAdapter(checkInList)
        checkinRV.adapter = checkInAdapter
        checkinRV.layoutManager = LinearLayoutManager(this)
        checkinBtn.isClickable = true
        checkinBtn.isVisible = true
        checkoutBtn.isClickable = false
        checkoutBtn.isVisible = false
        checkinStatus.text = "You Here?"
        loadCheckInFromDB()

        btEdit.setOnClickListener{ editActivity(activity) }
        backBtn.setOnClickListener { finish() }
        checkinBtn.setOnClickListener{ sendCheckInToDB() }
        checkoutBtn.setOnClickListener{ deleteCheckInFromDB() }
        copyAddressBtn.setOnClickListener { copyAddress() }
        tvAddress.setOnClickListener { clickAddress() }
    }

    private fun checkUser() {
        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            startActivity(Intent(this, GoogleLogin::class.java))
        }
        else {
            uid = firebaseUser.uid
        }
    }

    private fun clickAddress() {

        val address = activity.location.substringAfter("\n")
        val gmmIntentUri = Uri.parse("geo:0,0?q=$address")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        mapIntent.resolveActivity(packageManager)?.let {
            startActivity(mapIntent)
        }
    }

    private fun copyAddress() {
        val address = activity.location.substringAfter("\n")
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("text", address)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(this, "Copied", Toast.LENGTH_LONG).show()
    }

    // function to edit the activity
    @RequiresApi(Build.VERSION_CODES.N)
    private fun editActivity(activity: Activity) {
        if (cur_viewer == 1) {
            val view = LayoutInflater.from(this).inflate(R.layout.edit_activity, null)

            var location = activity.location
            val etName = view.findViewById<EditText>(R.id.etName)
            val tvTime = view.findViewById<TextView>(R.id.tvTime)
            val etCost = view.findViewById<EditText>(R.id.etCost)
            val etNotes = view.findViewById<EditText>(R.id.etNotes)

            // auto fill fields with existing data, very convenient
            etName.setText(activity.name)
            tvTime.text = activity.time
            etCost.setText(activity.cost)
            etNotes.setText(activity.notes)

            // Handle AutoComplete Places Search from GoogleAPI
            if (!Places.isInitialized()) {
                Places.initialize(this, getString(R.string.API_KEY))
            }
            val placesClient = Places.createClient(this)
            val autocompleteFragment =
                supportFragmentManager.findFragmentById(R.id.etEditedActLocation) as AutocompleteSupportFragment
            autocompleteFragment.setPlaceFields(
                listOf(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.ADDRESS
                )
            )
            autocompleteFragment.setText(location)
            autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
                override fun onPlaceSelected(place: Place) {
                    location = "${place.name}\n${place.address}"
                    Log.i("Places", "Place: ${place.name}, ${place.id}")
                }

                override fun onError(status: Status) {
                    Log.i("Places", "An error occurred: $status")
                }
            })

            val ibTimePicker = view.findViewById<View>(R.id.ibTimePick)
            ibTimePicker.setOnClickListener {
                val hour = 1
                val minute = 10

                val tpd = TimePickerDialog(this,
                    TimePickerDialog.OnTimeSetListener(function = { view, h, m ->
                        var input = "$h:$m"

                        val df = SimpleDateFormat("H:mm")
                        val outputFormat = SimpleDateFormat("h:mm a")
                        tvTime.text = outputFormat.format(df.parse(input))


                    }), hour, minute, false
                )

                tpd.show()
            }

            val newDialog = AlertDialog.Builder(this,R.style.popup_Theme)
            newDialog.setView(view)

            newDialog.setPositiveButton("Edit") { dialog, _ ->
                val name = etName.text.toString()
                val cost = etCost.text.toString()
                val notes = etNotes.text.toString()
                val time = tvTime.text.toString()

                if (name == activity.name) {
                    if (location != activity.location) {
                        activity.name = location.substringBefore("\n")
                    }
                } else {
                    activity.name = name
                }
                if (location != activity.location) {
                    activity.location = location
                }
                if (cost != activity.cost) {
                    activity.cost = cost
                }
                if (notes != activity.notes) {
                    activity.notes = notes
                }
                if (time != activity.time) {
                    activity.time = time
                }

                sendEditedActivityToDB(activity)
                supportFragmentManager.beginTransaction().remove(autocompleteFragment).commit()
                Toast.makeText(this, "Activity Edited", Toast.LENGTH_SHORT).show()
                dialog.dismiss()

                //reload the activity with no transition
                finish();
                overridePendingTransition(0, 0);
                startActivity(intent);
                overridePendingTransition(0, 0);
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
        else{
            Toast.makeText(this, "You do not have permission to preform this action", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendEditedActivityToDB(activity: Activity) {
        val tripID = activity.tripID.toString()
        val actID = activity.actID
        var curActivity = databaseReference.child("masterTripList").child(tripID)
            .child("Days").child(dayID.toString()).child(actID)

        curActivity.child("cost").setValue(activity.cost)
        curActivity.child("location").setValue(activity.location)
        curActivity.child("name").setValue(activity.name)
        curActivity.child("notes").setValue(activity.notes)
        curActivity.child("time").setValue(activity.time)
    }

    private fun sendCheckInToDB() {
        val tripID = "trip ${activity.tripID}"
        val dayID = "day $dayID"
        val actID = "activity ${activity.actID}"
        databaseReference.child("checkIn"). child(tripID).child(dayID).child(actID).child(uid).setValue(uid)
    }

    private fun loadCheckInFromDB() {
        val tripID = "trip ${activity.tripID}"
        val dayID = "day $dayID"
        val actID = "activity ${activity.actID}"
        databaseReference.child("checkIn"). child(tripID).child(dayID).child(actID)
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(data: DataSnapshot) {
                    checkInList.clear()     // clear the old list to load new list
                    for (eachData in data.children) {
                        val checkInUid = eachData.value
                        if (uid == checkInUid){
                            checkinBtn.isClickable = false
                            checkinBtn.isVisible = false
                            checkoutBtn.isClickable = true
                            checkoutBtn.isVisible = true
                            checkinStatus.text = "Leaving?"
                        }
                        checkInList.add(checkInUid as String)
                    }
                    checkInAdapter.notifyDataSetChanged()
                    countCheckIn = data.childrenCount.toInt()
                    count.text = countCheckIn.toString()
                }
                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
    }

    private fun deleteCheckInFromDB() {
        val tripID = "trip ${activity.tripID}"
        val dayID = "day $dayID"
        val actID = "activity ${activity.actID}"
        databaseReference.child("checkIn"). child(tripID).child(dayID).child(actID)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(data: DataSnapshot) {
                    for (eachData in data.children) {
                        val checkInUid = eachData.value
                        if (uid == checkInUid){
                            eachData.ref.removeValue()
                            checkinBtn.isClickable = true
                            checkinBtn.isVisible = true
                            checkoutBtn.isClickable = false
                            checkoutBtn.isVisible = false
                            checkinStatus.text = "You Here?"
                        }
                        checkInList.remove(checkInUid as String)
                    }
                    checkInAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
    }
}