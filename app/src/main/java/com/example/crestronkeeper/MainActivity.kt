package com.example.crestronkeeper

import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_USER_PRESENT)
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if( (intent.action == Intent.ACTION_USER_PRESENT) || (intent.action == Intent.ACTION_SCREEN_ON) ) {
                    val crestronIntent = Intent(context, MainActivity::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        crestronIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    crestronIntent.component =
                        ComponentName("air.com.crestron.andros", "air.com.crestron.andros.AppEntry")
                    try {
                        startActivity(crestronIntent)
                    } catch( ex: Exception ) {
                        ex.printStackTrace()
                    }
                }
            }
        }, intentFilter)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(applicationContext, HttpServerService::class.java))
        } else {
            startService(Intent(applicationContext, HttpServerService::class.java))
        }

        val btnClick = findViewById<Button>(R.id.button)
        btnClick.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
}