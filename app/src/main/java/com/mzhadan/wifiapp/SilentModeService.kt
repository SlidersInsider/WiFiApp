package com.mzhadan.wifiapp

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import android.app.NotificationManager
import android.app.NotificationChannel
import com.mzhadan.wifiapp.Constants.Companion.networkArrayList

@DelicateCoroutinesApi
@RequiresApi(Build.VERSION_CODES.O)
class SilentModeService() : Service() {

    private lateinit var wifiManager: WifiManager
    private lateinit var wifiInfo: WifiInfo
    private lateinit var audioManager: AudioManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkRequest: NetworkRequest
    private lateinit var wiFiNetworkCallback: WiFiNetworkCallback
    private lateinit var serviceNotificationManager: NotificationManager
    private lateinit var serviceChannel: NotificationChannel
    private lateinit var serviceNotification: Notification
    private val tempNetworkArrayList = networkArrayList
    private val CHANNEL_ID = "silentServiceChannel"

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        initManagers()
        startForeground(1, serviceNotification)
    }

    @DelicateCoroutinesApi
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        connectivityManager.registerNetworkCallback(networkRequest, wiFiNetworkCallback)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun initManagers() {
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        wifiInfo = wifiManager.connectionInfo as WifiInfo

        networkRequest = NetworkRequest.Builder().build()
        connectivityManager = applicationContext.getSystemService(ConnectivityManager::class.java)
        wiFiNetworkCallback = WiFiNetworkCallback(wifiManager, connectivityManager, audioManager, tempNetworkArrayList)

        serviceChannel = NotificationChannel(CHANNEL_ID, "WiFiNotification", NotificationManager.IMPORTANCE_HIGH)
        serviceNotificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        serviceNotificationManager.createNotificationChannel(serviceChannel)
        serviceNotification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("WiFi Status")
            .setContentText("Connected to: ${wifiInfo.ssid}")
            .build()
    }
}