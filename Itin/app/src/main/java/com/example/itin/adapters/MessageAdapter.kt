package com.example.itin.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.itin.R
import com.example.itin.classes.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MessageAdapter(
    val context: Context,
    private val messageList: MutableList<Message>
    ): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val ITEM_RECEIVE = 1
    private val ITEM_SENT = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 1) {
            // inflate RECEIVE
            val view: View = LayoutInflater.from(context).inflate(R.layout.received_message, parent, false)
            ReceivedViewHolder(view)
        } else {
            // inflate SENT
            val view: View = LayoutInflater.from(context).inflate(R.layout.sent_message, parent, false)
            SentViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = messageList[position]

        if (holder.javaClass == SentViewHolder::class.java) {
            // sent view holder
            holder as SentViewHolder
            holder.sentMessage.text = currentMessage.message
            holder.timeSentTV.text = currentMessage.time
        }
        else {
            // received view holder
            holder as ReceivedViewHolder
            holder.receivedMessage.text = currentMessage.message
            holder.timeReceivedTV.text = currentMessage.time
            getSenderName(currentMessage, holder)
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun getItemViewType(position: Int): Int {
        val currentMessage = messageList[position]

        // if the currentMessage is sent by the currentUser, return code ITEM_SENT
        return if (FirebaseAuth.getInstance().currentUser?.uid.equals(currentMessage.senderId)) {
            ITEM_SENT    // 2
        } else {  // if the currentMessage is sent by the other users, return code ITEM_RECEIVE
            ITEM_RECEIVE // 1
        }
    }

    class SentViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val sentMessage: TextView = itemView.findViewById(R.id.sentMessage)
        val timeSentTV: TextView = itemView.findViewById(R.id.timeSentTV)
    }

    class ReceivedViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val receivedMessage: TextView = itemView.findViewById(R.id.receivedMessage)
        val timeReceivedTV: TextView = itemView.findViewById(R.id.timeReceivedTV)
        val senderNameTV: TextView = itemView.findViewById(R.id.senderNameTV)
    }

    private fun getSenderName(message: Message, holder: ReceivedViewHolder) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid.toString()
        FirebaseDatabase.getInstance().reference.child("users")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (user in snapshot.children) {
                        if (user.key == message.senderId) {
                            val senderName = user.child("userInfo").child("username").value.toString()

                            holder.senderNameTV.text = senderName
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
    }
}