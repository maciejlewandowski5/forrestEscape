package com.example.forestescape.viewmodel

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MapSharedViewModel : ViewModel() {
    private val _location: MutableLiveData<Location> = MutableLiveData(null)
    val location: LiveData<Location> = _location

    private val _azimuth: MutableLiveData<Float> = MutableLiveData(null)
    val azimuth: LiveData<Float> = _azimuth

    fun setLocation(location: Location) {
        _location.value = location
    }

    fun setAzimuth(angle: Float) {
        _azimuth.value = angle
    }
}