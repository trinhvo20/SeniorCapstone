package com.example.itin

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.itin.databinding.ActivityProfileScreenBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_profile_screen.*
import java.io.File


class ProfileScreen : AppCompatActivity() {

    private lateinit var binding: ActivityProfileScreenBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var uid : String
    private lateinit var curUser: DatabaseReference
    private lateinit var curUserInfo: DatabaseReference
    private lateinit var storageReference: StorageReference
    private lateinit var imageUri: Uri

    private lateinit var fullName: String
    private lateinit var username: String
    private lateinit var email: String
    private lateinit var phoneNo: String

    private val PICK_IMAGE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        // Logout button
        logoutBtn.setOnClickListener {
            firebaseAuth.signOut()
            checkUser()
        }

        // update button
        updateButton.setOnClickListener { update() }

        previousTripBtn.setOnClickListener {
            val intent = Intent(this, PreviousTripActivity::class.java)
            startActivity(intent)
        }

        friendBtn.setOnClickListener {
            val intent = Intent(this, FriendActivity::class.java)
            startActivity(intent)
        }

        editProfileIV.setOnClickListener { openGallery() }

        // bottom Navigation Bar
        bottomNavBarSetup()
    }
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
                emailTV.text = email
                userNameTV.text = username
                fullNameTV.text = fullName

                fullNameInput.editText?.setText(fullName)
                usernameInput.editText?.setText(username)
                phoneNumberInput.editText?.setText(phoneNo)

                getUserProfile()
            } else {
                Log.d("print", "User does not exist")
            }
        }.addOnCanceledListener {
            Log.d("print", "Failed to fetch the user")
        }
    }

    // function to update user info
    private fun update() {
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

                if (isNameChanged() || isUsernameChanged(isExist) || isPhoneNoChanged()) {
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

    private fun isNameChanged(): Boolean {
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

    private fun isUsernameChanged(isExist:Boolean): Boolean {
        val newUsername = usernameInput.editText?.text.toString()
        val noWhiteSpace = Regex("^(.*\\s+.*)+\$")

        if (isExist) {
            usernameInput.error = "Username exists"
            return false
        }
        else if (newUsername == username) {
            return false
        }
        else if (newUsername.length < 6) {
            usernameInput.error = "Minimum 6 characters"
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
            return true
        }
    }

    private fun isPhoneNoChanged(): Boolean {
        val newPhoneNo = phoneNumberInput.editText?.text.toString()
        if (newPhoneNo == phoneNo) {
            return false
        }
        else if (newPhoneNo.length != 11) {
            phoneNumberInput.error = "Must contain 11 digits"
            return false
        }
        else {
            val formattedPhoneNo = PhoneNumberUtils.formatNumber(newPhoneNo, "US")
            curUserInfo.child("phone").setValue(formattedPhoneNo)
            phoneNumberInput.error = null
            phoneNumberInput.isErrorEnabled = false
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
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE){
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

