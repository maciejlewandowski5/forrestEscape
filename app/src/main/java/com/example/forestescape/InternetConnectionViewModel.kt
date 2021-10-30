package com.example.forestescape

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel

import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.LiveData
import android.net.wifi.WifiManager

import android.net.wifi.WifiInfo





class InternetConnectionViewModel(application: Application) : AndroidViewModel(application) {
    val wifiSignalStrength = WifiSignalStrengthLiveData()

    inner class WifiSignalStrengthLiveData : LiveData<Int>() {
        private val connectivityManager: ConnectivityManager by lazy {
            getApplication<Application>()
                .getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        }

        private val callbacks = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    postValue(networkCapabilities.signalStrength)
                }
            }
        }

        override fun onActive() {
            val networkRequest: NetworkRequest =
                NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()
            connectivityManager.registerNetworkCallback(networkRequest, callbacks)


        }

        override fun onInactive() {
            connectivityManager.unregisterNetworkCallback(callbacks)
        }
    }

}