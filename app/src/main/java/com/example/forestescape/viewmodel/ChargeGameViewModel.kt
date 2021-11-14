package com.example.forestescape.viewmodel

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*

class ChargeGameViewModel : ViewModel() {
    private var _isCharging = MutableLiveData(false)
    var isCharging: LiveData<Boolean> = _isCharging
    private val _charge: MutableLiveData<Long> = MutableLiveData(0)
    val charge: LiveData<Long> = _charge

    fun startCharge() {
        if (!_isCharging.value!!) {
            _isCharging.value = true
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post(object : Runnable {
                override fun run() {
                    _charge.value?.let {
                        if (it < 100) {
                            _charge.value = it + 1
                            if (_isCharging.value == true) {
                                mainHandler.postDelayed(
                                    this,
                                    CHARGE_ONE_PERCENT_PER_MILLISECONDS
                                )
                            }
                        }
                    }
                }
            })
        }
    }

    fun stopCharge() {
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.postDelayed({ _isCharging.value = false },
            CHARGE_ONE_PERCENT_PER_MILLISECONDS / 2)
    }

    companion object {
        private const val CHARGE_ONE_PERCENT_PER_MILLISECONDS = 1000L
    }
}

