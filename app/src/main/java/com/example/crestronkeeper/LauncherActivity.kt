package com.example.crestronkeeper

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Browser
import android.util.Log
import java.net.URL

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cmd = intent.dataString?.replaceFirst(Regex("[^:]*:[/][/]"),"")
        Log.i("TAG","data: $cmd")
        val intent1 = Intent(Intent.ACTION_VIEW, Uri.parse(cmd))
        intent1.putExtra(Browser.EXTRA_APPLICATION_ID, baseContext.packageName)
        startActivity(intent1)

        val thread = Thread {
            try {
                URL("http://127.0.0.1:${Constants.PORT}/ping/120").readBytes()
            } catch (ex: Exception) {
                // expected 404 FileNotFoundException
                ex.printStackTrace()
            }
        }
        thread.start()

        // removing itself from history so that BACK works as expected
        finishAffinity()
    }
}