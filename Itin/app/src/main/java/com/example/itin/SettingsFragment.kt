package com.example.itin

import android.content.SharedPreferences
import androidx.preference.PreferenceFragmentCompat
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceManager
// imports for biometric scanner
import android.app.KeyguardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.CancellationSignal
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat


class SettingsFragment : PreferenceFragmentCompat() {

    private var preferenceChangeListener: OnSharedPreferenceChangeListener? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        // listener for changed preferences
        preferenceChangeListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == DARK_MODE) {
                loadSettings()
            }
            // if fingerprint is changed
            if (key == FINGERPRINT) {
                // get the value for fingerprint
                val sp = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
                val fingerprint = sp?.getBoolean("fingerprint_key",false)
                // if they changed it to true
                if("$fingerprint" == "true"){
                    // check to see if fingerprint scanner is supported
                    val biometricSupport = checkBiometricSupport()
                    // if its not supported, change to false
                    if(!biometricSupport){
                        val prefEditor = sp?.edit()
                        prefEditor?.putBoolean("fingerprint_key",false)
                        prefEditor?.commit()
                        // refresh the page
                        activity?.finish()
                        activity?.overridePendingTransition(0,0)
                        activity?.startActivity(activity?.intent)
                        activity?.overridePendingTransition(0,0)
                    }
                }
            }
            if (key == CACHE){
                // get the value for cache
                val sp = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
                val cache = sp?.getBoolean(CACHE,false)
                // if they changed it to true
                if("$cache" == "true") {
                    // clear the cache
                    context?.cacheDir?.deleteRecursively()
                    notifyUser("Cache cleared.")
                    // recreate directory
                    context?.cacheDir
                    //change back to false
                    val prefEditor = sp?.edit()
                    prefEditor?.putBoolean(CACHE, false)
                    prefEditor?.commit()
                    // refresh the page
                    activity?.finish()
                    activity?.overridePendingTransition(0, 0)
                    activity?.startActivity(activity?.intent)
                    activity?.overridePendingTransition(0, 0)
                }
            }
        }
    }

    // function to take the settings from root preferences and put them into action
    private fun loadSettings(){
        val sp = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }

        val dark = sp?.getBoolean("dark_mode_key",false)
        if("$dark" == "false"){
            // this is light mode
            val theme = AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(theme)
        }
        else{
            // this is dark mode
            val theme = AppCompatDelegate.MODE_NIGHT_YES
            AppCompatDelegate.setDefaultNightMode(theme)
        }
    }

    private fun checkBiometricSupport(): Boolean {
        val keyguardManager : KeyguardManager = activity?.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if(!keyguardManager.isKeyguardSecure) {
            notifyUser("Fingerprint has not been enabled in system settings.")
            return false
        }
        if (ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.USE_BIOMETRIC) !=PackageManager.PERMISSION_GRANTED) {
            notifyUser("Fingerprint has not been enabled in system settings.")
            return false
        }
        return if (requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            true
        } else true
    }

    // create a toast
    private fun notifyUser(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(
            preferenceChangeListener
        )
        loadSettings()
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(
            preferenceChangeListener
        )
    }

    // constants
    companion object {
        const val DARK_MODE = "dark_mode_key"
        const val FINGERPRINT = "fingerprint_key"
        const val CACHE = "cache_key"
    }

}