package com.example.itin

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.adapters.FriendAdapter
import com.example.itin.classes.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_friend.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class FriendActivity : AppCompatActivity() {

    // Variables for recycler view
    private lateinit var friends: MutableList<Pair<User, Boolean>>
    private lateinit var friendAdater: FriendAdapter

    // Variable for error messages
    val TAG = "FriendActivity"

    // Some global variables that are accessed throughout the activity
    private var userCount: Int = 0
    private var friendsList: MutableList<Int> = mutableListOf()
    private var friend:Boolean = false
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var masterUserList: DatabaseReference
    private lateinit var curUser: DatabaseReference
    private lateinit var uid: String

    // Variables for floating button animations
    private val rotateOpen: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim) }
    private val rotateClose: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim) }
    private val fromBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.from_bottom_anim) }
    private val toBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.to_bottom_anim) }
    private var clicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set up and bind recycler view
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend)

        friends = mutableListOf()
        friendAdater = FriendAdapter(friends)
        rvFriends.adapter = friendAdater
        rvFriends.layoutManager = LinearLayoutManager(this)

        // Firebase initialization information
        firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser = firebaseAuth.currentUser
        uid = firebaseUser!!.uid
        curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
        masterUserList = FirebaseDatabase.getInstance().getReference("masterUserList")

        // Setting up floating buttons
        btExpandMenu.setOnClickListener { onExpandButtonClicked() }
        btSendReq.setOnClickListener { onSendButtonClicked() }
        btSearchFriend.setOnClickListener { searchVisibility() }

        btRmFriend.setOnClickListener {

        }

        // Finishes activity when back button is finished
        backBtn.setOnClickListener { finish() }

        // Overwrites the initial value of 0 for numFriends if user has any friends
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

    private fun addFriendReq() {
        // Base firebase variables
        val userName = friendsUsername.text.toString()
        val firebaseUser = firebaseAuth.currentUser
        val uid = firebaseUser!!.uid

        // Navigates to the directories within the database that we will be manipulating
        masterUserList = FirebaseDatabase.getInstance().getReference("masterUserList")
        curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
        // Takes the given username and finds the associated UID
        val friendsID = masterUserList.child(userName)
        var myUsername = ""
        var myID = ""

        // If the friends username exists, add them to the current users profile, else tell the user that it does not exist
        friendsID.get().addOnSuccessListener {
            if (it.exists()) {
                val friendsIDStr = it.value.toString()
                val friendsIDInt = it.value.toString().toInt()

                curUser.get().addOnSuccessListener {
                    if (it.exists()) {
                        myUsername = it.child("userInfo").child("username").value.toString()

                        masterUserList.get().addOnSuccessListener {
                            if (it.exists()){
                                myID = it.child(myUsername).value.toString()
                                val friendsUID = it.child(friendsIDStr).child("UID").value.toString()

                                if (friendsList.contains(friendsIDInt)) {
                                    Toast.makeText(this, "This user is already your friend!", Toast.LENGTH_SHORT).show()
                                    friendsUsername.text?.clear()
                                }
                                else {
                                    FirebaseDatabase.getInstance().getReference("users").child(friendsUID).child("reqList").child("Request $myID").setValue(myID)
                                    // User feedback
                                    Toast.makeText(this, "Request Sent", Toast.LENGTH_SHORT).show()
                                    friendsUsername.text?.clear()
                                    // send notification
                                    createNotification(friendsUID)
                                }
                            }
                            // User does not exist
                            else {
                                Toast.makeText(this, "User does not exist", Toast.LENGTH_SHORT).show()
                                friendsUsername.text?.clear()
                            }
                        }
                    }
                }
            }
            else{
                Toast.makeText(this, "User does not exist!", Toast.LENGTH_SHORT).show()
                friendsUsername.text?.clear()
            }
        }
    }

    private fun readData(userCount: Int) {
        Log.d("FriendActivity", "Reading Data")
        val firebaseUser = firebaseAuth.currentUser
        val uid = firebaseUser!!.uid
        var curFriend = FirebaseDatabase.getInstance().getReference("users").child(uid).child("friendsList")
        var curReq = FirebaseDatabase.getInstance().getReference("users").child(uid).child("reqList")

        for (i in 0 until userCount)
            curReq.child("Request $i").get().addOnSuccessListener {
                if (it.exists()) {
                    var curID = it.value
                    Log.d("FriendActivity", "curID: $curID")
                    if (curID != null) {
                        friend = false
                        accessMasterUserList(curID, friend)
                        friendsList.add(i)
                        Log.d("FriendActivity", "CurID: $curID")
                    }
                } else {
                    Log.d("FriendActivity", "Request does not exist")
                }
            }
        for (i in 0 until userCount)
            curFriend.child("Friend $i").get().addOnSuccessListener {
                if (it.exists()) {
                    var curID = it.value
                    Log.d("FriendActivity", "curID: $curID")
                    if (curID != null) {
                        friend = true
                        accessMasterUserList(curID,friend)
                        friendsList.add(i)
                        Log.d("FriendActivity", "CurID: $curID")
                    }
                } else {
                    Log.d("FriendActivity", "Friend does not exist")
                }
            }.addOnCanceledListener {
                Log.d("FriendActivity", "Failed to fetch the Friend")
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

                        val user = User(
                            uid,
                            "null",
                            username,
                            "null",
                            "null"
                        )
                        friends.add(Pair(user, friend))
                        friendAdater.notifyDataSetChanged()
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
            val title = "Friend Request"
            val message = "$fullName would like to be your friend!"
            PushNotification(
                NotificationData(title,message),
                friendToken
            ).also{
                sendNotification(it)
            }
        }

    }
    private fun onExpandButtonClicked() {
        setVisibility(clicked)
        setAnimation(clicked)
        setClickable(clicked)
        clicked = !clicked
    }

    private fun searchVisibility() {
        btSearchFriend.visibility = View.INVISIBLE
        btRmFriend.visibility = View.INVISIBLE
        btExpandMenu.visibility = View.INVISIBLE
        btSearchFriend.isClickable = false
        btRmFriend.isClickable = false
        btExpandMenu.isClickable = false
        btSearchFriend.startAnimation(toBottom)
        btRmFriend.startAnimation(toBottom)
        btExpandMenu.startAnimation(toBottom)
        clicked = !clicked

        btSendReq.visibility = View.VISIBLE
        friendsUsername.visibility = View.VISIBLE
        btSendReq.isClickable = true
        friendsUsername.clearFocus()
    }

    private fun onSendButtonClicked() {
        addFriendReq()

        btSendReq.visibility = View.INVISIBLE
        friendsUsername.visibility = View.INVISIBLE
        btSendReq.isClickable = false

        btExpandMenu.visibility = View.VISIBLE
        btExpandMenu.isClickable = true
        btExpandMenu.startAnimation(fromBottom)

    }

    private fun setAnimation(clicked: Boolean) {
        if (!clicked) {
            btSearchFriend.startAnimation(fromBottom)
            btRmFriend.startAnimation(fromBottom)
            btExpandMenu.startAnimation(rotateOpen)
        } else {
            btSearchFriend.startAnimation(toBottom)
            btRmFriend.startAnimation(toBottom)
            btExpandMenu.startAnimation(rotateClose)
        }
    }

    private fun setVisibility(clicked: Boolean) {
        if (!clicked) {
            btSearchFriend.visibility = View.VISIBLE
            btRmFriend.visibility = View.VISIBLE
        } else {
            btSearchFriend.visibility = View.INVISIBLE
            btRmFriend.visibility = View.INVISIBLE
        }
    }
    
    private fun setClickable(clicked: Boolean) {
        if (!clicked) {
            btSearchFriend.isClickable = true
            btRmFriend.isClickable = true
        } else {
            btSearchFriend.isClickable = false
            btRmFriend.isClickable = false
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

