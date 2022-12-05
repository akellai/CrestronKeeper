package com.example.crestronkeeper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.net.URL

class StartupOnBootUpReceiver  : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "${intent.action}")

        if( Intent.ACTION_BOOT_COMPLETED == intent.action ) {
            val activityIntent = Intent(context, MainActivity::class.java)
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(activityIntent)
        }
        else if( Intent.ACTION_USER_PRESENT == intent.action ) {
            val thread = Thread {
                try {
                    URL("http://127.0.0.1:${Constants.PORT}/switch").readBytes()
                } catch (ex: Exception) {
                    // expected 404 FileNotFoundException
                    ex.printStackTrace()
                }
            }
            thread.start()
        }
    }

    companion object {
        private val TAG = StartupOnBootUpReceiver::class.java.simpleName
    }
}