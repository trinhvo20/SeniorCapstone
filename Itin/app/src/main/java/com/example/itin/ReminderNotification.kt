package com.example.itin

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.preference.PreferenceManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.RemoteMessage
import java.text.SimpleDateFormat
import java.util.*

const val notificationID = 1
const val channelID = "channel1"
const val titleExtra = "titleExtra"
const val messageExtra = "messageExtra"

class ReminderNotification : BroadcastReceiver()
{
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent)
    {
        val receiveNotification = loadSettings(context)
        sendToDB(intent)

        if(receiveNotification) {
            val notification = NotificationCompat.Builder(context, channelID)
                .setSmallIcon(R.drawable.ic_logo_itin_redo)
                .setContentTitle(intent.getStringExtra(titleExtra))
                .setContentText(intent.getStringExtra(messageExtra))
                .build()

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(notificationID, notification)
        }
    }

    // function to take the settings from root preferences and put them into action
    private fun loadSettings(context: Context): Boolean{
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        // return the value of friend_request_key from the system preferences
        return sp.getBoolean("trip_remind_key",true)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkUser(): DatabaseReference? {
        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser = firebaseAuth.currentUser
        var curUser: DatabaseReference? = null
        val uid = firebaseUser?.uid
        if(uid != null) {
            curUser = FirebaseDatabase.getInstance().getReference("users").child(uid)
        }
        return curUser
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendToDB(intent: Intent){
        val curUser = checkUser()
        val notifInstance = curUser?.child("notifications")
        if (notifInstance != null) {
            notifInstance.child("title").setValue(intent.getStringExtra(titleExtra))
            notifInstance.child("message").setValue(intent.getStringExtra(messageExtra))
            val time = getTime()
            notifInstance.child("time").setValue(time)
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getTime(): String{
        val calendar = Calendar.getInstance()
        val dateFormat = "dd/MM hh:mm"
        val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
        return formatter.format(calendar.time)
    }


}