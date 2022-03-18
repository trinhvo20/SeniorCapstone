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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShareViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.friend_share_item, parent, false)
        return ShareViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShareViewHolder, position: Int) {
        val curFriend = Friends[position]

        holder.itemView.apply {
            tvName.text = curFriend
        }
    }

    override fun getItemCount(): Int {
        return Friends.size
    }

    // this interface will handle the RecyclerView clickable
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }
}