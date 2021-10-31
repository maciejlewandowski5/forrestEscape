package com.example.forestescape.viewmodel

import android.location.Location
import androidx.lifecycle.*
import com.example.forestescape.model.CurrentGame
import com.example.forestescape.model.Game
import com.example.forestescape.model.MutableStateLiveData
import com.example.forestescape.model.State
import com.example.forestescape.repository.GameRepository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class GameViewModel(private val repository: GameRepository) : ViewModel() {
    private val _gameModel: MutableStateLiveData<Game> = MutableStateLiveData()
    val gameModel: MutableStateLiveData<Game> = _gameModel

    val currentGame: MutableStateLiveData<CurrentGame> = MutableStateLiveData()

    @ExperimentalCoroutinesApi
    fun initializeGameSession(deviceId: String): Job {
        _gameModel.postLoading()
        currentGame.postLoading()
        return viewModelScope.launch {
            if (_gameModel.value == null) {
                repository.initializeGameSession(deviceId)
                    .fold({ // TODO do not do anything if collect isnt send
                    }, { key ->
                        _gameModel.postSuccess(Game(key, deviceId))
                        collectCurrentGame(key)
                    })
            }
        }
    }

    @ExperimentalCoroutinesApi
    private suspend fun collectCurrentGame(key: String) {

            repository.currentGameAsFlow(key).collect { currentGame.postSuccess(it) }
        //collect {  }
    }

    private fun getGame(): Game? {
        return if (_gameModel.value?.dataStatus == State.DataStatus.SUCCESS) {
            _gameModel.value?.dataValue
        } else {
            null
        }
    }

    fun setSensorsData(sensorsData: SensorsData) {
        getGame()?.also {
            it.light = sensorsData.light
            it.azimuth = sensorsData.azimuth
            it.deviceRotationX = sensorsData.deviceRotationX
            it.deviceRotationY = sensorsData.deviceRotationY
            it.deviceRotationZ = sensorsData.deviceRotationZ
            _gameModel.postSuccess(it)
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
    }

    fun setBatteryLevel(value: Int) {
        getGame()?.also {
            it.batteryLevel = value
            sendGameSession(mapOf("batteryLevel" to value.toDouble()))
        }
    }

    fun setLocation(value: Location) {
        getGame()?.also {
            it.latitude = value.latitude
            it.longitude = value.longitude
            it.attitude = value.altitude
            sendGameSession(
                mapOf(
                    "latitude" to value.latitude,
                    "longitude" to value.longitude,
                    "altitude" to value.altitude
                )
            )
        }
    }

    private fun sendGameSession(mapToUpdate: Map<String, Double>) {
        if (_gameModel.value != null) {
            viewModelScope.launch {
                repository.sendGameSession(mapToUpdate, getGame())
            }
        }
    }

}

class GameViewModelFactory(private val repository: GameRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST") // TODO
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return GameViewModel(repository) as T;
    }

}
