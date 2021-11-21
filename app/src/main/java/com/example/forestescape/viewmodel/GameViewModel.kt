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
    private var previousUpdate: Long = System.currentTimeMillis()

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
            it.light = if (sensorsData.light.isFinite()) {
                sensorsData.light
            } else {
                0f
            }
            it.azimuth = if (sensorsData.azimuth.isFinite()) {
                sensorsData.azimuth.toFloat()
            } else {
                0f
            }
            it.deviceRotationX = if (sensorsData.deviceRotationX.isFinite()) {
                sensorsData.deviceRotationX
            } else {
                0f
            }
            it.deviceRotationZ = if (sensorsData.deviceRotationZ.isFinite()) {
                sensorsData.deviceRotationZ
            } else {
                0f
            }
            it.deviceRotationY = if (sensorsData.deviceRotationY.isFinite()) {
                sensorsData.deviceRotationY
            } else {
                0f
            }
            _gameModel.postSuccess(it)
            if (System.currentTimeMillis() - previousUpdate > 1000) {
                previousUpdate = System.currentTimeMillis()
                sendGameSession(
                    mapOf(
                        "light" to it.light.toDouble(),
                        "azimuth" to it.azimuth.toDouble(),
                        "deviceRotationX" to it.deviceRotationX.toDouble(),
                        "deviceRotationY" to it.deviceRotationY.toDouble(),
                        "deviceRotationZ" to it.deviceRotationZ.toDouble(),
                    )
                )
            }
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
