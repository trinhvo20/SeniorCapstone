package com.example.itin

import android.app.TimePickerDialog
import android.content.Intent
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
        tvStartDate.text = activity.cost
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

        val etName = view.findViewById<EditText>(R.id.etName)
        val tvTime = view.findViewById<TextView>(R.id.tvTime)
        val etLocation = view.findViewById<EditText>(R.id.etLocation)
        val etCost = view.findViewById<EditText>(R.id.etCost)
        val etNotes = view.findViewById<EditText>(R.id.etNotes)

        // auto fill fields with existing data, very convenient
        etName.setText(activity.name)
        tvTime.text = activity.time
        etLocation.setText(activity.location)
        etCost.setText(activity.cost)
        etNotes.setText(activity.notes)

        val ibTimePicker = view.findViewById<View>(R.id.ibTimePick)
        ibTimePicker.setOnClickListener{
            val hour = 1
            val minute = 10

            val tpd = TimePickerDialog(this,
                TimePickerDialog.OnTimeSetListener(function = { view, h, m ->

                //Toast.makeText(this, h.toString() + " : " + m , Toast.LENGTH_LONG).show()
                var input = h.toString() + ":" + m

                val df = SimpleDateFormat("H:m")
                val outputFormat = SimpleDateFormat("h:ma")
                tvTime.text = outputFormat.format(df.parse(input))


            }),hour,minute,false)

            tpd.show()
        }

        val newDialog = AlertDialog.Builder(this)
        newDialog.setView(view)

        newDialog.setPositiveButton("Edit") { dialog, _ ->
            val location = etLocation.text.toString()
            activity.cost = etCost.text.toString()
            activity.notes = etNotes.text.toString()
            activity.time = tvTime.text.toString()

            activity.name = if (etName.text.toString().isEmpty()) {
                "$location"
            } else {
                etName.text.toString()
            }
            activity.location = location

            sendEditedActivityToDB(activity)
            Toast.makeText(this, "Activity Edited", Toast.LENGTH_SHORT).show()
            dialog.dismiss()

            //reload the activity with no transition
            finish();
            overridePendingTransition(0, 0);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }

        newDialog.setNegativeButton("Cancel") { dialog, _ ->
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