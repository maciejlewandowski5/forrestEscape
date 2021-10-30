package com.example.forestescape

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NavigatorViewModel(application: Application) : AndroidViewModel(application) {
    fun setCurrentGame(it: CurrentGame?) {
        when (it) {
            CurrentGame.PASSWORD -> {
                val intent: Intent = Intent(getApplication(), MainActivity::class.java)
                getApplication<Application>().startActivity(intent)
            }
            CurrentGame.MAP -> {
                val intent: Intent = Intent(getApplication(), MainActivity::class.java)
                getApplication<Application>().startActivity(intent)
            }
            CurrentGame.CHARGE -> {
                val intent: Intent = Intent(getApplication(), MainActivity::class.java)
                getApplication<Application>().startActivity(intent)
            }
            CurrentGame.SCAN -> {
                val intent: Intent = Intent(getApplication(), MainActivity::class.java)
                getApplication<Application>().startActivity(intent)
            }
        }
    }

    private val _currentGame: MutableLiveData<CurrentGame?> = MutableLiveData(null)
    val currentGame: LiveData<CurrentGame?> = _currentGame
}