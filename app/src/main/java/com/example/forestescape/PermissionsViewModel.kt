package com.example.forestescape

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.example.forestescape.utils.Cancellable
import com.example.forestescape.utils.PermissionRequester
import com.example.forestescape.utils.State

class PermissionsViewModel(application: Application) : AndroidViewModel(application) {
    private val cancelRequest: Cancellable = requestPermission()
    private val canceled: MutableLiveData<Int> = MutableLiveData(0);

    fun requestPermission(): Cancellable {
        return PermissionRequester.requestPermissions(
            getApplication(),
            "android.permission.CAMERA"
        ) {
            if (it.firstOrNull()?.state == State.GRANTED) {
                Toast.makeText(getApplication(), "GRANTED", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(getApplication(), "DENIED", Toast.LENGTH_LONG).show()
                canceled.value = canceled.value?.inc()
            }
        }
    }

    fun getCanceled(): LiveData<Int> {
        return canceled
    }

    fun hasCameraPermission(activity: Activity?): Boolean {
        return (ContextCompat.checkSelfPermission(
            activity!!,
            "android.permission.CAMERA",
        ) == PackageManager.PERMISSION_GRANTED)
    }

    override fun onCleared() {
        super.onCleared()
        cancelRequest()
    }

}
