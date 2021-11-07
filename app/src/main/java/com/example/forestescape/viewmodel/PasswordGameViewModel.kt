package com.example.forestescape.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PasswordGameViewModel : ViewModel() {
    private val _password: MutableLiveData<MutableList<Int>> =
        MutableLiveData(emptyList<Int>().toMutableList())
    val password: LiveData<MutableList<Int>> = _password

    private val _inputIsCorrect: MutableLiveData<Boolean> = MutableLiveData(false)
    val isInputCorrect: LiveData<Boolean> = _inputIsCorrect

    private val correctPassword = listOf(8, 5, 6, 2, 3, 4, 7)

    fun addLetter(value: Int) {
        addValueNotifyingLiveData(value)
        _password.value?.forEachIndexed { index, i ->
            if (validateInput(index, i)) return
        }
        checkIfCorrectElseReset()
    }

    private fun addValueNotifyingLiveData(value: Int) {
        val copy = _password.value?.toMutableList()
        copy?.add(value)
        _password.value = copy
    }

    private fun checkIfCorrectElseReset() {
        if (_password.value?.size == correctPassword.size) {
            _inputIsCorrect.value = true
        } else {
            if (_inputIsCorrect.value == true) {
                _inputIsCorrect.value = false
            }
        }
    }

    private fun validateInput(index: Int, i: Int): Boolean {
        if (correctPassword.size > index) {
            if (checkIfInputIsIncorrect(i, index)) return true
        } else {
            resetPasswordState()
            return true
        }
        return false
    }

    private fun checkIfInputIsIncorrect(i: Int, index: Int): Boolean {
        if (i != correctPassword[index]) {
            resetPasswordState()
            return true
        }
        return false
    }

    private fun resetPasswordState() {
        _password.value = emptyList<Int>().toMutableList()
        _inputIsCorrect.value = false
    }

}