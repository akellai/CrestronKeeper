package com.example.crestronkeeper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StartupOnBootUpReceiver  : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        System.out.println(intent.getAction())
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            val activityIntent = Intent(context, MainActivity::class.java)
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(activityIntent)
        }
    }
}