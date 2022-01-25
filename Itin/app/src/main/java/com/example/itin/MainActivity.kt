package com.example.itin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.create_trip.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var topAnim: Animation
    lateinit var bottomAnim: Animation
    private var splashScreen = 5000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_main)

        // Load animation for main page
        topAnim = AnimationUtils.loadAnimation(this, R.anim.top_animation)
        bottomAnim = AnimationUtils.loadAnimation(this, R.anim.bottom_animation)


        // assign animation
        LogoIV.animation = topAnim
        ItinTV.animation = bottomAnim
        IntroTV.animation = bottomAnim

        // show the Main Screen and after 5s, change to GoogleLogin page
        // if already login, go to TripActivity page (handle in GoogleLogin.kt)
        Handler(Looper.getMainLooper()).postDelayed({
            Intent(this@MainActivity, GoogleLogin::class.java).also{
                startActivity((it))
            }
            finish()
        }, splashScreen.toLong())
    }
}