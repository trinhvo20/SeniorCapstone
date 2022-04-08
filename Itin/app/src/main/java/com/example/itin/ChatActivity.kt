package com.example.itin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itin.adapters.MessageAdapter
import com.example.itin.classes.Message
import com.example.itin.classes.Trip
import com.example.itin.notifications.NotificationData
import com.example.itin.notifications.PushNotification
import com.example.itin.notifications.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    // Variable for error messages
    private val TAG = "ChatActivity"

    private lateinit var senderUid: String
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: MutableList<Message>
    private lateinit var databaseReference: DatabaseReference
    private lateinit var tripName: String
    private lateinit var tripViewers: MutableList<String>

    private var receiverRoom: String? = null
    private var senderRoom: String? = null
    private var groupName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        databaseReference = FirebaseDatabase.getInstance().reference
        checkUser()

        // set the name of the chat box (= tripName)
        val trip = intent.getSerializableExtra("trip") as Trip
        tripName = trip.name
        tripViewers = trip.viewers

        //set the groupchat name
        groupChatName.text = tripName

        groupName = "Group Chat Trip ${trip.tripID}"
        senderRoom = "Sender Room"
        receiverRoom = "Receiver Room"

        messageList = mutableListOf()
        messageAdapter = MessageAdapter(this, messageList)

        chatRV.adapter = messageAdapter
        chatRV.layoutManager = LinearLayoutManager(this)

        // Add data from Database to Recyclerview
        loadMessagesFromDB()

        // Send message to Database
        sendMessageBtn.isEnabled = false

        messageBox.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                sendMessageBtn.isEnabled = false
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                sendMessageBtn.isEnabled = s.toString() != ""
            }
            override fun afterTextChanged(s: Editable?) {
                sendMessageBtn.setOnClickListener {
                    sendMessagesToDB(s.toString())
                    // send a notification to everyone on the trip
                    for(viewer in tripViewers){
                        if(viewer != senderUid){
                            createNotification(viewer)
                        }
                    }
                }
            }
        })

        backBtn.setOnClickListener { finish() }
    }

    private fun sendMessagesToDB(message: String) {
        val sdf = SimpleDateFormat("dd/M/yyyy  hh:mm:ss")
        val currentDate = sdf.format(Date())
        val messageObj = Message(message, senderUid, currentDate)

        databaseReference.child("chats").child(groupName!!).child("messages").push()
            .setValue(messageObj).addOnSuccessListener {
            }
        messageBox.setText("")

    }

    private fun loadMessagesFromDB() {
        databaseReference.child("chats").child(groupName!!).child("messages")
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(messages: DataSnapshot) {
                    messageList.clear()     // clear the old list to load new list
                    for (eachMessage in messages.children) {
                        val message = eachMessage.getValue(Message::class.java)
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

    // function to make get the token of who we are sending the notification to
    // then fills out notification
    private fun createNotification(friendUID: String){
        FirebaseDatabase.getInstance().getReference("users").get().addOnSuccessListener {
            val friendToken = it.child(friendUID).child("userInfo").child("token").value.toString()
            val fullName = it.child(senderUid).child("userInfo").child("fullName").value.toString()
            val title = "Group Message"
            val message = "$fullName sent a message to ${tripName}'s group chat!"
            PushNotification(
                NotificationData(title,message),
                friendToken
            ).also{
                sendNotification(it)
            }
        }
    }

    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if(response.isSuccessful) {
                Log.d(TAG, "Response: Success!")
            } else {
                Log.e(TAG, response.errorBody().toString())
            }
        } catch(e: Exception) {
            Log.e(TAG, e.toString())
        }
    }
}