package com.example.forestescape.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.forestescape.utils.Cancellable
import com.example.forestescape.utils.PermissionRequester
import com.example.forestescape.utils.State
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import java.lang.Exception

class ArSessionViewModel(application: Application) : AndroidViewModel(application) {
    private val _sessionConfig: MutableLiveData<Config?> = MutableLiveData(null)
    private val _userRequestedInstall: MutableLiveData<Boolean> = MutableLiveData(true)
    val userRequestedInstall: LiveData<Boolean> = _userRequestedInstall
    private val _session: MutableLiveData<Session?> = MutableLiveData(null)
    val session: LiveData<Session?> = _session
    val userDeclinedInstallation: MutableLiveData<Boolean> = MutableLiveData(false)
    val requestInstall: MutableLiveData<Boolean> = MutableLiveData(false)


    fun requestSessionAndInstall(installStatus: ArCoreApk.InstallStatus) {
        try {
            if (_session.value == null) {
                when (installStatus) {
                    ArCoreApk.InstallStatus.INSTALLED -> {
                        _session.value = Session(getApplication())
                    }
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        _userRequestedInstall.value = false
                        return
                    }
                }
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            Toast.makeText(getApplication(), "TODO: handle exception " + e, Toast.LENGTH_LONG)
                .show()
            userDeclinedInstallation.value = true
            return
        } catch (e: Exception) {
            return
        }
    }

    fun configureSession() {
        _session.value?.configure(_sessionConfig.value)
    }

    fun getConfig(installStatus: ArCoreApk.InstallStatus): LiveData<Config?> {
        if (_sessionConfig.value == null) {
            if (_session.value == null) {
                requestSessionAndInstall(installStatus)
            }
            _sessionConfig.value = Config(_session.value)

        }
        return _sessionConfig
    }

    fun closeSession() {
        _session.value?.close()
    }

    override fun onCleared() {
        super.onCleared()
        closeSession()
    }
}