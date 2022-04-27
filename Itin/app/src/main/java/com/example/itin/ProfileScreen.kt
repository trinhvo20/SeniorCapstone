package com.example.itin

import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.telephony.PhoneNumberUtils
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.itin.databinding.ActivityProfileScreenBinding
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig
import com.google.android.gms.auth.api.credentials.HintRequest
import com.google.android.gms.common.api.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.account.*
import kotlinx.android.synthetic.main.activity_profile_screen.*
import kotlinx.android.synthetic.main.activity_share_trip.view.*
import java.io.File


class ProfileScreen : AppCompatActivity() {

    private lateinit var binding: ActivityProfileScreenBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var uid: String
    private lateinit var curUser: DatabaseReference
    private lateinit var masterUserList: DatabaseReference
    private lateinit var ID: String
    private var phoneNoRead: Boolean = false
    private lateinit var curUserInfo: DatabaseReference
    private lateinit var storageReference: StorageReference
    private lateinit var imageUri: Uri
    private lateinit var mGoogleApiClient: GoogleApiClient
    private lateinit var phoneNumberInput: TextView

    private lateinit var fullName: String
    private lateinit var username: String
    private lateinit var email: String
    private lateinit var phoneNo: String

    private val RC_HINT = 1000
    private val PICK_IMAGE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addApi(Auth.CREDENTIALS_API)
            .build()

        // Logout button
        logoutBtn.setOnClickListener {
            firebaseAuth.signOut()
            checkUser()
        }

        previousTripBtn.setOnClickListener {
            val intent = Intent(this, PreviousTripActivity::class.java)
            startActivity(intent)
        }

        settingsBtn.setOnClickListener {
            val intent = Intent(this, Settings::class.java)
            startActivity(intent)
        }

        editProfileIV.setOnClickListener { openGallery() }

        accountBtn.setOnClickListener { updateUserInfo() }

        usBtn.setOnClickListener {
            val intent = Intent(this, AboutUsActivity::class.java)
            startActivity(intent)
        }

        // bottom Navigation Bar
        bottomNavBarSetup()
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private fun showHint() {
        val hintRequest = HintRequest.Builder()
            .setHintPickerConfig(
                CredentialPickerConfig.Builder()
                    .setShowCancelButton(true)
                    .build()
            )
            .setPhoneNumberIdentifierSupported(true)
            .build()

        val intent = Auth.CredentialsApi.getHintPickerIntent(mGoogleApiClient, hintRequest)
        try {
            startIntentSenderForResult(intent.intentSender, RC_HINT, null, 0, 0, 0)
        } catch (e: SendIntentException) {
            Log.e("Phone Number Debugging", "Could not start hint picker Intent", e)
        }
    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
        // If the user is not currently logged in:
        if (firebaseUser == null) {
            startActivity(Intent(this, GoogleLogin::class.java))
        }
        else {
            uid = firebaseUser.uid
            Log.d("print", "firebaseAuthWithGoogleAccount: Uid: $uid")

            // for realtime database, find the current user by its uid
            readData(uid)
        }
    }

//    private fun updateUserInfo() {
//        showHint()
//    }

