package com.example.itin


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView

class Settings : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val title = findViewById<View>(R.id.activityTitleSettings) as TextView
        title.text = "Settings!"

        var bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavView_Bar)
        var menu = bottomNavigationView.menu
        var menuItem = menu.getItem(2)
        menuItem.setChecked(true)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.ic_trips -> {
                    Intent(this, MainActivity::class.java).also {
                        startActivity(it)
                    }
                }
                R.id.ic_profile -> {
                    Intent(this, Profile::class.java).also {
                        startActivity(it)
                    }
                }
                R.id.ic_settings -> {

                }
            }
            true
        }
    }

}