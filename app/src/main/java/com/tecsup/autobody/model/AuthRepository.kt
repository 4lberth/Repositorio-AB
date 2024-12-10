package com.tecsup.autobody.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Registra un nuevo usuario en Firebase Authentication.
     */
    suspend fun registerUser(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid ?: throw Exception("No se pudo obtener el UID del usuario"))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error en registerUser: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Guarda los datos adicionales del usuario en Firestore.
     */
    suspend fun saveUserToFirestore(userId: String, userData: Map<String, String>): Result<Unit> {
        return try {
            firestore.collection("users").document(userId).set(userData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error en saveUserToFirestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Inicia sesión con un usuario existente en Firebase Authentication.
     */
    suspend fun loginUser(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error en loginUser: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Cierra la sesión del usuario actualmente autenticado.
     */
    fun logoutUser() {
        auth.signOut()
    }

    /**
     * Obtiene el estado actual del usuario autenticado.
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}
