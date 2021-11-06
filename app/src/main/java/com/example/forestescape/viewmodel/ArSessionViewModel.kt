package com.example.forestescape.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.ar.core.*
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import java.io.IOException
import java.lang.Exception
import java.util.*

class ArSessionViewModel(application: Application) : AndroidViewModel(application) {
    private val _sessionConfig: MutableLiveData<Config?> = MutableLiveData(null)
    private val _userRequestedInstall: MutableLiveData<Boolean> = MutableLiveData(true)
    val userRequestedInstall: LiveData<Boolean> = _userRequestedInstall
    private val _session: MutableLiveData<Session?> = MutableLiveData(null)
    val session: LiveData<Session?> = _session
    private val _sharedCamera: MutableLiveData<SharedCamera?> = MutableLiveData(null)
    val sharedCamera: LiveData<SharedCamera?> = _sharedCamera
    private val _cameraId: MutableLiveData<String?> = MutableLiveData(null)
    val cameraId: LiveData<String?> = _cameraId
    val userDeclinedInstallation: MutableLiveData<Boolean> = MutableLiveData(false)
    val requestInstall: MutableLiveData<Boolean> = MutableLiveData(false)

    private val useSingleImage = false

    fun requestSessionAndInstall(installStatus: ArCoreApk.InstallStatus) {
        try {
            if (_session.value == null) {
                when (installStatus) {
                    ArCoreApk.InstallStatus.INSTALLED -> {
                        _session.value =
                            Session(getApplication(), EnumSet.of(Session.Feature.SHARED_CAMERA))
                        _sharedCamera.value = _session.value!!.sharedCamera
                        _cameraId.value = _session.value!!.cameraConfig.cameraId
                        getConfig(installStatus)
                        configureSession()
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
        _sessionConfig.value?.let {
            it.focusMode = Config.FocusMode.AUTO
            if (!setupAugmentedImageDatabase(it)) {
                Log.e("ArSessionViewModel", "Could not setup augmented image database")
            }
            _session.value?.configure(it)
        }
    }

    private fun setupAugmentedImageDatabase(config: Config): Boolean {
        var augmentedImageDatabase: AugmentedImageDatabase

        if (useSingleImage) {
            val augmentedImageBitmap: Bitmap = loadAugmentedImageBitmap() ?: return false
            augmentedImageDatabase = AugmentedImageDatabase(_session.value)
            augmentedImageDatabase.addImage("image_name", augmentedImageBitmap)
        } else {
            try {
                getApplication<Application>()
                    .assets.open("sample_database.imgdb").use {
                        augmentedImageDatabase =
                            AugmentedImageDatabase.deserialize(_session.value, it)
                    }
            } catch (e: IOException) {
                Log.e(
                    "ArSessionViewModel",
                    "IO exception loading augmented image database.",
                    e
                )
                return false
            }
        }
        config.augmentedImageDatabase = augmentedImageDatabase
        return true
    }

    private fun loadAugmentedImageBitmap(): Bitmap? {
        try {
            getApplication<Application>()
                .assets.open("default.jpg")
                .use { `is` -> return BitmapFactory.decodeStream(`is`) }
        } catch (e: IOException) {
            Log.e(
                "ArSessionViewModel",
                "IO exception loading augmented image bitmap.",
                e
            )
        }
        return null
    }

    private fun getConfig(installStatus: ArCoreApk.InstallStatus): LiveData<Config?> {
        if (_sessionConfig.value == null) {
            if (_session.value == null) {
                requestSessionAndInstall(installStatus)
            }
            _sessionConfig.value = Config(_session.value)

        }
        return _sessionConfig
    }

    private fun closeSession() {
        _session.value?.close()
    }

    override fun onCleared() {
        super.onCleared()
        closeSession()
    }


}