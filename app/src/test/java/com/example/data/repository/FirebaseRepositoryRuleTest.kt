package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.firebase.FirebaseRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FirebaseRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun syncUserProfile_authenticatedUser_succeeds() = runBlocking {
    val email = "alice_${System.nanoTime()}@test.com"
    val uid = signInTestUser(email)
    val repo = FirebaseRepository(firestore)

    withTimeout(5000L) {
      repo.syncUserProfile(uid, "Alice", email, null)
    }

    val snapshot = firestore.collection("users").document(uid).get().await()
    assertTrue(snapshot.exists())
    assertEquals("Alice", snapshot.getString("displayName"))
  }

  @Test
  fun addAndObserveFavorite_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser("alice_${System.nanoTime()}@test.com")
    val repo = FirebaseRepository(firestore)
    val songId = "song_${System.currentTimeMillis()}"

    withTimeout(5000L) {
      repo.addFavorite(uid, songId, "الكوكب")
    }

    val favs = withTimeout(5000L) {
      repo.observeFavorites(uid).first { it.isNotEmpty() }
    }
    assertTrue(favs.any { it.songId == songId })
  }

  @Test
  fun observeFavorites_crossUser_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser("alice_${System.nanoTime()}@test.com")
    val aliceRepo = FirebaseRepository(firestore)
    val songId = "song_${System.currentTimeMillis()}"

    withTimeout(5000L) {
      aliceRepo.addFavorite(aliceUid, songId, "وعد مني")
    }

    // Switch to Bob
    auth.signOut()
    signInTestUser("bob_${System.nanoTime()}@test.com")
    val bobRepo = FirebaseRepository(firestore)

    try {
      withTimeout(5000L) {
        bobRepo.observeFavorites(aliceUid).first()
      }
      fail("Expected permission denied exception when Bob reads Alice's favorites")
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      assertNotNull("Expected FirebaseFirestoreException", firestoreException)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }
  }

  @Test
  fun observeFavorites_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = FirebaseRepository(firestore)

    try {
      withTimeout(5000L) {
        repo.observeFavorites("some_uid").first()
      }
      fail("Expected exception for unauthenticated read")
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      assertNotNull("Expected FirebaseFirestoreException", firestoreException)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }
  }
}
