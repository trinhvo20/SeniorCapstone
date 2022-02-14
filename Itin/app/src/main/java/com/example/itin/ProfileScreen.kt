package com.example.itin

import android.content.Intent
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.itin.databinding.ActivityProfileScreenBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_profile_screen.*

class ProfileScreen : AppCompatActivity() {

    private lateinit var binding: ActivityProfileScreenBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var uid : String

    // for realtime database
    private lateinit var curUser: DatabaseReference

    private lateinit var fullName: String
    private lateinit var username: String
    private lateinit var email: String
    private lateinit var phoneNo: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        // Logout button
        logoutBtn.setOnClickListener {
            firebaseAuth.signOut()
            checkUser()
        }

        // update button
        updateButton.setOnClickListener { update() }

        previousTripBtn.setOnClickListener {
            val intent = Intent(this, PreviousTripActivity::class.java)
            startActivity(intent)
        }

        friendBtn.setOnClickListener {
            val intent = Intent(this, FriendActivity::class.java)
            startActivity(intent)
        }

        // bottom Navigation Bar
        bottomNavBarSetup()
    }
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
        // If the user is not currently logged in:
        if (firebaseUser == null) {
            startActivity(Intent(this, GoogleLogin::class.java))
        }
        else {
            uid = firebaseUser.uid
            Log.d("print", "firebaseAuthWithGoogleAccount: Uid: $uid")

            // for realtime database, find the current user by its uid
            readData(uid)
        }
    }

    // function to read data from Realtime Database
    private fun readData(uid: String) {
        curUser = FirebaseDatabase.getInstance().getReference("users").child(uid).child("userInfo")
        curUser.get().addOnSuccessListener {
            if (it.exists()){
                fullName = it.child("fullName").value.toString()
                username = it.child("username").value.toString()
                email = it.child("email").value.toString()
                phoneNo = it.child("phone").value.toString()

                // Show user info
                emailTV.text = email
                userNameTV.text = username
                fullNameTV.text = fullName

                fullNameInput.editText?.setText(fullName)
                usernameInput.editText?.setText(username)
                phoneNumberInput.editText?.setText(phoneNo)

            } else {
                Log.d("print", "User does not exist")
            }
        }.addOnCanceledListener {
            Log.d("print", "Failed to fetch the user")
        }
    }

    // function to update user info
    private fun update() {

        if (isNameChanged() || isUsernameChanged() || isPhoneNoChanged()) {
            Toast.makeText(this,"Updated", Toast.LENGTH_SHORT).show()
        } else {

            Toast.makeText(this,"Update at least one field", Toast.LENGTH_SHORT).show()
        }

        readData(uid)
    }

    private fun isNameChanged(): Boolean {
        val newName = fullNameInput.editText?.text.toString()
        if (newName == fullName) {
            return false
        }
        else {
            curUser.child("fullName").setValue(newName)
            fullNameInput.error = null
            fullNameInput.isErrorEnabled = false
            return true
        }
    }

    private fun isUsernameChanged(): Boolean {
        val newUsername = usernameInput.editText?.text.toString()
        val noWhiteSpace = Regex("^(.*\\s+.*)+\$")
        if (newUsername == username) {
            return false
        }
        else if (newUsername.length < 6) {
            usernameInput.error = "Minimum 6 characters"
            return false
        }
        else if (newUsername.matches(noWhiteSpace)) {
            usernameInput.error = "Cannot contain whitespaces"
            return false
        }
        else {
            curUser.child("username").setValue(newUsername)
            usernameInput.error = null
            usernameInput.isErrorEnabled = false
            return true
        }
    }

    private fun isPhoneNoChanged(): Boolean {
        val newPhoneNo = phoneNumberInput.editText?.text.toString()
        if (newPhoneNo == phoneNo) {
            return false
        }
        else if (newPhoneNo.length != 11) {
            phoneNumberInput.error = "Must contain 11 digits"
            return false
        }
        else {
            val formattedPhoneNo = PhoneNumberUtils.formatNumber(newPhoneNo, "US")
            curUser.child("phone").setValue(formattedPhoneNo)
            phoneNumberInput.error = null
            phoneNumberInput.isErrorEnabled = false
            return true
        }
    }

    // function to set up the bottom navigation bar
    private fun bottomNavBarSetup(){
        // create the bottom navigation bar
        var bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavView_Bar)

        // light up the icon you are on
        var menu = bottomNavigationView.menu
        var menuItem = menu.getItem(1)
        menuItem.setChecked(true)

        // actually switch between activities
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.ic_trips -> {
                    Intent(this, TripActivity::class.java).also {
                        startActivity(it)
                    }
                }
                R.id.ic_profile -> {

                }
                R.id.ic_settings -> {
                    Intent(this, Settings::class.java).also {
                        startActivity(it)
                    }
                }
            }
            true
            }
        }
}
