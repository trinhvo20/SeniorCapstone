// https://www.bing.com/videos/search?q=impelenting+google+sign+in+using+firebase+android+studio&docid=608032649073863753&mid=80FFE78A331ED38AF8EC80FFE78A331ED38AF8EC&view=detail&FORM=VIRE
package com.example.itin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.itin.classes.User
import com.example.itin.databinding.GoogleLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import java.lang.Exception
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class GoogleLogin : AppCompatActivity() {

    // View binding
    private lateinit var binding: GoogleLoginBinding

    // Creating variables for our authentication services through Firebase
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth

    // Constants
    private companion object {
        private const val RC_SIGN_IN = 100
        private const val TAG = "GOOGLE_SIGN_IN_TAG"
    }

    // for realtime database
    private lateinit var rootNode: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = GoogleLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Google SignIn Settings. This is also where we will request addition information as needed from gmail account
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        // Initiate Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        // Links button to Google Sign In
        binding.googleSignInBtn.setOnClickListener {
            Log.d(TAG, "onCreate: begin Google Sign In")
            val intent = googleSignInClient.signInIntent
            startActivityForResult(intent, RC_SIGN_IN)
        }
    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
        // This is the case if user is already logged in, skips login screen if that is the case
        if (firebaseUser != null) {
            startActivity(Intent(this@GoogleLogin, TripActivity::class.java))
            finish()
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

                // for realtime database
                rootNode = FirebaseDatabase.getInstance()
                databaseReference = rootNode.getReference("users")

                // Check if this is a new user or existing
                if (authResult.additionalUserInfo!!.isNewUser) {
                    Log.d(TAG, "firebaseAuthWithGoogleAccount: Account created... \n$email")
                    Toast.makeText(this@GoogleLogin, "Account created... \n$email", Toast.LENGTH_LONG).show()
                    // create a new user account in realtime database (unique id = uid)
                    val user = User(fullName,username,email,phoneNumber)
                    databaseReference.child(uid).setValue(user)
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

