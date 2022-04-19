package com.example.itin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_settings.backBtn


class NotificationListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_list)

        // Finishes activity when back button is finished
        backBtn.setOnClickListener {
            finish()
            startActivity(Intent(this, TripActivity::class.java))
        }

    }
}