package com.example.itin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.itin.adapters.TripAdapter
import com.example.itin.classes.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import kotlinx.android.synthetic.main.activity_previous_trip.*

class PreviousTripActivity : AppCompatActivity() {

    private lateinit var tripAdapter : TripAdapter
    private lateinit var trips : MutableList<Trip>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var uid : String
    private lateinit var curUser: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_previous_trip)

        backBtn.setOnClickListener { finish() }
    }

}