package com.example.itin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.itin.classes.Activity
import com.example.itin.classes.Trip
import kotlinx.android.synthetic.main.activity_details.*

class DetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        val activity = intent.getSerializableExtra("ACTIVITY") as Activity

        tvName.text = activity.name
        tvTime.text = activity.time




    }
}