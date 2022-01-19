package com.example.itin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.itin.databinding.ActivityProfileScreenBinding
import com.example.itin.databinding.GoogleLoginBinding
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
}