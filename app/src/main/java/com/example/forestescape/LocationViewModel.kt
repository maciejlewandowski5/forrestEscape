package com.example.forestescape

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationRequest.create

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val _isUpdatingLocation: MutableLiveData<Boolean> = MutableLiveData(false)
    val isUpdatingLocation: LiveData<Boolean> = _isUpdatingLocation
    val location = LocationLiveData()

    inner class LocationLiveData : LiveData<Location>() {
        private val fusedLocationClient: FusedLocationProviderClient by lazy {
            LocationServices.getFusedLocationProviderClient(
                getApplication<Application>().applicationContext
            )
        }
        private val locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                if (p0 != null) {
                    postValue(p0.lastLocation)
                }

            }
        }

        override fun onActive() {
            requestCurrentLocation()
        }


        private fun requestCurrentLocation() {
            val locationRequest = create()
            configureLocationRequest(locationRequest)
            requestLocationUpdates(locationRequest)
            _isUpdatingLocation.value = true
        }

        @SuppressLint("MissingPermission") // Checked by PermissionViewModel
        private fun requestLocationUpdates(locationRequest: LocationRequest?) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }

        private fun configureLocationRequest(locationRequest: LocationRequest) {
            locationRequest.interval = LOCATION_REQUEST_INTERVAL_MILLISECONDS
            locationRequest.maxWaitTime = LOCATION_REQUEST_INTERVAL_MILLISECONDS
            locationRequest.expirationTime = Long.MAX_VALUE
            locationRequest.fastestInterval = LOCATION_REQUEST_INTERVAL_MILLISECONDS / 2
            locationRequest.priority = PRIORITY_HIGH_ACCURACY
            locationRequest.smallestDisplacement = LOCATION_SENSITIVITY_IN_METERS
        }

        override fun onInactive() {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            _isUpdatingLocation.value = false
        }
    }

    companion object {
        private const val LOCATION_REQUEST_INTERVAL_MILLISECONDS = 500L
        private const val LOCATION_SENSITIVITY_IN_METERS = 0.5f
    }
}