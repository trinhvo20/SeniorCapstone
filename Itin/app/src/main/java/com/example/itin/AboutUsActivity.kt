package com.example.itin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_about_us.*


class AboutUsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)

        linearLayout5.setOnClickListener{ sendEmail() }

        backUsBtn.setOnClickListener { finish() }
    }

    private fun sendEmail(){
        val uri = Uri.parse("mailto:itin.inquiry@gmail.com")
        val it = Intent(Intent.ACTION_SENDTO, uri)
        startActivity(it)
    }
}