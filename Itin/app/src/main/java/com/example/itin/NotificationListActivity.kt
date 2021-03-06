package com.example.itin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        tvNoNotifs.visibility = View.INVISIBLE

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

        // incorporate swipe to delete
        val swipeToDeleteCallback = object : SwipeToDeleteCallback(){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                deleteNotif(notificationList[position])
                notificationList.removeAt(position)
                if(notificationList.isEmpty()){
                    tvNoNotifs.visibility = View.VISIBLE
                }
                rvNotifList.adapter?.notifyItemRemoved(position)
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(rvNotifList)

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
            curUser.get().addOnSuccessListener {
                if (it.hasChild("notifications")) {
                    for (notif in it.child("notifications").children){
                        val title = it.child("notifications").child(notif.key.toString()).child("title").value.toString()
                        val message = it.child("notifications").child(notif.key.toString()).child("message").value.toString()
                        val time = it.child("notifications").child(notif.key.toString()).child("time").value.toString()
                        notificationList.add(NotificationInstance(title,message,time, notif.key.toString()))
                    }
                }
                else{
                    tvNoNotifs.visibility = View.VISIBLE
                }
                notifAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun deleteNotif(notif : NotificationInstance){
        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser = firebaseAuth.currentUser
        if(firebaseUser != null){
            val uid = firebaseUser.uid
            val curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
            val notifDirectory = curUser?.child("notifications")
            notifDirectory?.child(notif.id)?.removeValue()
        }
    }
}