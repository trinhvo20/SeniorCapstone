package com.example.itin

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import com.example.itin.classes.User
import kotlinx.android.synthetic.main.activity_friend_info.*
import kotlinx.android.synthetic.main.activity_friend_info.backBtn
import kotlinx.android.synthetic.main.activity_profile_screen.userNameTV
import kotlinx.android.synthetic.main.activity_settings.*


class FriendInfoActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var uid : String
    private lateinit var curUser: DatabaseReference
    private lateinit var masterUserList: DatabaseReference
    private lateinit var ID: String
    private lateinit var curUserInfo: DatabaseReference
    private lateinit var storageReference: StorageReference
    private lateinit var imageUri: Uri

    private lateinit var fullName: String
    private lateinit var username: String
    private lateinit var email: String
    private lateinit var phoneNo: String

    private lateinit var friend : User

    private val PICK_IMAGE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_info)

        // get the trip object from MainActivity
        friend = intent.getSerializableExtra("EXTRA_FRIEND") as User

        userNameTV.text = friend.username
        fullNameTV.text = friend.fullName
        emailTV.text = friend.email
        phoneNumberTV.text = friend.phone

        uid = friend.uid
        getUserProfile()

        // Finishes activity when back button is finished
        backBtn.setOnClickListener {
            finish()
        }
    }
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~



    private fun getUserProfile() {
        val storageReference = FirebaseStorage.getInstance().getReference("Users/$uid.jpg")
        val localFile = File.createTempFile("tempImage","jpg")
        localFile.deleteOnExit()
        storageReference.getFile(localFile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
            friendImageIV.setImageBitmap(bitmap)
        }.addOnFailureListener {
            Log.d("ProfilePicture","Failed to retrieve image")
        }
    }
}
