package com.example.itin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.adapters.ShareAdapter
import com.example.itin.classes.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_share_trip.*

class ShareTripActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var uid : String
    private lateinit var curUser: DatabaseReference
    private var friend:Boolean = false
    private lateinit var masterUserList: DatabaseReference
    private var userCount: Int = 0
    private var friends: MutableList<User> = mutableListOf()
    private lateinit var shareAdapter: ShareAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_trip)
        firebaseAuth = FirebaseAuth.getInstance()

        val tripID = intent.getStringExtra("TRIP_ID")?.toInt()

        // initiate a new object of class ShareAdapter, pass in friends list as parameter
        shareAdapter = ShareAdapter(this, friends, tripID)

        // assign adapter for our RecyclerView
        rvShareFriends.adapter = shareAdapter

        // determine how items are arrange in our list
        rvShareFriends.layoutManager = LinearLayoutManager(this)

        shareAdapter.notifyDataSetChanged()

        ibShareUsername.setOnClickListener { ShareByUsername(etUsername.text.toString(),tripID) }

        firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser = firebaseAuth.currentUser
        uid = firebaseUser!!.uid
        curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
        masterUserList = FirebaseDatabase.getInstance().getReference("masterUserList")

        masterUserList.get().addOnSuccessListener {
            if (it.exists()) {
                // Try to grab the value from the DB for tripCount, if it doesn't exist, create the child
                try {
                    userCount = it.child("userCount").value.toString().toInt()
                    Log.d("FriendActivity", "userCount: $userCount")
                    readData(userCount)
                } catch (e: NumberFormatException) {
                    masterUserList.child("userCount").setValue(0)
                }
            } else {
                Log.d("FriendActivity", "The user that is logged in doesn't exist?")
            }
        }
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

    private fun readData(userCount: Int) {
        Log.d("ShareTrip", "Reading Data")
        val firebaseUser = firebaseAuth.currentUser
        val uid = firebaseUser!!.uid
        var curFriend = FirebaseDatabase.getInstance().getReference("users").child(uid).child("friendsList")

        for (i in 0 until userCount)
            curFriend.child("Friend $i").get().addOnSuccessListener {
                if (it.exists()) {
                    var curID = it.value
                    Log.d("ShareTrip", "curID: $curID")
                    if (curID != null) {
                        friend = true
                        accessMasterUserList(curID,friend)
                        Log.d("ShareTrip", "CurID: $curID")
                    }
                } else {
                    Log.d("ShareTrip", "Friend does not exist")
                }
            }.addOnCanceledListener {
                Log.d("ShareTrip", "Failed to fetch the Friend")
            }
    }

    private fun accessMasterUserList(curID: Any, friend: Boolean) {
        val masterUserList = FirebaseDatabase.getInstance().getReference("masterUserList")
        val curFriend = FirebaseDatabase.getInstance().getReference("users")

        masterUserList.child(curID.toString()).get().addOnSuccessListener {
            if (it.exists()) {
                val friendsUid = it.child("UID").value.toString()

                curFriend.child(friendsUid).get().addOnSuccessListener {
                    if (it.exists()) {
                        val username = it.child("userInfo").child("username").value.toString()
                        val uid = it.child("userInfo").child("uid").value.toString()
                        val fullName = it.child("userInfo").child("fullName").value.toString()

                        val user = User(
                            uid,
                            fullName,
                            username,
                            "null",
                            "null"
                        )
                        friends.add(user)
                        shareAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }
}