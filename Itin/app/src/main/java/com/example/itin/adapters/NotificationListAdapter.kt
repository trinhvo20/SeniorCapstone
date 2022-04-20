package com.example.itin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.example.itin.classes.NotificationInstance

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
        }
    }

    override fun getItemCount(): Int {
        return notificationList.size
    }

}