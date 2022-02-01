package com.example.itin

import android.content.SharedPreferences
import androidx.preference.PreferenceFragmentCompat
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceManager

class SettingsFragment : PreferenceFragmentCompat() {

    private var preferenceChangeListener: OnSharedPreferenceChangeListener? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        // listener for changed preferences
        preferenceChangeListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == DARK_MODE) {
                loadSettings()
            }
        }
    }

    // function to take the settings from root preferences and put them into action
    private fun loadSettings(){
        val sp = PreferenceManager.getDefaultSharedPreferences(context)

        val dark = sp.getBoolean("dark_mode_key",false)
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

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(
            preferenceChangeListener
        )
        loadSettings()
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(
            preferenceChangeListener
        )
    }

    // constants
    companion object {
        const val DARK_MODE = "dark_mode_key"
    }


}