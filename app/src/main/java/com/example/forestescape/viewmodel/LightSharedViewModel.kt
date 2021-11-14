package com.example.forestescape.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LightSharedViewModel : ViewModel() {
    private val _light: MutableLiveData<Float> = MutableLiveData(0f)
    val light: LiveData<Float> = _light

    fun setLight(value: Float) {
        _light.value = value
    }
}