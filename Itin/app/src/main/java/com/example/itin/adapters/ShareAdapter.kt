package com.example.itin.adapters

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.example.itin.ShareTripActivity
import com.example.itin.classes.Trip
import kotlinx.android.synthetic.main.friend_share_item.view.*
import kotlinx.android.synthetic.main.trip_item.view.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class ShareAdapter(
    private val context: Context,
    private val Friends: MutableList<String>,

) : RecyclerView.Adapter<ShareAdapter.ShareViewHolder>() {

    inner class ShareViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ibShareFriend: ImageView = itemView.findViewById(R.id.ibShareFriend)
        init {
            ibShareFriend.setOnClickListener { ShareByFriend(it) }
        }

        inner class ShareByFriend(it: View?) {
            val curFriend = Friends[adapterPosition]
            val toast = Toast.makeText(context, curFriend, Toast.LENGTH_SHORT).show()

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShareViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.friend_share_item, parent, false)
        return ShareViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShareViewHolder, position: Int) {
        val curFriend = Friends[position]

        holder.itemView.apply {
            tvFriendName.text = curFriend
        }
    }

    override fun getItemCount(): Int {
        return Friends.size
    }
}