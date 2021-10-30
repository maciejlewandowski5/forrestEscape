package com.example.forestescape

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData


open class GameLiveData(
    open val id: LiveData<String?>,
    open val gameStarted: LiveData<String>,
    open val currentGame: LiveData<CurrentGame>,
    open val deviceId: LiveData<String?>,
    open val isError: LiveData<Boolean>,
    open val light: LiveData<Float>,
    open val deviceRotationX: LiveData<Float>,
    open val deviceRotationY: LiveData<Float>,
    open val deviceRotationZ: LiveData<Float>,
    open val azimuth: LiveData<Float>,
    open val latitude: LiveData<Double?>,
    open val longitude: LiveData<Double?>,
    open val attitude: LiveData<Double?>,
    open val phoneSignalStrength: LiveData<Float>,
    open val wifiSignalStrength: LiveData<Float>,
    open val batteryLevel: LiveData<Int>
) {
    constructor() : this(
        id = MutableLiveData(null),
        gameStarted = MutableLiveData(""),
        currentGame = MutableLiveData(CurrentGame.PASSWORD),
        deviceId = MutableLiveData(null),
        isError = MutableLiveData(false),
        light = MutableLiveData(0f),
        deviceRotationX = MutableLiveData(0f),
        deviceRotationY = MutableLiveData(0f),
        deviceRotationZ = MutableLiveData(0f),
        azimuth = MutableLiveData(0f),
        latitude = MutableLiveData(0.0),
        longitude = MutableLiveData(0.0),
        attitude = MutableLiveData(0.0),
        phoneSignalStrength = MutableLiveData(0f),
        wifiSignalStrength = MutableLiveData(0f),
        batteryLevel = MutableLiveData(0)
    )


}

class GameMutableLiveData(
    override val id: MutableLiveData<String?>,
    override val gameStarted: MutableLiveData<String>,
    override val currentGame: MutableLiveData<CurrentGame>,
    override val deviceId: MutableLiveData<String?>,
    override val isError: MutableLiveData<Boolean>,
    override val light: MutableLiveData<Float>,
    override val deviceRotationX: MutableLiveData<Float>,
    override val deviceRotationY: MutableLiveData<Float>,
    override val deviceRotationZ: MutableLiveData<Float>,
    override val azimuth: MutableLiveData<Float>,
    override val latitude: MutableLiveData<Double?>,
    override val longitude: MutableLiveData<Double?>,
    override val attitude: MutableLiveData<Double?>,
    override val phoneSignalStrength: MutableLiveData<Float>,
    override val wifiSignalStrength: MutableLiveData<Float>,
    override val batteryLevel: MutableLiveData<Int>
) : GameLiveData(
    id = id,
    gameStarted = gameStarted,
    currentGame = currentGame,
    deviceId = deviceId,
    isError = isError,
    light = light,
    deviceRotationX = deviceRotationX,
    deviceRotationY = deviceRotationY,
    deviceRotationZ = deviceRotationZ,
    azimuth = azimuth,
    latitude = latitude,
    longitude = longitude,
    attitude = attitude,
    phoneSignalStrength = phoneSignalStrength,
    wifiSignalStrength = wifiSignalStrength,
    batteryLevel = batteryLevel

)