package com.example.forestescape.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.forestescape.model.CurrentGame

class CurrentGameSharedViewModel : ViewModel() {
    val currentGame: MutableLiveData<CurrentGame> = MutableLiveData(null)
}