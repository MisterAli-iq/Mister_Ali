package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun syncUserProfile(userId: String, displayName: String, email: String, photoUrl: String?) {
        val path = "users/$userId"
        val docRef = db.collection("users").document(userId)
        try {
            val existingSnapshot = docRef.get().await()
            if (!existingSnapshot.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "userId" to userId,
                    "displayName" to displayName,
                    "email" to email,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                if (!photoUrl.isNullOrBlank()) {
                    payload["photoUrl"] = photoUrl
                }
                docRef.set(payload).await()
            } else {
                val updateMap = mutableMapOf<String, Any>(
                    "displayName" to displayName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!photoUrl.isNullOrBlank()) {
                    updateMap["photoUrl"] = photoUrl
                }
                docRef.update(updateMap).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    fun observeFavorites(userId: String): Flow<List<UserFavorite>> = flow {
        val path = "users/$userId/favorites"
        val query = db.collection("users").document(userId).collection("favorites")
        query.snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(UserFavorite::class.java) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun addFavorite(userId: String, songId: String, songTitle: String) {
        val path = "users/$userId/favorites/$songId"
        val docRef = db.collection("users").document(userId).collection("favorites").document(songId)
        val payload = mapOf(
            "userId" to userId,
            "songId" to songId,
            "songTitle" to songTitle,
            "addedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun removeFavorite(userId: String, songId: String) {
        val path = "users/$userId/favorites/$songId"
        val docRef = db.collection("users").document(userId).collection("favorites").document(songId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }
}
