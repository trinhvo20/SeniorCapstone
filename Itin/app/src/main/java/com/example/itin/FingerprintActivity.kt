// fingerprint authentication in android applications
// source: Briana Nzivu

package com.example.itin

import android.app.KeyguardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CancellationSignal
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

class FingerprintActivity : AppCompatActivity() {

    private var cancellationSignal: CancellationSignal? = null
    private val  authenticationCallback: BiometricPrompt.AuthenticationCallback
        get() =
            @RequiresApi(Build.VERSION_CODES.P)
            object: BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    notifyUser("Authentication error: $errString")
                }
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    notifyUser("Authentication Success!")
                    startActivity(Intent(this@FingerprintActivity, TripActivity::class.java))
                }
            }
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fingerprint)

        val button = findViewById<Button>(R.id.authenticate)
        button.setOnClickListener{
            val biometricPrompt : BiometricPrompt = BiometricPrompt.Builder(this)
                .setTitle("Itin Security")
                .setDescription("Fingerprint Authentication")
                .setNegativeButton("Cancel", this.mainExecutor, DialogInterface.OnClickListener { dialog, which ->
                }).build()
            biometricPrompt.authenticate(getCancellationSignal(), mainExecutor, authenticationCallback)
        }

          //gradiant for welcome page
//        val paint = binding.welcomeTV.paint
//        val width = paint.measureText(binding.welcomeTV.text.toString())
//        val textShader: Shader = LinearGradient(0f, 0f, width, binding.welcomeTV.textSize, intArrayOf(
//            Color.parseColor("#F97C3C"),
//            Color.parseColor("#FDB54E"),
//            Color.parseColor("#8446CC")
//        ), null, Shader.TileMode.REPEAT)
//        binding.welcomeTV.paint.shader = textShader
    }
    private fun notifyUser(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    private fun getCancellationSignal(): CancellationSignal {
        cancellationSignal = CancellationSignal()
        cancellationSignal?.setOnCancelListener {
            notifyUser("Authentication was cancelled by the user")
        }
        return cancellationSignal as CancellationSignal
    }
}