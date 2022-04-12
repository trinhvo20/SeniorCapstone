package com.example.itin

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import androidx.core.app.NotificationCompat

const val notificationID = 1
const val channelID = "channel1"
const val titleExtra = "titleExtra"
const val messageExtra = "messageExtra"

class ReminderNotification : BroadcastReceiver()
{
    override fun onReceive(context: Context, intent: Intent)
    {
        val receiveNotification = loadSettings(context)

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

}