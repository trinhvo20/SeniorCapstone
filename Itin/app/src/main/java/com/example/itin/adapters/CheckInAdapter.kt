package com.example.itin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.checkin_item.view.*

class CheckInAdapter (
    private val checkInList : MutableList<String>
    ) : RecyclerView.Adapter<CheckInAdapter.CheckInViewHolder>() {
    inner class CheckInViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckInViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v  = inflater.inflate(R.layout.checkin_item,parent,false)
        return CheckInViewHolder(v)
    }

    override fun onBindViewHolder(holder: CheckInViewHolder, position: Int) {
        var uid = checkInList[position]
        holder.itemView.apply {
            FirebaseDatabase.getInstance().reference.child("users")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (user in snapshot.children) {
                            if (user.key == uid) {
                                val name = user.child("userInfo").child("username").value.toString()

                                checkinName.text = name
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })
        }
    }

    override fun getItemCount(): Int {
        return checkInList.size
    }

}
