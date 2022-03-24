// https://www.bing.com/videos/search?q=impelenting+google+sign+in+using+firebase+android+studio&docid=608032649073863753&mid=80FFE78A331ED38AF8EC80FFE78A331ED38AF8EC&view=detail&FORM=VIRE
package com.example.itin

import android.content.Intent
import android.content.Context
import android.content.DialogInterface
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_profile_screen.*

class GoogleLogin : AppCompatActivity() {

    // values needed for fingerprint authentication
    // source code from Briana Nzivu
    private var useFingerprint: Boolean = false
    private var cancellationSignal: CancellationSignal? = null
    private val  authenticationCallback: BiometricPrompt.AuthenticationCallback
        get() =
            @RequiresApi(Build.VERSION_CODES.P)
            object: BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    notifyUser("Authentication error: $errString")
                }
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    notifyUser("Authentication Success!")
                    startActivity(Intent(this@GoogleLogin, TripActivity::class.java))
                }
            }

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

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = GoogleLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        binding.googleSignInBtn.setOnClickListener {
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
                val biometricPrompt : BiometricPrompt = BiometricPrompt.Builder(this)
                    .setTitle("Itin Security")
                    .setDescription("Fingerprint Authentication")
                    .setNegativeButton("", this.mainExecutor, DialogInterface.OnClickListener { dialog, which ->
                    }).build()
                biometricPrompt.authenticate(getCancellationSignal(), mainExecutor, authenticationCallback)
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

    // create a toast
    private fun notifyUser(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // if the user hits cancel instead of giving fingerprint
    private fun getCancellationSignal(): CancellationSignal {
        cancellationSignal = CancellationSignal()
        cancellationSignal?.setOnCancelListener {
            finish()
            startActivity(Intent(this@GoogleLogin, MainActivity::class.java))
            //notifyUser("Authentication was cancelled by the user")
        }
        return cancellationSignal as CancellationSignal
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
                            masterUserList.child(userCount.toString()).child(username).setValue(uid)
                            masterUserList.child(username).setValue(userCount)
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

