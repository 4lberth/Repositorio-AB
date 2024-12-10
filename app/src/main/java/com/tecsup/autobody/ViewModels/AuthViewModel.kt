package com.tecsup.autobody.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tecsup.autobody.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(private val authRepository: AuthRepository = AuthRepository()) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _vehicles = MutableStateFlow<List<Map<String, String>>>(emptyList())
    val vehicles: StateFlow<List<Map<String, String>>> = _vehicles

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Registra un usuario y guarda datos en Firestore utilizando el repositorio.
     */
    fun registerUser(
        name: String,
        dni: String,
        address: String,
        phone: String,
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Registro de usuario
                val userIdResult = authRepository.registerUser(email, password)
                if (userIdResult.isSuccess) {
                    val userId = userIdResult.getOrNull() ?: throw Exception("UID no obtenido")
                    val userData = mapOf(
                        "name" to name,
                        "dni" to dni,
                        "address" to address,
                        "phone" to phone,
                        "email" to email
                    )
                    // Guardar datos en Firestore
                    val saveResult = authRepository.saveUserToFirestore(userId, userData)
                    if (saveResult.isSuccess) {
                        _authState.value = AuthState.Success
                    } else {
                        throw Exception(saveResult.exceptionOrNull()?.message ?: "Error desconocido al guardar datos")
                    }
                } else {
                    throw Exception(userIdResult.exceptionOrNull()?.message ?: "Error desconocido en registro")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Inicia sesión con un usuario.
     */
    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = authRepository.loginUser(email, password)
                if (result.isSuccess) {
                    _authState.value = AuthState.Success
                } else {
                    throw Exception(result.exceptionOrNull()?.message ?: "Error al iniciar sesión")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Cierra la sesión del usuario.
     */
    fun logoutUser() {
        viewModelScope.launch {
            authRepository.logoutUser()
            _authState.value = AuthState.LoggedOut
        }
    }

    /**
     * Recupera el nombre del usuario desde Firestore.
     */
    fun getUserName(userId: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val document = firestore.collection("users").document(userId).get().await()
                val name = document.getString("name") ?: "Usuario"
                onSuccess(name)
            } catch (e: Exception) {
                onFailure("Error al recuperar el nombre del usuario")
            }
        }
    }

    /**
     * Agrega un vehículo asociado al cliente actual.
     */
    fun addVehicle(userId: String, vehicleData: Map<String, String>) {
        viewModelScope.launch {
            try {
                val userVehiclesRef = firestore.collection("users").document(userId).collection("vehicles")
                userVehiclesRef.add(vehicleData).await()
                fetchVehicles(userId) // Actualizar lista de vehículos después de agregar
            } catch (e: Exception) {
                // Maneja el error aquí
            }
        }
    }

    /**
     * Recupera la lista de vehículos del cliente desde Firestore.
     */
    fun fetchVehicles(userId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(userId).collection("vehicles").get().await()
                val vehicleList = snapshot.documents.mapNotNull { it.data as? Map<String, String> }
                _vehicles.value = vehicleList
            } catch (e: Exception) {
                _vehicles.value = emptyList() // Manejo de errores devolviendo una lista vacía
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object LoggedOut : AuthState()
    data class Error(val message: String) : AuthState()
}
