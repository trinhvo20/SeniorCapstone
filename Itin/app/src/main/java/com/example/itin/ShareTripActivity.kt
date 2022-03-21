package com.example.itin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.adapters.ShareAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_share_trip.*

class ShareTripActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private var tripCount: Int = 0
    private lateinit var uid : String
    private lateinit var curUser: DatabaseReference
    private lateinit var curTrips: DatabaseReference
    private lateinit var masterTripList: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_trip)
        firebaseAuth = FirebaseAuth.getInstance()

        val tripID = intent.getStringExtra("TRIP_ID")?.toInt()

        val Friends = mutableListOf<String>("test1","test2","test3")

        // initiate a new object of class ShareAdapter, pass in friends list as parameter
        var shareAdapter = ShareAdapter(this, Friends)

        // assign adapter for our RecyclerView
        rvShareFriends.adapter = shareAdapter

        // determine how items are arrange in our list
        rvShareFriends.layoutManager = LinearLayoutManager(this)

        shareAdapter.notifyDataSetChanged()

        ibShareUsername.setOnClickListener { ShareByUsername(etUsername.text.toString(),tripID) }
    }

    private fun ShareByUsername(Username: String, tripID: Int?) {

        // Base firebase variables
        val firebaseUser = firebaseAuth.currentUser
        val uid = firebaseUser!!.uid

        // Navigates to the directories within the database that we will be manipulating
        val masterUserList = FirebaseDatabase.getInstance().getReference("masterUserList")
        curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
        // Takes the given username and finds the associated UID
        val friendsID = masterUserList.child(Username)

        // If the friends username exists, add them to the current users profile, else tell the user that it does not exist
        friendsID.get().addOnSuccessListener {
            if (it.exists()) {
                val friendsIDStr = it.value.toString()

                curUser.get().addOnSuccessListener {
                    if (it.exists()) {
                        masterUserList.get().addOnSuccessListener {
                            if (it.exists()) {
                                val friendsUID =
                                    it.child(friendsIDStr).child("UID").value.toString()
                                FirebaseDatabase.getInstance().getReference("users")
                                    .child(friendsUID).child("trips").child("Trip $tripID")
                                    .setValue(tripID)
                                Toast.makeText(this, "Trip Shared", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(this, "User does not exist", Toast.LENGTH_SHORT).show()
            }
        }
    }
}