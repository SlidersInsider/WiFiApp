package com.mzhadan.wifiapp

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.mzhadan.wifiapp.databinding.ActivityMainBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import java.io.*
import com.mzhadan.wifiapp.Constants.Companion.INFO_FILE_NAME
import com.mzhadan.wifiapp.Constants.Companion.networkArrayList

@DelicateCoroutinesApi
@RequiresApi(Build.VERSION_CODES.R)
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var networkInfoFile: File
    private lateinit var audioManager: AudioManager
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiInfo: WifiInfo
    private lateinit var notificationManager: NotificationManager
    private lateinit var sharedPreferences: SharedPreferences
    private var locationPermission = false
    private var silentModePermission = false
    private var serviceWorkingState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        networkInfoFile = File(filesDir, INFO_FILE_NAME)
        if (!networkInfoFile.exists()) {
            networkInfoFile.createNewFile()
        }

        initData()
        checkPermissions()

        val serviceIntent = Intent(this, SilentModeService::class.java)

        binding.mainActivityConfirmWifiNetworkButton.setOnClickListener {
            permissionsConfirmNetwork()
        }

        binding.mainActivityButtonChoose.setOnClickListener {
            turnOnOffWifi()
        }

        binding.mainActivityButtonTurnOnService.setOnClickListener {
            startSilentModeService(serviceIntent)
        }

        binding.mainActivityButtonTurnOffService.setOnClickListener {
            stopSilenceModeService(serviceIntent)
        }
    }

    private fun stopSilenceModeService(serviceIntent: Intent) {
        stopService(serviceIntent)
        serviceWorkingState = false
        commitServiceWorkingState(serviceWorkingState)
        binding.mainActivityButtonTurnOffService.isEnabled = false
        binding.mainActivityButtonTurnOnService.isEnabled = true
    }

    private fun startSilentModeService(serviceIntent: Intent) {
        locationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) > -1
        silentModePermission = notificationManager.isNotificationPolicyAccessGranted

        if(locationPermission && silentModePermission){
            startForegroundService(serviceIntent)
            serviceWorkingState = true
            commitServiceWorkingState(serviceWorkingState)
            binding.mainActivityButtonTurnOnService.isEnabled = false
            binding.mainActivityButtonTurnOffService.isEnabled = true
            binding.mainActivityConfirmWifiNetworkButton.isEnabled = true
            binding.mainActivityWifiNameEditText.isEnabled = true
        }
        else{
            Toast.makeText(this, "Not enough permissions! Restart app and allow necessary permissions!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun commitServiceWorkingState(serviceWorkingState: Boolean) {
        val sharedPreferencesEditor = sharedPreferences.edit()
        sharedPreferencesEditor.apply {
            sharedPreferencesEditor.putBoolean("serviceWorkingState", serviceWorkingState)
            sharedPreferencesEditor.apply()
        }
    }

    private fun commitPermissions(locationPermission: Boolean, silentModePermission: Boolean) {
        val sharedPreferencesEditor = sharedPreferences.edit()
        sharedPreferencesEditor.apply {
            sharedPreferencesEditor.putBoolean("locationPermission", locationPermission)
            sharedPreferencesEditor.putBoolean("silentModePermission", silentModePermission)
            sharedPreferencesEditor.apply()
        }
    }

    private fun permissionsConfirmNetwork() {
        if (networkArrayList.contains(binding.mainActivityWifiNameEditText.text.toString())){
            Toast.makeText(this, "Network already added!", Toast.LENGTH_SHORT).show()
        }
        else{
            val wifiName = binding.mainActivityWifiNameEditText.text.toString()
            Toast.makeText(this, "Network '$wifiName' saved!", Toast.LENGTH_SHORT).show()
            confirmNetwork(wifiName)
            checkTempWiFi()
            binding.mainActivityWifiNameEditText.setText("")
        }
    }

    private fun checkTempWiFi() {
        wifiInfo = wifiManager.connectionInfo as WifiInfo
        val tempNetworkName = wifiInfo.ssid.substring(1, wifiInfo.ssid.length - 1)
        if (networkArrayList.contains(tempNetworkName)) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        }
    }

    private fun confirmNetwork(wifiName: String) {
        if (!networkArrayList.contains(wifiName) && wifiName.isNotEmpty()) {
            networkArrayList.add(wifiName)
            writeInfoFile(wifiName)
        }
    }

    private fun checkPermissions() {
        locationPermission = sharedPreferences.getBoolean("locationPermission", false)
        silentModePermission = sharedPreferences.getBoolean("silentModePermission", false)
        serviceWorkingState = sharedPreferences.getBoolean("serviceWorkingState", false)

        if(!locationPermission && !silentModePermission){
            requestSilentModePermission().show()
            requestLocationPermission().show()
        }
        else if(locationPermission && !silentModePermission){
            requestSilentModePermission().show()
        }
        else if(!locationPermission && silentModePermission){
            requestLocationPermission().show()
        }
        else if(locationPermission && silentModePermission){
            binding.mainActivityConfirmWifiNetworkButton.isEnabled = true
            binding.mainActivityWifiNameEditText.isEnabled = true
        }

        if(serviceWorkingState){
            binding.mainActivityButtonTurnOnService.isEnabled = false
            binding.mainActivityButtonTurnOffService.isEnabled = true
        }
    }

    private fun turnOnOffWifi(){
        val wifiTurnOnOffIntent = Intent(Settings.Panel.ACTION_WIFI)
        startActivity(wifiTurnOnOffIntent)
    }

    private fun initData(){
        val infoFileStr = readInfoFile()
        convertInfoStrToArray(infoFileStr)

        sharedPreferences = getSharedPreferences("appSettings", Context.MODE_PRIVATE)
        notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private fun readInfoFile(): String{
        val fileReader = openFileInput(INFO_FILE_NAME)
        val infoByteArray = ByteArray(fileReader.available())
        fileReader.read(infoByteArray)
        fileReader.close()

        return String(infoByteArray)
    }

    private fun writeInfoFile(newNetwork: String){
        val fileWriter = openFileOutput(INFO_FILE_NAME, MODE_APPEND)
        fileWriter.write("$newNetwork\n".toByteArray())
        fileWriter.close()
    }

    private fun convertInfoStrToArray(infoFileStr: String){
        val tempInfoArray = infoFileStr.split("\n")
        for (i in tempInfoArray.indices){
            networkArrayList.add(tempInfoArray[i])
        }
    }

    private fun requestLocationPermission(): AlertDialog{
        return AlertDialog.Builder(this)
            .setTitle("Location Permission")
            .setMessage("You need to go to location settings then permissions and allow 'Using location all the time' for this app")
            .setPositiveButton("Allow"){_, _ ->
                val locationPermissionIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(locationPermissionIntent)
            }
            .setNegativeButton("Deny"){dialog, _ ->
                dialog.cancel()
            }
            .create()
    }

    private fun requestSilentModePermission(): AlertDialog{
        return AlertDialog.Builder(this)
            .setTitle("Silent Mode Permission")
            .setMessage("You need to go to silent mode settings and allow 'Using silent mode' for this app")
            .setPositiveButton("Allow"){_, _ ->
                val silentModePermissionIntent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivity(silentModePermissionIntent)
            }
            .setNegativeButton("Deny"){dialog, _ ->
                dialog.cancel()
            }
            .create()
    }

    override fun onPause() {
        super.onPause()
        locationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) > -1
        silentModePermission = notificationManager.isNotificationPolicyAccessGranted

        commitPermissions(locationPermission, silentModePermission)
    }
}