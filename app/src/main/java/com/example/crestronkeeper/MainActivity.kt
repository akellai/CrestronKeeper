package com.example.crestronkeeper

import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import java.net.URL

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_USER_PRESENT)
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if ((intent.action == Intent.ACTION_USER_PRESENT) || (intent.action == Intent.ACTION_SCREEN_ON)) {
                    runCrestron()
                }
            }
        }, intentFilter)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(applicationContext, HttpServerService::class.java))
        } else {
            startService(Intent(applicationContext, HttpServerService::class.java))
        }
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        val sharedPreference = PreferenceManager.getDefaultSharedPreferences(this)
        val boolPermissions = sharedPreference.getBoolean("switch_preference_permissions", true)
        if (boolPermissions) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
    }

    private fun runCrestron()
    {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            intent.component =
                ComponentName(Constants.crestronPackageName, Constants.crestronComponentPackage)
            startActivity(intent)
        } catch( ex: Exception ) {
            ex.printStackTrace()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val preferenceHttpLocal: Preference? = findPreference("switch_preference_local_web")
            preferenceHttpLocal!!.onPreferenceChangeListener =
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