    private fun updateUserInfo() {

        val view = LayoutInflater.from(this).inflate(R.layout.account, null)

        val usernameInput = view.findViewById<TextInputLayout>(R.id.usernameInput)
        val fullNameInput = view.findViewById<TextInputLayout>(R.id.fullNameInput)
        phoneNumberInput = view.findViewById<TextView>(R.id.phoneNumberInput)

        val newDialog = AlertDialog.Builder(this,R.style.popup_Theme)
        newDialog.setView(view)

        phoneNumberInput.setOnClickListener{
            showHint()
        }

        Log.d("Phone Number Debugging", "$phoneNo")

        FirebaseDatabase.getInstance().getReference("users").child(uid).child("userInfo")
            .get().addOnSuccessListener {
                if (it.exists()){
                    fullName = it.child("fullName").value.toString()
                    username = it.child("username").value.toString()
                    phoneNo =  it.child("phone").value.toString()

                    fullNameInput.editText?.setText(fullName)
                    usernameInput.editText?.setText(username)
                    phoneNumberInput.text = phoneNo
                } else {
                    Log.d("print", "User does not exist")
                }
            }.addOnCanceledListener {
                Log.d("print", "Failed to fetch the user")
            }

        newDialog.setPositiveButton("Update") { dialog, _ ->
            update(usernameInput, fullNameInput, phoneNumberInput)
            Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        newDialog.setNegativeButton("Cancel") { dialog, _ ->
            Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        newDialog.setOnCancelListener {
            Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show()
        }

        newDialog.create()
        newDialog.show()
    }

    // function to read data from Realtime Database
    private fun readData(uid: String) {
        curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
        curUserInfo = curUser.child("userInfo")
        curUserInfo.get().addOnSuccessListener {
            if (it.exists()){
                fullName = it.child("fullName").value.toString()
                username = it.child("username").value.toString()
                email = it.child("email").value.toString()
                phoneNo = it.child("phone").value.toString()

                // Show user info
                userNameTV.text = username

                getUserProfile()
            } else {
                Log.d("print", "User does not exist")
            }
        }.addOnCanceledListener {
            Log.d("print", "Failed to fetch the user")
        }
    }

    // function to update user info
    private fun update(usernameInput: TextInputLayout, fullNameInput: TextInputLayout, phoneNumberInput: TextView) {
        val newUsername = usernameInput.editText?.text.toString()
        val usernameQuery = FirebaseDatabase.getInstance().reference.child("users")
        usernameQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            var isExist = false
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot : DataSnapshot in snapshot.children) {
                    if (userSnapshot.key != uid) {
                        val existingUsername =
                            userSnapshot.child("userInfo").child("username").value.toString()
                        if (existingUsername == newUsername) {
                            isExist = true
                            break
                        }
                    } else {
                        continue
                    }
                }
                Log.d("INSIDE",isExist.toString())

                if (isNameChanged(fullNameInput) || isUsernameChanged(isExist, usernameInput) || isPhoneNoChanged(phoneNumberInput)) {
                    Toast.makeText(this@ProfileScreen,"Updated", Toast.LENGTH_SHORT).show()
                } else{
                    Toast.makeText(this@ProfileScreen,"Update at least one field", Toast.LENGTH_SHORT).show()
                }
                    readData(uid)
            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun isNameChanged(fullNameInput: TextInputLayout): Boolean {
        val newName = fullNameInput.editText?.text.toString()
        if (newName == fullName) {
            return false
        }
        else {
            curUserInfo.child("fullName").setValue(newName)
            fullNameInput.error = null
            fullNameInput.isErrorEnabled = false
            return true
        }
    }

    private fun isUsernameChanged(isExist:Boolean, usernameInput: TextInputLayout): Boolean {
        val newUsername = usernameInput.editText?.text.toString()
        val noWhiteSpace = Regex("^(.*\\s+.*)+\$")

        if (isExist) {
            usernameInput.error = "Username exists"
            return false
        }
        else if (newUsername == username) {
            return false
        }
        else if (newUsername.length < 4 || newUsername.length > 14) {
            usernameInput.error = "Username must be between 4 to 14 characters"
            return false
        }
        else if (newUsername.matches(noWhiteSpace)) {
            usernameInput.error = "Cannot contain whitespaces"
            return false
        }
        else {
            curUserInfo.child("username").setValue(newUsername)
            usernameInput.error = null
            usernameInput.isErrorEnabled = false

            // This will update the masterUserList with the new username
            masterUserList = FirebaseDatabase.getInstance().getReference("masterUserList")
            masterUserList.get().addOnSuccessListener {
                if (it.exists()) {
                    ID = it.child("$username").value.toString()
                    masterUserList.child(username).removeValue()
                    masterUserList.child(ID).child("UID").removeValue()
                    masterUserList.child(newUsername).setValue("$ID")
                    masterUserList.child(ID).child("UID").setValue(uid)
                } else {
                    Log.d("TripActivity", "There is no masterUserList")
                }
            }
            return true
        }
    }

    private fun isPhoneNoChanged(phoneNumberInput: TextView): Boolean {
        var newPhoneNo = phoneNumberInput.text.toString()
        Log.d("Phone Number Debugging", "$newPhoneNo")
        if (newPhoneNo == curUserInfo.child("phone").get().toString()) {
            return false
        }
//        else if (newPhoneNo.length != 11) {
//            phoneNumberInput.error = "Must contain 11 digits"
//            return false
//        }
        else {
//            newPhoneNo = newPhoneNo.slice((2..12))
//            val formattedPhoneNo = PhoneNumberUtils.formatNumber(newPhoneNo, "US")
            curUserInfo.child("phone").setValue(newPhoneNo)
            return true
        }
    }

    private fun openGallery() {
        val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(gallery, PICK_IMAGE)
    }

    // handle the profile_picture change
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_HINT) {
            if (resultCode == RESULT_OK) {
                val cred: Credential? = data?.getParcelableExtra(Credential.EXTRA_KEY)
                if (cred != null) {
                    phoneNo = cred.id
                    phoneNumberInput.text = phoneNo
                    //getValues()
                }
            } else {
                //getValues()
            }
        }
        else if (resultCode == RESULT_OK && requestCode == PICK_IMAGE){
        if (data != null) {
            imageUri = data.data!!
            Log.d("Profile",imageUri.toString())
            profileImageIV.setImageURI(imageUri)
            uploadProfilePic()
        }
    }
}

    private fun uploadProfilePic() {
        //imageUri = Uri.parse("android.resource://$packageName/${R.drawable.profile}")
        storageReference = FirebaseStorage.getInstance().getReference("Users/$uid.jpg")
        storageReference.putFile(imageUri).addOnSuccessListener {
            Toast.makeText(this@ProfileScreen,"Profile successfully updated", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this@ProfileScreen,"Failed to upload image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUserProfile() {
        storageReference = FirebaseStorage.getInstance().getReference("Users/$uid.jpg")
        val localFile = File.createTempFile("tempImage","jpg")
        localFile.deleteOnExit()
        storageReference.getFile(localFile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
            profileImageIV.setImageBitmap(bitmap)
        }.addOnFailureListener {
            Log.d("ProfilePicture","Failed to retrieve image")
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
                R.id.ic_friends -> {
                    Intent(this, FriendActivity::class.java).also {
                        startActivity(it)
                    }
                }
            }
            true
            }
        }
}

private fun Any.addConnectionCallbacks(profileScreen: ProfileScreen) { }
private fun Any.enableAutoManage(profileScreen: ProfileScreen, profileScreen1: ProfileScreen) { }

