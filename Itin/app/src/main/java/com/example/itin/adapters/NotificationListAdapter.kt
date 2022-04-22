package com.example.itin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.example.itin.classes.NotificationInstance
import kotlinx.android.synthetic.main.add_activity.view.*
import kotlinx.android.synthetic.main.notification_item.view.*
import kotlinx.android.synthetic.main.notification_item.view.tvTitle
import kotlinx.android.synthetic.main.trip_item.view.*

class NotificationListAdapter (
    private val notificationList : MutableList<NotificationInstance>
) : RecyclerView.Adapter<NotificationListAdapter.NotificationListViewHolder>() {
    inner class NotificationListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v  = inflater.inflate(R.layout.notification_item,parent,false)
        return NotificationListViewHolder(v)
    }

    override fun onBindViewHolder(holder: NotificationListViewHolder, position: Int) {
        var notification = notificationList[position]
        holder.itemView.apply {
            tvTitle.text = notification.notifTitle
            tvMessage.text = notification.notifMessage
            tvNotifTime.text = notification.notifTime
            if(notification.notifTitle == "Friend Request"){
                notifImage.setImageResource(R.drawable.ic_friends)
            }else if(notification.notifTitle == "Trip Invitation"){
                notifImage.setImageResource(R.drawable.ic_mail)
            }else if(notification.notifTitle == "Group Message"){
                notifImage.setImageResource(R.drawable.ic_message)
            }else{
                notifImage.setImageResource(R.drawable.ic_trips)
            }
        }
    }

    override fun getItemCount(): Int {
        return notificationList.size
    }

    fun clear() {
        notificationList.clear()
        notifyDataSetChanged()
    }

}