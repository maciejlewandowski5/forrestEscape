package com.example.forestescape

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
    var currentGame: CurrentGame,
    var deviceId: String?,
    var isError: Boolean,
    var light: Float,
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
        null,
        LocalDateTime.now().format(dateFormatter),
        startingGame,
        null,
        false,
        0f,
        0f,
        0f,
        0f,
        0f,
        null,
        null,
        null,
        0f,
        0f,
        0
    )

    constructor(key: String, deviceId: String) : this(
        key,
        LocalDateTime.now().format(dateFormatter),
        startingGame,
        deviceId,
        false,
        0f,
        0f,
        0f,
        0f,
        0f,
        null,
        null,
        null,
        0f,
        0f,
        0
    )

    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "gameStarted" to gameStarted,
            "currentGame" to currentGame,
            "deviceId" to deviceId,
            "isError" to isError,
            "light" to light,
            "deviceRotationX" to deviceRotationX,
            "deviceRotationY" to deviceRotationY,
            "deviceRotationZ" to deviceRotationZ,
        )
    }

    companion object {
        private val dateFormatter = DateTimeFormatter.ISO_DATE_TIME
        private val startingGame = CurrentGame.PASSWORD
    }
}

enum class CurrentGame {
    MAP, CHARGE, PASSWORD, SCAN
}

sealed class State<T> {
    class Loading<T> : State<T>()
    data class Success<T>(val data: T?) : State<T?>()
    data class Failed<T>(val message: String) : State<T>()

    companion object {
        fun <T> loading() = Loading<T>()
        fun <T> success(data: T?) = Success(data)
        fun <T> failed(message: String) = Failed<T>(message)
    }
}
