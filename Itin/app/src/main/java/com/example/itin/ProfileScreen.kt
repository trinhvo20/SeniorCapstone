package com.example.itin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.itin.databinding.ActivityProfileScreenBinding
import com.example.itin.databinding.GoogleLoginBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class ProfileScreen : AppCompatActivity() {

    private lateinit var binding: ActivityProfileScreenBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        binding.logoutBtn.setOnClickListener {
            firebaseAuth.signOut()
            checkUser()
        }

        bottomNavBarSetup()
    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) { // If the user is not currently logged in
            startActivity(Intent(this, GoogleLogin::class.java))
        }
        else {
            val email = firebaseUser.email
            binding.emailTV.text = email
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
                    Intent(this, MainActivity::class.java).also {
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