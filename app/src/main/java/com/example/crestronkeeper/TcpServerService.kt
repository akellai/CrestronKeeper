package com.example.crestronkeeper

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread


class TcpServerService : Service() {
    private var serverSocket: ServerSocket? = null
    private val working = AtomicBoolean(true)
    private val pinging = AtomicBoolean(false)
    private val pingSeconds = AtomicInteger(0 )

    private val runnable = Runnable {
        var socket = Socket()
        val sharedPreference = PreferenceManager.getDefaultSharedPreferences(this);
        var blocal = sharedPreference.getBoolean("switch_preference_local_web", true)
        var serverSocket = openServerSocket(blocal)

        runCrestron()
        while (working.get()) {
            if (serverSocket.isClosed)
                serverSocket = openServerSocket(blocal)

            if (!serverSocket.isBound) {
                try {
                    serverSocket.close()
                } catch (ex: Exception) {
                    // just in case
                }
                Thread.sleep(10000L)
                continue
            }

            try {
                socket = serverSocket.accept()
                Log.i(TAG, "New client: $socket")
                val dataInputStream = DataInputStream(socket.getInputStream())
                val dataOutputStream = DataOutputStream(socket.getOutputStream())

                var qstring: String? = null
                while (true) {
                    Thread.sleep(50L)
                    val rb = dataInputStream.available()
                    if (rb <= 0) break
                    val buf: ByteArray = ByteArray(rb)
                    dataInputStream.read(buf, 0, rb)
                    val sget: String = String(buf)
                    if (qstring == null) {
                        qstring = Regex("(?<=GET ).*?(?= HTTP/1.1)").find(sget)?.value
                    }
                }
                Log.i(TAG, "qstring: $qstring")
                if (qstring != null) {
                    dataOutputStream.writeBytes(
                        "HTTP/1.1 404 Not Found\n" +
                                "Content-Type: text/plain\n" +
                                "Content-Length: 15\n" +
                                "Connection: close\n" +
                                "\n" +
                                "404: Not Found\n"
                    )
                }
                dataInputStream.close()
                dataOutputStream.close()
                socket.close()

                if (qstring != null) {
                    if (qstring == "/local-listener-true") {
                        serverSocket.close()
                        blocal = true
                    } else if (qstring == "/local-listener-false") {
                        serverSocket.close()
                        blocal = false
                    } else if (qstring.startsWith("/reset"))
                        resetCrestron()
                    else if (qstring.startsWith("/ping")) {
                        var result = qstring.filter { it.isDigit() }
                        runCrestronDelayed(result.toInt())
                    } else
                        runCrestron();
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    socket.close()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            }
        }
    }

    private fun openServerSocket( blocal: Boolean ): ServerSocket {
        try {
            if (blocal)
                return ServerSocket(Constants.PORT, 1, InetAddress.getByName("127.0.0.1"))
            return ServerSocket(Constants.PORT)
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
        return ServerSocket()
    }

    private fun runCrestronDelayed(seconds: Int)
    {
        pingSeconds.set(seconds)
        if( pinging.compareAndSet(false,true) )
        {
            thread {
                while( pingSeconds.decrementAndGet()>0 ) {
                    Thread.sleep(1000)
                    Log.i(TAG, "PING: $pingSeconds")
                }
                runCrestron()
                pinging.set(false)
            }
        }
    }

    private fun resetCrestron()
    {
        thread {
            try {
                val dialogIntent = Intent(this, MainActivity::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                startActivity(dialogIntent)
                Thread.sleep(3000)
                val am = getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager
                am.killBackgroundProcesses("air.com.crestron.andros")
                runCrestron()
            } catch( ex: Exception ) {
                ex.printStackTrace()
                val toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
            }
        }
    }

    private fun runCrestron()
    {
        val intent = Intent(Intent.ACTION_MAIN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        try {
            intent.component =
                ComponentName("air.com.crestron.andros", "air.com.crestron.andros.AppEntry")
                startActivity(intent)
        } catch( ex: Exception ) {
            ex.printStackTrace()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        startMeForeground()
        Thread(runnable).start()
    }

    override fun onDestroy() {
        working.set(false)
    }

    private fun startMeForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val NOTIFICATION_CHANNEL_ID = packageName
            val channelName = "Tcp Server Background Service"
            val chan = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE)
            chan.lightColor = Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            manager.createNotificationChannel(chan)
            val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            val notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Tcp Server is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
            startForeground(2, notification)
        } else {
            startForeground(1, Notification())
        }
    }

    companion object {
        private val TAG = TcpServerService::class.java.simpleName
    }
}