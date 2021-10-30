package com.example.forestescape

import android.location.Location
import androidx.lifecycle.*

import kotlinx.coroutines.ExperimentalCoroutinesApi

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class GameViewModel(private val repository: GameRepository) : ViewModel() {
    private var _gameModel: GameMutableLiveData? = null
    val gameModel: GameLiveData? = _gameModel

    @ExperimentalCoroutinesApi
    fun initializeGameSession(deviceId: String) {
        viewModelScope.launch {
            if (_gameModel == null) {
                repository.initializeGameSession(deviceId)
                    .fold({ // TODO do not do anything if collect isnt send
                    }, { key ->
                        _gameModel = GameMutableLiveData(key, deviceId)
                        repository.currentGameAsFlow(key)
                            .collect { _gameModel?.currentGame?.value = it }
                    })
            }
        }
    }

    fun setSensorsData(sensorsData: SensorsData) {
        _gameModel?.light?.value = sensorsData.light
        _gameModel?.azimuth?.value = sensorsData.azimuth
        _gameModel?.deviceRotationX?.value = sensorsData.deviceRotationX
        _gameModel?.deviceRotationY?.value = sensorsData.deviceRotationY
        _gameModel?.deviceRotationZ?.value = sensorsData.deviceRotationZ
        sendGameSession(
            mapOf(
                "light" to sensorsData.light.toDouble(),
                "azimuth" to sensorsData.azimuth.toDouble(),
                "deviceRotationX" to sensorsData.deviceRotationX.toDouble(),
                "deviceRotationY" to sensorsData.deviceRotationY.toDouble(),
                "deviceRotationZ" to sensorsData.deviceRotationZ.toDouble(),
            )
        )
    }

    fun setBatteryLevel(value: Int) {
        _gameModel?.batteryLevel?.value = value
        sendGameSession(mapOf("batteryLevel" to value.toDouble()))
    }

    private fun sendGameSession(mapToUpdate: Map<String, Double>) {
        if (_gameModel != null) {
            viewModelScope.launch {
                repository.sendGameSession(mapToUpdate, _gameModel!!)
            }
        }
    }

    fun setLocation(value: Location) {
        _gameModel?.latitude?.value = value.latitude
        _gameModel?.longitude?.value = value.longitude
        _gameModel?.attitude?.value = value.altitude
        sendGameSession(
            mapOf(
                "latitude" to value.latitude,
                "longitude" to value.longitude,
                "altitude" to value.altitude
            )
        )
    }
}

class GameViewModelFactory(private val repository: GameRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST") // TODO
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return GameViewModel(repository) as T;
    }

}
