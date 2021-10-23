package com.example.forestescape

import androidx.lifecycle.*

import kotlinx.coroutines.ExperimentalCoroutinesApi

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class GameViewModel(private val repository: GameRepository) : ViewModel() {
    private val _gameSession: MutableLiveData<GameSession?> = MutableLiveData(null)
    val gameSession: LiveData<GameSession?> = _gameSession


    @ExperimentalCoroutinesApi
    fun initializeGameSession(deviceId: String) {
        viewModelScope.launch {
            if (_gameSession.value == null) {
                repository.initializeGameSession(deviceId)
                    .fold({ // TODO do not do anything if collect isnt send
                    }, { key ->
                        repository.asFlow(key,deviceId).collect { _gameSession.value = it }
                    })
            }
        }
    }
}

class GameViewModelFactory(private val repository: GameRepository) : ViewModelProvider.Factory {


    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return GameViewModel(repository) as T;
    }

}