package com.tecsup.autobody.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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

    private val _companies = MutableStateFlow<List<String>>(emptyList())
    val companies: StateFlow<List<String>> = _companies

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    /**
     * Registrar usuario y guardar información en Firestore.
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
                    val saveResult = authRepository.saveUserToFirestore(userId, userData)
                    if (saveResult.isSuccess) {
                        _authState.value = AuthState.Success
                    } else {
                        throw Exception(saveResult.exceptionOrNull()?.message ?: "Error al guardar datos")
                    }
                } else {
                    throw Exception(userIdResult.exceptionOrNull()?.message ?: "Error en registro")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Iniciar sesión y cargar vehículos del usuario.
     */
    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = authRepository.loginUser(email, password)
                if (result.isSuccess) {
                    _authState.value = AuthState.Success
                    val currentUser = auth.currentUser
                    val userId = currentUser?.uid ?: throw Exception("UID no encontrado")
                    fetchVehicles(userId)
                } else {
                    throw Exception(result.exceptionOrNull()?.message ?: "Error al iniciar sesión")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Cerrar sesión del usuario.
     */
    fun logoutUser() {
        viewModelScope.launch {
            authRepository.logoutUser()
            _authState.value = AuthState.LoggedOut
        }
    }

    /**
     * Obtener nombre de usuario desde Firestore.
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
     * Subir imagen de vehículo a Firebase Storage.
     */
    suspend fun uploadVehicleImage(userId: String, imageUri: Uri): String {
        return try {
            val storageRef = storage.reference.child("vehicles/$userId/${imageUri.lastPathSegment}")
            storageRef.putFile(imageUri).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw Exception("Error al subir la imagen: ${e.message}")
        }
    }

    /**
     * Recuperar vehículos asociados al usuario.
     */
    fun fetchVehicles(userId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("vehicles")
                    .get()
                    .await()
                val vehicleList = snapshot.documents.mapNotNull { it.data as? Map<String, String> }
                _vehicles.value = vehicleList
            } catch (e: Exception) {
                _vehicles.value = emptyList()
            }
        }
    }

    /**
     * Agregar un vehículo con o sin imagen.
     */
    fun addVehicleWithImage(
        userId: String,
        vehicleData: Map<String, String>,
        imageUri: Uri?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val updatedVehicleData = vehicleData.toMutableMap()
                if (imageUri != null) {
                    val imageUrl = uploadVehicleImage(userId, imageUri)
                    updatedVehicleData["imageUrl"] = imageUrl
                }

                firestore.collection("users")
                    .document(userId)
                    .collection("vehicles")
                    .add(updatedVehicleData)
                    .await()

                fetchVehicles(userId)
                onSuccess()
            } catch (e: Exception) {
                onFailure("Error al agregar el vehículo: ${e.message}")
            }
        }
    }

    /**
     * Recuperar todas las compañías de Firestore.
     */
    fun fetchCompanies() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("companies").get().await()
                val companyList = snapshot.documents.mapNotNull { it.getString("name") }
                _companies.value = companyList
            } catch (e: Exception) {
                _companies.value = emptyList() // Asegúrate de resetear a una lista vacía en caso de error
            }
        }
    }


    /**
     * Agregar una nueva compañía a Firestore.
     */
    fun addCompany(
        companyName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val companyData = mapOf("name" to companyName)
                firestore.collection("companies").add(companyData).await()
                fetchCompanies()
                onSuccess()
            } catch (e: Exception) {
                onFailure("Error al agregar compañía: ${e.message}")
            }
        }
    }

    fun updateCompany(
        oldName: String,
        newName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("companies")
                    .whereEqualTo("name", oldName)
                    .get()
                    .await()
                val document = snapshot.documents.firstOrNull()
                if (document != null) {
                    document.reference.update("name", newName).await()
                    fetchCompanies()
                    onSuccess()
                } else {
                    onFailure("Compañía no encontrada")
                }
            } catch (e: Exception) {
                onFailure("Error al actualizar compañía: ${e.message}")
            }
        }
    }

    fun deleteCompany(
        companyName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("companies")
                    .whereEqualTo("name", companyName)
                    .get()
                    .await()
                val document = snapshot.documents.firstOrNull()
                if (document != null) {
                    document.reference.delete().await()
                    fetchCompanies()
                    onSuccess()
                } else {
                    onFailure("Compañía no encontrada")
                }
            } catch (e: Exception) {
                onFailure("Error al eliminar compañía: ${e.message}")
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
