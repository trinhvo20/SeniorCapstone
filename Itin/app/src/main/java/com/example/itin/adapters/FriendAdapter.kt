package com.example.itin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.example.itin.classes.User
import kotlinx.android.synthetic.main.friend_item.view.*

class FriendAdapter (
    private val friends: MutableList<User>
): RecyclerView.Adapter<FriendAdapter.FriendViewHolder>(){

    inner class FriendViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.friend_item, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendAdapter.FriendViewHolder, position: Int) {
        val curFriend = friends[position]

        holder.itemView.apply {
            friendFullName.text = curFriend.username
        }
    }

    override fun getItemCount(): Int {
        return friends.size
    }

}