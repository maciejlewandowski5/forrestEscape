package com.example.forestescape.model

import androidx.lifecycle.MutableLiveData


open class MutableStateLiveData<T> : MutableLiveData<State<T>>() {
    fun postLoading() {
        postValue(State.Loading())
    }

    fun postError(error: Throwable) {
        postValue(State.Error(error))
    }

    fun postSuccess(dataValue: T) {
        postValue(State.Success(dataValue))
    }
}

open class State<T>(
    val dataStatus: DataStatus,
    val dataValue: T? = null,
    var error: Throwable? = null,
) {

    class Loading<T>() : State<T>(dataStatus = DataStatus.LOADING, null, null)
    class Success<T>(data: T) : State<T>(dataStatus = DataStatus.SUCCESS, data, null)
    class Error<T>(error: Throwable) : State<T>(dataStatus = DataStatus.ERROR, null, error)

    enum class DataStatus {
        LOADING, ERROR, SUCCESS
    }
}