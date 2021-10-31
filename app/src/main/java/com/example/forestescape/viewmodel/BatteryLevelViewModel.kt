package com.example.forestescape.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData


class BatteryLevelViewModel(application: Application) : AndroidViewModel(application) {
    val batteryLevel = BatteryLevelLiveData()

    inner class BatteryLevelLiveData : LiveData<Int>() {
        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val rawLevel = intent.getIntExtra("level", -1)
                val scale = intent.getIntExtra("scale", -1)
                var level = -1
                if (rawLevel >= 0 && scale > 0) {
                    level = rawLevel * 100 / scale
                }

                postValue(level)
            }
        }

        override fun onActive() {
            getApplication<Application>().registerReceiver(
                receiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
        }

        override fun onInactive() {
            getApplication<Application>().unregisterReceiver(receiver)
        }
    }
}
