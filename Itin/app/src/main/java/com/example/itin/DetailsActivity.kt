package com.example.itin

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.itin.classes.Activity
import kotlinx.android.synthetic.main.activity_details.*

class DetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        val activity = intent.getSerializableExtra("ACTIVITY") as Activity

        //filling in information
        tvName.text = activity.name
        tvTime.text = activity.time
        tvLocation.text = activity.location
        tvCost.text = activity.cost
        tvNotes.text = activity.notes

        btEdit.setOnClickListener{editActivity(activity)}




    }

    // function to edit the activity
    private fun editActivity(activity: Activity) {
        val view = LayoutInflater.from(this).inflate(R.layout.edit_activity, null)

        val etName = view.findViewById<EditText>(R.id.etName)
        val etTime = view.findViewById<EditText>(R.id.etTime)
        val etLocation = view.findViewById<EditText>(R.id.etLocation)
        val etCost = view.findViewById<EditText>(R.id.etCost)
        val etNotes = view.findViewById<EditText>(R.id.etNotes)

        // auto fill fields with existing data, very convenient
        etName.setText(activity.name)
        etTime.setText(activity.time)
        etLocation.setText(activity.location)
        etCost.setText(activity.cost)
        etNotes.setText(activity.notes)


        val newDialog = AlertDialog.Builder(this)
        newDialog.setView(view)

        newDialog.setPositiveButton("Edit") { dialog, _ ->
            val location = etLocation.text.toString()
            activity.cost = etCost.text.toString()
            activity.notes = etNotes.text.toString()
            activity.time = etTime.text.toString()

            activity.name = if (etName.text.toString().isEmpty()) {
                "$location"
            } else {
                etName.text.toString()
            }
            activity.location = location

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
}