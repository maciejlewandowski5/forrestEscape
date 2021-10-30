package com.example.forestescape

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel

import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.LiveData
import android.net.wifi.WifiManager

import android.net.wifi.WifiInfo
import android.net.NetworkInfo
import androidx.core.content.ContextCompat

import androidx.core.content.ContextCompat.getSystemService


class InternetConnectionViewModel(application: Application) : AndroidViewModel(application) {
    fun isOnline(): Boolean {
        val connectivityManager =
            getApplication<Application>()
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    }
}