package com.example.itin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.adapters.FriendAdapter
import com.example.itin.classes.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_friend.*


class FriendActivity : AppCompatActivity() {

    private lateinit var friends: MutableList<User>
    private lateinit var friendAdater: FriendAdapter

    // Some global variables that are accessed throughout the activity
    private var numFriends: Int = 0
    private var userCount: Int = 0
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var masterUserList: DatabaseReference
    private lateinit var curUser: DatabaseReference
    private lateinit var curUserFriends: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend)

        friends = mutableListOf()
        friendAdater = FriendAdapter(friends)
        rvFriends.adapter = friendAdater
        rvFriends.layoutManager = LinearLayoutManager(this)

        firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser = firebaseAuth.currentUser
        val uid = firebaseUser!!.uid
        curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
        masterUserList = FirebaseDatabase.getInstance().getReference("masterUserList")

        // Functions that are called when the buttons are clicked (finish just closes the page)
        backBtn.setOnClickListener { finish() }
        btAddFriend.setOnClickListener { addFriend() }

        // Overwrites the initial value of 0 for numFriends if user has any friends
        curUser.get().addOnSuccessListener {
            if (it.exists()) {
                // Try to grab the value from the DB for tripCount, if it doesn't exist, create the child
                try {
                    numFriends =
                        it.child("friendsList").child("numFriends").value.toString().toInt()
                    Log.d("FriendActivity", "numFriends: $numFriends")
                    // check if user is there, then read current friends from the DB
                    checkUser(numFriends)
                } catch (e: NumberFormatException) {
                    curUser.child("friendsList").child("numFriends").setValue(0)
                }
            } else {
                Log.d("FriendActivity", "The user that is logged in doesn't exist?")
            }
        }
        // Overwrites the initial value of 0 for numFriends if user has any friends
        masterUserList.get().addOnSuccessListener {
            if (it.exists()) {
                // Try to grab the value from the DB for tripCount, if it doesn't exist, create the child
                try {
                    userCount = it.child("userCount").value.toString().toInt()
                    Log.d("FriendActivity", "numFriends: $userCount")
                } catch (e: NumberFormatException) {
                    masterUserList.child("userCount").setValue(0)
                }
            } else {
                Log.d("FriendActivity", "The user that is logged in doesn't exist?")
            }
        }
    }

    private fun addFriend() {
        // Base firebase variables
        val userName = friendsUsername.text.toString()
        val firebaseUser = firebaseAuth.currentUser
        val uid = firebaseUser!!.uid

        // Navigates to the directories within the database that we will be manipulating
        masterUserList = FirebaseDatabase.getInstance().getReference("masterUserList")
        curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
        // Takes the given username and finds the associated UID
        val friendsID = masterUserList.child(userName)

        // If the friends username exists, add them to the current users profile, else tell the user that it does not exist
        friendsID.get().addOnSuccessListener {
            if (it.exists()) {
                // In the users friend lists it is stored as username:UID
                curUser.child("friendsList").child("Friend $numFriends")
                    .setValue(it.value.toString())

                numFriends += 1
                Log.d("TripActivity", "tripCount updated: $numFriends")
                curUser.child("friendsList").child("numFriends").setValue(numFriends)
            } else {
                Toast.makeText(this, "User does not exist", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkUser(numFriends: Int) {
        val firebaseUser = firebaseAuth.currentUser

        // If the use is not current logged in, log them in
        if (firebaseUser == null) {
            startActivity(Intent(this, GoogleLogin::class.java))
        } else {
            // If they are, pull the current data
            readData(numFriends)
        }
    }

    private fun readData(numFriends: Int) {
        val firebaseUser = firebaseAuth.currentUser
        val uid = firebaseUser!!.uid
        var curFriend = FirebaseDatabase.getInstance().getReference("users").child(uid).child("friendsList")

        for (i in 0 until numFriends) {

        }
    }
}


/*//       /*
After you get the user object:
    friends.add(user)
    friendAdapter.notifyDataSetChanged()

    or on removed... friends.remove(user)'
                     friendAdapter.notifyDataSetChanged()
 */
}
}