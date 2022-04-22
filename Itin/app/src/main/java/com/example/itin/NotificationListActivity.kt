package com.example.itin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.adapters.NotificationListAdapter
import com.example.itin.classes.NotificationInstance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_notification_list.*
import kotlinx.android.synthetic.main.activity_settings.backBtn
import kotlinx.android.synthetic.main.activity_trip.*

private lateinit var notificationList : MutableList<NotificationInstance>
private lateinit var notifAdapter: NotificationListAdapter


class NotificationListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_list)

        // Finishes activity when back button is finished
        backBtn.setOnClickListener {
            finish()
            startActivity(Intent(this, TripActivity::class.java))
        }

        // set trip list
        notificationList = mutableListOf()

        // initiate a new object of class TripAdapter, pass in trips list as parameter
        notifAdapter = NotificationListAdapter(notificationList)

        // assign adapter for our RecyclerView
        rvNotifList.adapter = notifAdapter

        // determine how items are arrange in our list
        rvNotifList.layoutManager = LinearLayoutManager(this)

        // This following codes handle Pull-to-Refresh
        notifSwipeContainer.setOnRefreshListener {
            notifAdapter.clear()
            readNotifs()
            notifSwipeContainer.isRefreshing = false
        }
        // Configure the refreshing colors
        notifSwipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light);

        readNotifs()

    }

    private fun readNotifs(){
        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser = firebaseAuth.currentUser
        if(firebaseUser != null){
            val uid = firebaseUser.uid
            val curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
            Log.d("ACKACK","HERE3")
            curUser.get().addOnSuccessListener {
                if (it.hasChild("notifications")) {
                    Log.d("ACKACK","HERE2")
                    for (notif in it.child("notifications").children){
                        val title = it.child("notifications").child(notif.key.toString()).child("title").value.toString()
                        val message = it.child("notifications").child(notif.key.toString()).child("message").value.toString()
                        val time = it.child("notifications").child(notif.key.toString()).child("time").value.toString()
                        notificationList.add(NotificationInstance(title,message,time))
                        Log.d("ACKACK","HERE")
                    }
                }
                else{
                    // put up "you have no notifications
                }
                notifAdapter.notifyDataSetChanged()
            }
        }
    }
}