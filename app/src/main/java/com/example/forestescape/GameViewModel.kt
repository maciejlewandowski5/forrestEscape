package com.example.forestescape

import android.location.Location
import androidx.lifecycle.*

import kotlinx.coroutines.ExperimentalCoroutinesApi

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class GameViewModel(private val repository: GameRepository) : ViewModel() {
    private val _gameModel: MutableLiveData<Game?> = MutableLiveData(null)
    val gameModel: LiveData<Game?> = _gameModel

    @ExperimentalCoroutinesApi
    fun initializeGameSession(deviceId: String) {
        viewModelScope.launch {
            if (_gameModel.value == null) {
                repository.initializeGameSession(deviceId)
                    .fold({ // TODO do not do anything if collect isnt send
                    }, { key ->
                        repository.asFlow(key, deviceId).collect { _gameModel.value = it }
                    })
            }
        }
    }

    fun setSensorsData(sensorsData: SensorsData) {
        _gameModel.value?.light = sensorsData.light
        _gameModel.value?.azimuth = sensorsData.azimuth
        _gameModel.value?.deviceRotationX = sensorsData.deviceRotationX
        _gameModel.value?.deviceRotationY = sensorsData.deviceRotationY
        _gameModel.value?.deviceRotationZ = sensorsData.deviceRotationZ
        sendGameSession()
    }

    fun setBatteryLevel(value: Int) {
        _gameModel.value?.batteryLevel = value
        sendGameSession()
    }

    private fun sendGameSession() {
        viewModelScope.launch {
            repository.sendGameSession(_gameModel.value)
        }
    }

    fun setLocation(value: Location) {
        _gameModel.value?.latitude = value.latitude
        _gameModel.value?.longitude = value.longitude
        _gameModel.value?.attitude = value.altitude
    }
}

class GameViewModelFactory(private val repository: GameRepository) : ViewModelProvider.Factory {


    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return GameViewModel(repository) as T;
    }

}
