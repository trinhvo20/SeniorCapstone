package com.example.itin.adapters

import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.example.itin.classes.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_friend.*
import kotlinx.android.synthetic.main.friend_item.view.*
import kotlinx.android.synthetic.main.trip_item.view.*
import java.io.File

class FriendAdapter (
    private val friends: MutableList<Pair<User, Boolean>>
): RecyclerView.Adapter<FriendAdapter.FriendViewHolder>() {

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.friend_item, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendAdapter.FriendViewHolder, position: Int) {
        lateinit var curUser: DatabaseReference
        lateinit var firebaseAuth: FirebaseAuth
        lateinit var masterUserList: DatabaseReference

        val curFriend = friends[position].first
        val friend = friends[position].second

        holder.itemView.apply {

            var storageReference = FirebaseStorage.getInstance().getReference("Users/${curFriend.uid}.jpg")
            val localFileV2 =
                File.createTempFile("tempImage_${curFriend.uid}", "jpg")
            storageReference.getFile(localFileV2).addOnSuccessListener {
                val bitmap = BitmapFactory.decodeFile(localFileV2.absolutePath)
                friendsPP.setImageBitmap(bitmap)
            }.addOnFailureListener {
                friendsPP.setImageResource(R.drawable.profile)
            }
        }

        if (friend == true) {
            holder.itemView.apply {
                pendingReq.visibility = View.INVISIBLE
                pendingReq.height = 0
                acceptButton.visibility = View.INVISIBLE
                friendFullName.text = curFriend.username
            }
        } else {
            holder.itemView.apply {
                pendingReq.visibility = View.VISIBLE
                acceptButton.visibility = View.VISIBLE
                friendFullName.text = curFriend.username
            }
        }
        holder.itemView.apply {
            acceptButton.setOnClickListener {
                firebaseAuth = FirebaseAuth.getInstance()
                val firebaseUser = firebaseAuth.currentUser
                val uid = firebaseUser!!.uid
                masterUserList = FirebaseDatabase.getInstance().getReference("masterUserList")
                curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
                //val userName = friendsUsername.text.toString()
                val friendsID = masterUserList.child(curFriend.username) //masterUserList.child(userName)
                var myID = ""
                var myUsername = ""

                friendsID.get().addOnSuccessListener {
                    if (it.exists()) {
                        val friendsIDStr = it.value.toString()

                        curUser.get().addOnSuccessListener {
                            if (it.exists()) {
                                myUsername = it.child("userInfo").child("username").value.toString()

                                masterUserList.get().addOnSuccessListener {
                                    if (it.exists()) {
                                        myID = it.child(myUsername).value.toString()
                                        val friendsUID = it.child(friendsIDStr).child("UID").value.toString()

                                        curUser.child("friendsList").child("Friend $friendsIDStr").setValue(friendsIDStr)
                                        FirebaseDatabase.getInstance().getReference("users").child(friendsUID).child("friendsList").child("Friend $myID").setValue(myID)
                                        curUser.child("reqList").child("Request $friendsIDStr").removeValue()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return friends.size
    }

    // Clean all elements of the recycler
    fun clear() {
        friends.clear()
        notifyDataSetChanged()
    }

}