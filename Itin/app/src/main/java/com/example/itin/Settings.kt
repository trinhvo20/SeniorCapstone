package com.example.itin


import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_settings.*

class Settings : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val sp = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        // Finishes activity when back button is finished
        backBtn.setOnClickListener {
            finish()
            startActivity(Intent(this, ProfileScreen::class.java))
        }

    }

}