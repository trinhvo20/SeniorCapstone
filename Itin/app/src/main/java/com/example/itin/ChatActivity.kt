package com.example.itin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.adapters.MessageAdapter
import com.example.itin.classes.Message
import com.example.itin.classes.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_chat.*

class ChatActivity : AppCompatActivity() {

    private lateinit var senderUid: String
    private lateinit var receiverUidList: MutableList<String>
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: MutableList<Message>
    private lateinit var databaseReference: DatabaseReference

    var receiverRoom: String? = null
    var senderRoom: String? = null
    var groupName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        databaseReference = FirebaseDatabase.getInstance().reference
        checkUser()

        // set the name of the chat box (= tripName)
        val trip = intent.getSerializableExtra("trip") as Trip
        groupChatName.text = trip.name
        receiverUidList = trip.viewers

        groupName = "Group Chat Trip ${trip.tripID}"
        senderRoom = "Sender Room"
        receiverRoom = "Receiver Room"

        messageList = mutableListOf()
        messageAdapter = MessageAdapter(this, messageList)

        chatRV.adapter = messageAdapter
        chatRV.layoutManager = LinearLayoutManager(this)
        //messageBox
        // Add data from Database to Recyclerview
        loadMessagesFromDB()

        // Send message to Database
        sendMessageBtn.setOnClickListener { sendMessagesToDB() }

        backBtn.setOnClickListener {
            /*
            finish()
            Intent(this, ItineraryActivity::class.java).also {
                startActivity(it)
            }
             */
        }
    }

    private fun sendMessagesToDB() {
        val message = messageBox.text.toString()
        if (message.isNotBlank()) {
            val messageObj = Message(message, senderUid)

            databaseReference.child("chats").child(groupName!!).child(senderRoom!!).push()
                .setValue(messageObj).addOnSuccessListener {
                    databaseReference.child("chats").child(groupName!!).child(receiverRoom!!).push()
                        .setValue(messageObj)
                }
            messageBox.setText("")
        }
        else {
        }
    }

    private fun loadMessagesFromDB() {
        databaseReference.child("chats").child(groupName!!).child(senderRoom!!)
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()     // clear the old list to load new list
                    for (postSnapshot in snapshot.children) {
                        val message = postSnapshot.getValue(Message::class.java)
                        messageList.add(message!!)
                    }
                    messageAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    private fun checkUser() {
        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            startActivity(Intent(this, GoogleLogin::class.java))
        }
        else {
            senderUid = firebaseUser.uid
        }
    }
}