package com.example.itin.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.example.itin.classes.Trip
import com.example.itin.classes.User
import com.example.itin.notifications.NotificationData
import com.example.itin.notifications.PushNotification
import com.example.itin.notifications.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.friend_share_item.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

// Variable for error messages
private val TAG = "ShareAdapter"

class ShareAdapter(
    private val context: Context,
    private val Friends: MutableList<User>,
    private val Curtrip: Trip,

    ) : RecyclerView.Adapter<ShareAdapter.ShareViewHolder>() {

    val tripID = Curtrip.tripID
    val Viewers = Curtrip.viewers
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var uid : String
    private lateinit var curUser: DatabaseReference

    inner class ShareViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ibShareFriend: ImageView = itemView.findViewById(R.id.ibShareFriend)
        init {
            ibShareFriend.setOnClickListener {
                ShareByFriend(3)
                ibShareFriend.isClickable = false
                ibShareFriend.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
            }
        }

        private fun ShareByFriend(permlevel: Int) {
            val curFriend = Friends[adapterPosition].username

            // Base firebase variables
            firebaseAuth = FirebaseAuth.getInstance()
            val firebaseUser = firebaseAuth.currentUser
            uid = firebaseUser!!.uid

            // Navigates to the directories within the database that we will be manipulating
            val masterUserList = FirebaseDatabase.getInstance().getReference("masterUserList")
            val Users = FirebaseDatabase.getInstance().getReference("users")
            val masterTripList = FirebaseDatabase.getInstance().getReference("masterTripList")
            curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
            // Takes the given username and finds the associated UID
            val friendsID = masterUserList.child(curFriend)

            // If the friends username exists, add them to the current users profile, else tell the user that it does not exist
            friendsID.get().addOnSuccessListener {
                if (it.exists()) {
                    val friendsIDStr = it.value.toString()

                    if(!(it.child("trips").child("Trip $tripID").exists())) {
                        curUser.get().addOnSuccessListener {
                            if (it.exists()) {
                                masterUserList.get().addOnSuccessListener {
                                    if (it.exists()) {
                                        val friendsUID =
                                            it.child(friendsIDStr).child("UID").value.toString()
                                        Users.child(friendsUID).child("pending trips")
                                            .child("Trip $tripID").setValue(permlevel)
                                        Users.child(friendsUID).child("trips").child("Trip $tripID")
                                            .setValue(tripID)
                                        // send notification to friend
                                        createNotification(friendsUID)
                                        Toast.makeText(context, "Trip Shared", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                }
                            }
                        }
                    }
                }
                else {
                    Toast.makeText(context, "User does not exist", Toast.LENGTH_SHORT).show()
                }
            }
        }


        // function to make get the token of who we are sending the notification to
        // then fills out notification
        private fun createNotification(friendUID: String){
            FirebaseDatabase.getInstance().getReference("users").get().addOnSuccessListener {
                val friendToken = it.child(friendUID).child("userInfo").child("token").value.toString()
                val fullName = it.child(uid).child("userInfo").child("fullName").value.toString()
                val title = "Trip Invitation"
                val message = "$fullName has invited you to a trip!"
                PushNotification(
                    NotificationData(title,message),
                    friendToken
                ).also{
                    sendNotification(it)
                }
            }
        }

        private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.postNotification(notification)
                if(response.isSuccessful) {
                    Log.d(TAG, "Response: Success!")
                } else {
                    Log.e(TAG, response.errorBody().toString())
                }
            } catch(e: Exception) {
                Log.e(TAG, e.toString())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShareViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.friend_share_item, parent, false)
        return ShareViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShareViewHolder, position: Int) {
        lateinit var curUser: DatabaseReference
        lateinit var firebaseAuth: FirebaseAuth
        lateinit var masterUserList: DatabaseReference

        val curFriend = Friends[position]

        holder.itemView.apply {

            if(Viewers.containsKey(curFriend.uid)){
                ibShareFriend.isClickable = false
                ibShareFriend.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
            }

            var storageReference = FirebaseStorage.getInstance().getReference("Users/${curFriend.uid}.jpg")
            val localFile =
                File.createTempFile("tempImage_${curFriend.uid}", "jpg")
            localFile.deleteOnExit()
            storageReference.getFile(localFile).addOnSuccessListener {
                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                ivFriendPP.setImageBitmap(bitmap)
            }.addOnFailureListener {
                ivFriendPP.setImageResource(R.drawable.profile)
            }
            if(curFriend.fullName.length > 15){
                var shortname = curFriend.fullName.substring(0..11)
                tvFriendFullName.text = shortname+"..."
            }
            else{
                tvFriendFullName.text = curFriend.fullName
            }
            tvFriendName.text = curFriend.username
        }
    }

    override fun getItemCount(): Int {
        return Friends.size
    }
}