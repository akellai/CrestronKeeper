package com.example.crestronkeeper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import java.net.URL

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val preference: Preference? = findPreference("switch_preference_local_web")
            preference!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue -> //do your action here
                    val thread = Thread {
                        try {
                            URL("http://127.0.0.1:${Constants.PORT}/local-listener-$newValue").readBytes()
                        } catch (ex: Exception) {
                            // expected 404 FileNotFoundException
                            ex.printStackTrace()
                        }
                    }
                    thread.start()
                    true
                }
        }
    }
}