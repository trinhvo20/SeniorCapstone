package com.example.itin


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView

class Profile : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // basic text to say what page you are on
        val title = findViewById<View>(R.id.activityTitleProfile) as TextView
        title.text = "Profile!"

        // set up the bottom navigation bar
        bottomNavBarSetup()

        }

    private fun bottomNavBarSetup() {
        // set up the bottom navigation bar
        var bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavView_Bar)

        // make the icons light up when you are on the page
        var menu = bottomNavigationView.menu
        var menuItem = menu.getItem(1)
        menuItem.setChecked(true)

        // move between activities
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                // go to trips page (main Activity)
                R.id.ic_trips -> {
                    Intent(this, MainActivity::class.java).also {
                        startActivity(it)
                    }
                }
                R.id.ic_profile -> {

                }
                // go to settings activity
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