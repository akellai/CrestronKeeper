package com.example.crestronkeeper

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread


class HttpServerService : Service() {
    private val working = AtomicBoolean(true)
    private val pinging = AtomicBoolean(false)
    private val pingSeconds = AtomicInteger(0 )

    private val runnable = Runnable {
        var socket: Socket? = null
        var serverSocket: ServerSocket? = null
        val sharedPreference = PreferenceManager.getDefaultSharedPreferences(this)
        var boolHttpLocal = sharedPreference.getBoolean("switch_preference_local_web", true)

        runCrestron(false)

        while (working.get()) {
            if (serverSocket==null)
                serverSocket = openServerSocket(boolHttpLocal)

            if (serverSocket==null) {
                Thread.sleep(10000L)
                continue
            }

            try {
                socket = serverSocket.accept()
                Log.i(TAG, "New client: $socket")
                val dataInputStream = DataInputStream(socket.getInputStream())
                val dataOutputStream = DataOutputStream(socket.getOutputStream())

                var queryString: String? = null
                while (true) {
                    Thread.sleep(50L)
                    val rb = dataInputStream.available()
                    if (rb <= 0) break
                    val buf = ByteArray(rb)
                    dataInputStream.read(buf, 0, rb)
                    val stringBuf = String(buf)
                    if (queryString == null) {
                        queryString = Regex("(?<=GET ).*?(?= HTTP/1.1)").find(stringBuf)?.value
                    }
                }
                Log.i(TAG, "queryString: $queryString")
                if (queryString != null) {
                    try {
                        dataOutputStream.writeBytes(
                            "HTTP/1.1 404 Not Found\n" +
                                    "Content-Type: text/plain\n" +
                                    "Content-Length: 15\n" +
                                    "Connection: close\n" +
                                    "\n" +

                                    "404: Not Found\n"
                        )
                    }
                    catch(_:Exception) {
                        //just ignore errors
                    }

                    val tokens = queryString.split("/")
                    var cmd = ""
                    if ( tokens.size>1 )
                        cmd = tokens[1]
                    var param = ""
                    if ( tokens.size>2 )
                        param = tokens[2]

                    when (cmd) {
                        "local-listener" -> {
                            boolHttpLocal = ("true" == param)
                            serverSocket.close()
                            serverSocket = null
                        }
                        "reset" -> resetCrestron()
                        "ping" -> {
                            val result = param.filter { it.isDigit() }
                            runCrestronDelayed(result.toInt())
                        }
                        "" -> {}
                        else -> runCrestron(false)
                    }
                }
                dataInputStream.close()
                dataOutputStream.close()
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    socket?.close()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            }
        }
    }

    private fun openServerSocket(boolHttpLocal: Boolean ): ServerSocket? {
        try {
            if (boolHttpLocal)
                return ServerSocket(Constants.PORT, 1, InetAddress.getByName("127.0.0.1"))
            return ServerSocket(Constants.PORT)
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
        return null
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
                runCrestron(true)
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
                    dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(dialogIntent)
                Thread.sleep(3000)
                runCrestron(true)
            } catch( ex: Exception ) {
                ex.printStackTrace()
                val toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 30)
                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
            }
        }
    }

    private fun runCrestron(boolKill: Boolean)
    {
        if( boolKill ) {
            try {
                val am = getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager
                am.killBackgroundProcesses(Constants.crestronPackageName)
            }
            catch (_: Exception) {}

            Thread.sleep(200)
        }

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

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        startMeForeground()
        Thread(runnable).start()
        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_USER_PRESENT)
        registerReceiver(StartupOnBootUpReceiver(), intentFilter)
    }

    override fun onDestroy() {
        working.set(false)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun startMeForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = packageName
            val channelName = "CrestronKeeper Background Service"
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
            chan.lightColor = Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            manager.createNotificationChannel(chan)
            val notificationBuilder = NotificationCompat.Builder(this, channelId)
            val notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("CrestronKeeper is running")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
            startForeground(2, notification)
        } else {
            startForeground(1, Notification())
        }
    }

    companion object {
        private val TAG = HttpServerService::class.java.simpleName
    }
}