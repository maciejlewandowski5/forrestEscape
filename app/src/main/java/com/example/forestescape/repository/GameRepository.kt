package com.example.forestescape.repository

import android.util.Log
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.forestescape.model.CurrentGame
import com.example.forestescape.model.Game
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.IllegalStateException

interface GameRepository {
    @ExperimentalCoroutinesApi
    fun currentGame(key: String): Flow<CurrentGame>
    suspend fun initializeGameSession(deviceId: String): Either<Exception, String>
    suspend fun sendGameSession(mapToUpdate: Map<String, Double>, game: Game?)
}

class FireBaseGameRepository : GameRepository {
    private val database = Firebase.database

    override suspend fun initializeGameSession(deviceId: String): Either<Exception, String> {
        return database.getReference(GAMES).push().key?.let { gameId ->
            database.getReference(GAMES).child(gameId).setValue(Game(gameId, deviceId)).await()
            gameId.right()
        } ?: IllegalStateException("Could not initialize game session").left()
    }

    @ExperimentalCoroutinesApi
    override fun currentGame(gameId: String): Flow<CurrentGame> = callbackFlow {
        val callback = (
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    sendNonNullValue(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    cancel(CancellationException("API Error", error.toException()))
                }
            }
            )
        val addValueEventListener = valueEventListener(gameId, callback)
        awaitClose {
            addValueEventListener.onCancelled(
                DatabaseError.fromException(Error("Canceled firebase connection"))
            )
        }
    }

    private fun valueEventListener(gameId: String, callback: ValueEventListener) = database
        .getReference(GAMES)
        .child(gameId)
        .child(CURRENT_GAME)
        .addValueEventListener(callback)

    @ExperimentalCoroutinesApi
    private fun ProducerScope<CurrentGame>.sendNonNullValue(snapshot: DataSnapshot) {
        snapshot.getValue(CurrentGame::class.java)
            ?.let { trySendBlocking(it).onFailure(logOnFailure()) }
    }

    private fun logOnFailure(): (exception: Throwable?) -> Unit = { a ->
        Log.e(FireBaseGameRepository::class.java.name, "Error sending fetched currentGame", a)
    }

    override suspend fun sendGameSession(
        mapToUpdate: Map<String, Double>,
        game: Game?,
    ) {
        game?.id?.let { database.getReference(GAMES).child(it).updateChildren(mapToUpdate).await() }
    }

    companion object {
        private const val GAMES = "games"
        private const val CURRENT_GAME = "currentGame"
    }
}
