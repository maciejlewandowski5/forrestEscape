package com.example.forestescape

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.IllegalStateException

interface GameRepository {

    suspend fun initializeGameSession(deviceId: String): Either<Exception, String>

    suspend fun sendGameSession(mapToUpdate: Map<String, Double>, game: GameMutableLiveData)

    @ExperimentalCoroutinesApi
    fun currentGameAsFlow(key: String): Flow<CurrentGame?>
}

class FireBaseGameRepository : GameRepository {
    private val database = Firebase.database

    override suspend fun initializeGameSession(deviceId: String): Either<Exception, String> {
        return database.getReference("games").push().key?.let { key ->
            database.getReference("games").child(key).setValue(Game(key, deviceId)).await()
            key.right()
        } ?: IllegalStateException("Could not initialize game session").left()
    }


    @ExperimentalCoroutinesApi
    override fun currentGameAsFlow(key: String): Flow<CurrentGame?> = callbackFlow {
        val callback = (object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySendBlocking(snapshot.getValue(CurrentGame::class.java)).onFailure { _ ->
                    // Downstream has been cancelled or failed, can log here
                }
            }

            override fun onCancelled(error: DatabaseError) {
                cancel(CancellationException("API Error", error.toException()))
            }

        })
        val addValueEventListener = database.getReference("games").child(key)
            .child("currentGame")
            .addValueEventListener(callback)
        awaitClose { addValueEventListener.onCancelled(DatabaseError.fromException(Error("asd"))) }
    }

    override suspend fun sendGameSession(
        mapToUpdate: Map<String, Double>,
        game: GameMutableLiveData,
    ) {
        game.id.value?.let {
            database.getReference("games").child(it)
                .updateChildren(mapToUpdate).await()
        }
    }
}



