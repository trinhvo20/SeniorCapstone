// source code from Philipp Lackner
package com.example.itin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.random.Random

private const val CHANNEL_ID = "my_channel"

class FirebaseService : FirebaseMessagingService() {

    var friendReq = true
    var tripInvite = true
    var groupMessage = true
    var sendMessage = true
    lateinit var activity: String

    companion object {
        var sharedPref: SharedPreferences? = null

        var token: String?
            get() {
                return sharedPref?.getString("token", "")
            }
            set(value) {
                sharedPref?.edit()?.putString("token", value)?.apply()
            }
    }

    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
        token = newToken
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMessageReceived(message: RemoteMessage) {
        loadSettings()
        super.onMessageReceived(message)

        // get the title of the message
        val title = message.data["title"]
        if(title == "Friend Request"){
            if(friendReq) {
                populateMessage(message,FriendActivity::class.java,null,null)
            }
        }
        else if(title == "Trip Invitation"){
            if(tripInvite) {
                populateMessage(message,null, TripActivity::class.java, null)
            }
        }
        else if(title == "Group Message"){
            if(groupMessage) {
                populateMessage(message,null,null,TripActivity::class.java)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channelName = "channelName"
        val channel = NotificationChannel(CHANNEL_ID, channelName, IMPORTANCE_HIGH).apply {
            description = "My channel description"
            enableLights(true)
            lightColor = Color.GREEN
        }
        notificationManager.createNotificationChannel(channel)
    }

    // function to take the settings from root preferences and put them into action
    private fun loadSettings(){
        val sp = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        // return the value of friend_request_key from the system preferences
        friendReq = sp.getBoolean("friend_request_key",true)
        tripInvite = sp.getBoolean("trip_invite_key",true)
        groupMessage = sp.getBoolean("group_message_key",true)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun populateMessage(message: RemoteMessage, friendR:Class<FriendActivity>?, tripI:Class<TripActivity>?, groupM:Class<TripActivity>?){
        var intent: Intent? = null

        // if you wanted to send to other activities, replace NotificationList Activity with friendR, tripI, etc.
        if(friendR == null && tripI == null){
            intent = Intent(this, NotificationListActivity::class.java)
        }
        else if(groupM == null && tripI == null){
            intent = Intent(this, NotificationListActivity::class.java)
        }
        else if(groupM == null && friendR == null){
            intent = Intent(this, NotificationListActivity::class.java)
        }
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationID = Random.nextInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(message.data["title"])
            .setContentText(message.data["message"])
            .setSmallIcon(R.drawable.ic_logo_itin_redo)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationID, notification)
        sendToDB(message)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkUser(): DatabaseReference? {
        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser = firebaseAuth.currentUser
        var curUser: DatabaseReference? = null
        // If the use is not current logged in:
        if (firebaseUser == null) {
            startActivity(Intent(this, GoogleLogin::class.java))
        } else {
            val uid = firebaseUser.uid
            curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
        }
        return curUser
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendToDB(message: RemoteMessage){
        val curUser = checkUser()
        val notifDirectory = curUser?.child("notifications")
        if (notifDirectory != null) {
            val notifInstance = notifDirectory.push()
            notifInstance.child("title").setValue(message.data["title"])
            notifInstance.child("message").setValue(message.data["message"])
            val time = getTime()
            notifInstance.child("time").setValue(time)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getTime(): String{
        val calendar = Calendar.getInstance()
        val dateFormat = "MM/dd hh:mm"
        val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
        return formatter.format(calendar.time)
    }

}