// https://www.bing.com/videos/search?q=impelenting+google+sign+in+using+firebase+android+studio&docid=608032649073863753&mid=80FFE78A331ED38AF8EC80FFE78A331ED38AF8EC&view=detail&FORM=VIRE
package com.example.itin

import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.itin.classes.NotificationInstance
import com.example.itin.classes.User
import com.example.itin.databinding.GoogleLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class GoogleLogin : AppCompatActivity() {

    // values to see if phone has fingerprint authentication allowed
    private var useFingerprint: Boolean = false

    // View binding
    private lateinit var binding: GoogleLoginBinding

    // Creating variables for our authentication services through Firebase
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var masterUserList: DatabaseReference
    private var userCount: Int = 0

    // Constants
    private companion object {
        private const val RC_SIGN_IN = 100
        private const val TAG = "GOOGLE_SIGN_IN_TAG"
    }

    // for realtime database
    private lateinit var rootNode: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var topAnim: Animation
    lateinit var bottomAnim: Animation

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = GoogleLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            )
        }
        // Load animation for main page
        topAnim = AnimationUtils.loadAnimation(this, R.anim.top_animation)
        bottomAnim = AnimationUtils.loadAnimation(this, R.anim.bottom_animation)
        // assign animation
        binding.welcomeTV.animation = topAnim
        binding.LogoIV.animation = topAnim
        binding.welcome2TV.animation = bottomAnim
        binding.GoogleIcon.animation = bottomAnim


        // for biometric authentication
        val sp = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        useFingerprint = sp.getBoolean("fingerprint_key", false)

        // for realtime database
        rootNode = FirebaseDatabase.getInstance()
        databaseReference = rootNode.getReference("users")

        // Google SignIn Settings. This is also where we will request addition information as needed from gmail account
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        masterUserList = FirebaseDatabase.getInstance().getReference("masterUserList")

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

        // Initiate Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        // Links button to Google Sign In
        binding.GoogleIcon.setOnClickListener {
            Log.d(TAG, "onCreate: begin Google Sign In")
            val intent = googleSignInClient.signInIntent
            startActivityForResult(intent, RC_SIGN_IN)
        }


    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
        // This is the case if user is already logged in, skips login screen if that is the case
        if (firebaseUser != null) {
            if(!useFingerprint) {
                startActivity(Intent(this@GoogleLogin, TripActivity::class.java))

                finish()
            }
            else{
                startActivity(Intent(this@GoogleLogin, FingerprintActivity::class.java))
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            Log.d(TAG, "onActivityResult: Google SignIn intent result")
            val accountTask = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = accountTask.getResult(ApiException::class.java)
                firebaseAuthWithGoogleAccount(account)
            }
            catch (e: Exception) {
                Log.d(TAG, "onActivityResult: ${e.message}")
            }
        }
    }

    private fun firebaseAuthWithGoogleAccount(account: GoogleSignInAccount?) {
        Log.d(TAG, "firebaseAuthWithGoogleAccount: Logged In")

        val credential = GoogleAuthProvider.getCredential(account!!.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                val firebaseUser = firebaseAuth.currentUser
                val uid = firebaseUser!!.uid
                val email = firebaseUser.email.toString()
                val fullName = firebaseUser.displayName.toString()
                val username = email?.substringBefore("@").toString()
                val phoneNumber = firebaseUser.phoneNumber.toString()

                Log.d(TAG, "firebaseAuthWithGoogleAccount: Uid: $uid")
                Log.d(TAG, "firebaseAuthWithGoogleAccount: Email: $email")
                Log.d(TAG, "firebaseAuthWithGoogleAccount: Phone: $phoneNumber")

                // Check if this is a new user or existing
                if (authResult.additionalUserInfo!!.isNewUser) {
                    Log.d(TAG, "firebaseAuthWithGoogleAccount: Account created... \n$email")
                    // create a new user account in realtime database (unique id = uid)
                    val user = User(uid, fullName, username, email, phoneNumber)
                    databaseReference.child(uid).child("userInfo").setValue(user)
                    // Add the new user to the MasterUserList upon first sign in
                    masterUserList = FirebaseDatabase.getInstance().getReference("masterUserList")
                    masterUserList.get().addOnSuccessListener {
                        if (it.exists()) {
                            masterUserList.child(userCount.toString()).child("UID").setValue(uid)
                            masterUserList.child(username).setValue(userCount.toString())
                            userCount += 1
                            Log.d("TripActivity", "tripCount updated: $userCount")
                            masterUserList.child("userCount").setValue(userCount)

                        } else {
                            Log.d("TripActivity", "There is no masterUserList")
                        }
                    }
                }
                else {
                    Log.d(TAG, "firebaseAuthWithGoogleAccount: Existing user... \n$email")
                    Toast.makeText( this@GoogleLogin, "Logged In... \n@email", Toast.LENGTH_LONG).show()
                }

                // Opening the Trip page after log in
                startActivity(Intent(this@GoogleLogin, TripActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "firebaseAuthWithGoogleAccount: Loggin Failed due to ${e.message}")
                Toast.makeText(this@GoogleLogin, "Login failed due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


}

