package com.example.itin

import android.app.TimePickerDialog
import android.icu.text.SimpleDateFormat
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
import com.example.itin.classes.Activity
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_details.*

class DetailsActivity : AppCompatActivity() {
    private lateinit var databaseReference: DatabaseReference
    private var dayID : Int = 0

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        databaseReference = FirebaseDatabase.getInstance().reference

        dayID = intent.getIntExtra("DAY_ID", 0) - 1
        val activity = intent.getSerializableExtra("ACTIVITY") as Activity

        //filling in information
        tvName.text = activity.name
        tvTime.text = activity.time
        tvLocation.text = activity.location
        tvCost.text = activity.cost
        tvNotes.text = activity.notes

        btEdit.setOnClickListener{editActivity(activity)}

        backBtn.setOnClickListener {
            finish()
        }

    }

    // function to edit the activity
    @RequiresApi(Build.VERSION_CODES.N)
    private fun editActivity(activity: Activity) {
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
            Places.initialize(this,getString(R.string.API_KEY))
        }
        val placesClient = Places.createClient(this)
        val autocompleteFragment = supportFragmentManager.findFragmentById(R.id.etLocation) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS))
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
        ibTimePicker.setOnClickListener{
            val hour = 1
            val minute = 10

            val tpd = TimePickerDialog(this,
                TimePickerDialog.OnTimeSetListener(function = { view, h, m ->
                var input = "$h:$m"

                val df = SimpleDateFormat("H:mm")
                val outputFormat = SimpleDateFormat("h:mm a")
                tvTime.text = outputFormat.format(df.parse(input))


            }),hour,minute,false)

            tpd.show()
        }

        val newDialog = AlertDialog.Builder(this)
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

        newDialog.create()
        newDialog.show()
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
}