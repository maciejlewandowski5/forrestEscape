package com.example.forestescape.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.firestore.DocumentId
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@IgnoreExtraProperties
data class Game(
    @DocumentId
    var id: String?,
    var gameStarted: String,
    var deviceId: String?,
    var isError: Boolean,
    var light: Float,
    var currentGame: CurrentGame,
    var deviceRotationX: Float,
    var deviceRotationY: Float,
    var deviceRotationZ: Float,
    var azimuth: Float,
    var latitude: Double?,
    var longitude: Double?,
    var attitude: Double?,
    var phoneSignalStrength: Float,
    var wifiSignalStrength: Float,
    var batteryLevel: Int
) {
    constructor() : this(
        id = null,
        gameStarted = LocalDateTime.now().format(dateFormatter),
        deviceId = null,
        isError = false,
        light = 0f,
        currentGame = startingGame,
        deviceRotationX = 0f,
        deviceRotationY = 0f,
        deviceRotationZ = 0f,
        azimuth = 0f,
        latitude = null,
        longitude = null,
        attitude = null,
        phoneSignalStrength = 0f,
        wifiSignalStrength = 0f,
        batteryLevel = 0
    )

    constructor(gameId: String, deviceId: String) : this(
        id = gameId,
        gameStarted = LocalDateTime.now().format(dateFormatter),
        deviceId = deviceId,
        isError = false,
        light = 0f,
        currentGame = startingGame,
        deviceRotationX = 0f,
        deviceRotationY = 0f,
        deviceRotationZ = 0f,
        azimuth = 0f,
        latitude = null,
        longitude = null,
        attitude = null,
        phoneSignalStrength = 0f,
        wifiSignalStrength = 0f,
        batteryLevel = 0
    )
    companion object {
        private val dateFormatter = DateTimeFormatter.ISO_DATE_TIME
        private val startingGame = CurrentGame.NO_GAME
    }
}

enum class CurrentGame {
    MAP, CHARGE, PASSWORD, SCAN, NO_GAME
}

