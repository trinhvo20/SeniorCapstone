package com.example.itin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.itin.classes.User
import com.example.itin.databinding.ActivityProfileScreenBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_profile_screen.*

class ProfileScreen : AppCompatActivity() {

    private lateinit var binding: ActivityProfileScreenBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var user : User

    // for realtime database
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        // what happen when click the Logout button
        logoutBtn.setOnClickListener {
            firebaseAuth.signOut()
            checkUser()
        }

        // update button
        updateButton.setOnClickListener { update() }

        bottomNavBarSetup()
    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
        // If the user is not currently logged in:
        if (firebaseUser == null) {
            startActivity(Intent(this, GoogleLogin::class.java))
        }
        else {
            val uid = firebaseUser.uid
            Log.d("print", "firebaseAuthWithGoogleAccount: Uid: $uid")

            // for realtime database, find the current user by its uid
            readData(uid)
        }
    }

    // function to read data from Realtime Database
    private fun readData(uid: String) {
        databaseReference = FirebaseDatabase.getInstance().getReference("users")
        val checkUser = databaseReference.child(uid)

        checkUser.get().addOnSuccessListener {
            if(it.exists()){
                val fullName = it.child("fullName").value.toString()
                val username = it.child("username").value.toString()
                val email = it.child("email").value.toString()
                val phone = it.child("phone").value.toString()

                user = User(fullName,username,email,phone)

                emailTV.text = email
                userNameTV.text = username
                fullNameTV.text = fullName

            } else {
                Log.d("print", "User does not exist")
            }
        }.addOnCanceledListener {
            Log.d("print", "Failed to fetch the user")
        }
    }

    // function to update user info
    private fun update() {
        var check = 1
        val newName = fullNameInput.editText?.text.toString()
        val newUsername = usernameInput.editText?.text.toString()
        val newPhone = phoneNumberInput.editText?.text.toString()

        val uid = firebaseAuth.currentUser?.uid.toString() // get uid from Google
        val curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)

        if (newName.isNotEmpty()) {
            curUser.child("fullName").setValue(newName)
            check = 0
        }
        if (newUsername.isNotEmpty()) {
            curUser.child("username").setValue(newUsername)
            check = 0
        }
        if (newPhone.isNotEmpty()) {
            curUser.child("phone").setValue(newPhone)
            check = 0
        }
        if (check == 1) {
            Toast.makeText(this,"Fill in at least one field", Toast.LENGTH_SHORT).show()
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