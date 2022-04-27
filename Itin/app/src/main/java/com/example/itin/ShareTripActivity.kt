package com.example.itin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.adapters.ShareAdapter
import com.example.itin.classes.Trip
import com.example.itin.classes.User
import com.example.itin.notifications.NotificationData
import com.example.itin.notifications.PushNotification
import com.example.itin.notifications.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_friend.*
import kotlinx.android.synthetic.main.activity_share_trip.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShareTripActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var uid : String
    private lateinit var curUser: DatabaseReference
    private var friend:Boolean = false
    private lateinit var masterUserList: DatabaseReference
    private var userCount: Int = 0
    private var friends: MutableList<User> = mutableListOf()
    private lateinit var shareAdapter: ShareAdapter
    // Variable for error messages
    private val TAG = "ShareTripActivity"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_trip)
        firebaseAuth = FirebaseAuth.getInstance()

        val Curtrip = intent.getSerializableExtra("TRIP") as Trip
        val tripID = Curtrip.tripID
        val Viewers = Curtrip.viewers

        // initiate a new object of class ShareAdapter, pass in friends list as parameter
        shareAdapter = ShareAdapter(this, friends, Curtrip)

        // assign adapter for our RecyclerView
        rvShareFriends.adapter = shareAdapter

        // determine how items are arrange in our list
        rvShareFriends.layoutManager = LinearLayoutManager(this)

        shareAdapter.notifyDataSetChanged()

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

        backBtn.setOnClickListener{finish()}

        // Allow te soft input's enter key to send the request
        etUsername.setOnEditorActionListener { v, actionId, event ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    ShareByUsername(etUsername.text.toString(),tripID,3)
                    true
                }
                else -> false
            }
        }
    }

    private fun ShareByUsername(Username: String, tripID: Int?, permlevel: Int) {

        // Base firebase variables
        val firebaseUser = firebaseAuth.currentUser
        val uid = firebaseUser!!.uid

        // Navigates to the directories within the database that we will be manipulating
        val masterUserList = FirebaseDatabase.getInstance().getReference("masterUserList")
        val Users = FirebaseDatabase.getInstance().getReference("users")
        val masterTripList = FirebaseDatabase.getInstance().getReference("masterTripList")
        curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
        // Takes the given username and finds the associated UID
        val friendsID = masterUserList.child(Username)

        // If the friends username exists, add them to the current users profile, else tell the user that it does not exist
        friendsID.get().addOnSuccessListener {
            if (it.exists()) {
                val friendsIDStr = it.value.toString()
                if(!(it.child("trips").child("Trip $tripID").exists())) {

                    curUser.get().addOnSuccessListener {
                        if (it.exists()) {
                            masterUserList.get().addOnSuccessListener {
                                if (it.exists()) {
                                    val friendsUID =
                                        it.child(friendsIDStr).child("UID").value.toString()
                                    Users.child(friendsUID).child("pending trips")
                                        .child("Trip $tripID").setValue(permlevel)
                                    Users.child(friendsUID).child("trips").child("Trip $tripID")
                                        .setValue(tripID)
                                    // send notification to friend
                                    createNotification(friendsUID)
                                    Toast.makeText(this, "Trip Shared", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
            else {
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

    // function to make get the token of who we are sending the notification to
    // then fills out notification
    private fun createNotification(friendUID: String){
        FirebaseDatabase.getInstance().getReference("users").get().addOnSuccessListener {
            val friendToken = it.child(friendUID).child("userInfo").child("token").value.toString()
            val fullName = it.child(uid).child("userInfo").child("fullName").value.toString()
            val title = "Trip Invitation"
            val message = "$fullName has invited you to a trip!"
            PushNotification(
                NotificationData(title,message),
                friendToken
            ).also{
                sendNotification(it)
            }
        }
    }

    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if(response.isSuccessful) {
                Log.d(TAG, "Response: Success!")
            } else {
                Log.e(TAG, response.errorBody().toString())
            }
        } catch(e: Exception) {
            Log.e(TAG, e.toString())
        }
    }
}