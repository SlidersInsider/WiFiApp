package com.mzhadan.wifiapp

import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.*

@DelicateCoroutinesApi
class WiFiNetworkCallback(newWifiManager: WifiManager, newConnectivityManager: ConnectivityManager, newAudioManager: AudioManager, newNetworkArrayList: ArrayList<String>): NetworkCallback() {

    private val wifiManager = newWifiManager
    private val connectivityManager = newConnectivityManager
    private val audioManager = newAudioManager
    private val tempNetworkArrayList = newNetworkArrayList
    private lateinit var wifiInfo: WifiInfo
    private var tempNetworkName = ""
    private var networkInfo: String = ""
    private var tempInfo: String = ""

    override fun onAvailable(network: Network) {
        super.onAvailable(network)

        GlobalScope.launch(Dispatchers.IO) {
            delay(2500)
            wifiInfo = wifiManager.connectionInfo as WifiInfo
            networkInfo = connectivityManager.activeNetworkInfo.toString()
            if (networkInfo.isNotEmpty()){
                tempInfo = networkInfo.substring(networkInfo.indexOf(" ")+1, networkInfo.indexOf(" ") + 5 )
            }
            if (wifiManager.isWifiEnabled){
                tempNetworkName = wifiInfo.ssid.substring(1, wifiInfo.ssid.length-1)
                if (tempInfo == "WIFI"){
                    if(tempNetworkArrayList.contains(tempNetworkName)){
                        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    }
                    else{
                        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    }
                }
                else if (tempInfo != "WIFI") {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }
            }
        }
    }

    override fun onLost(network: Network) {
        super.onLost(network)

        GlobalScope.launch(Dispatchers.IO) {
            delay(500)
            networkInfo = connectivityManager.activeNetworkInfo.toString()
            if (networkInfo.isNotEmpty()) {
                tempInfo = networkInfo.substring(networkInfo.indexOf(" ")+1, networkInfo.indexOf(" ") + 5)
            }
            if (!wifiManager.isWifiEnabled) {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
            else if (wifiManager.isWifiEnabled){
                if (tempInfo == "null"){
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }
            }
        }
    }
}