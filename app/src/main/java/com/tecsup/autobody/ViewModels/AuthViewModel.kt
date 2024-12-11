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

data class Company(
    val name: String,
    val id: String
)

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object LoggedOut : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val authRepository: AuthRepository = AuthRepository()) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _vehicles = MutableStateFlow<List<Map<String, String>>>(emptyList())
    val vehicles: StateFlow<List<Map<String, String>>> = _vehicles

    private val _globalCompanies = MutableStateFlow<List<Company>>(emptyList())
    val globalCompanies: StateFlow<List<Company>> = _globalCompanies

    private val _personalCompanies = MutableStateFlow<List<Company>>(emptyList())
    val personalCompanies: StateFlow<List<Company>> = _personalCompanies

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

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
                    fetchCompanies(userId)
                } else {
                    throw Exception(result.exceptionOrNull()?.message ?: "Error al iniciar sesión")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            authRepository.logoutUser()
            _authState.value = AuthState.LoggedOut
        }
    }

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

    suspend fun uploadVehicleImage(userId: String, imageUri: Uri): String {
        return try {
            val storageRef = storage.reference.child("vehicles/$userId/${imageUri.lastPathSegment}")
            storageRef.putFile(imageUri).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw Exception("Error al subir la imagen: ${e.message}")
        }
    }

    fun fetchVehicles(userId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("vehicles")
                    .get()
                    .await()
                val vehicleList = snapshot.documents.mapNotNull { doc ->
                    (doc.data as? Map<String, String>)?.toMutableMap()?.apply {
                        // Agregamos el ID del documento para poder editar/eliminar
                        this["id"] = doc.id
                    }
                } ?: emptyList()
                _vehicles.value = vehicleList
            } catch (e: Exception) {
                _vehicles.value = emptyList()
            }
        }
    }

    fun fetchCompanies(userId: String? = auth.currentUser?.uid) {
        viewModelScope.launch {
            // Cargar globales
            try {
                val globalSnapshot = firestore.collection("companies_global").get().await()
                val globalList = globalSnapshot.documents.mapNotNull { doc ->
                    doc.getString("name")?.let { Company(it, doc.id) }
                }
                _globalCompanies.value = globalList
            } catch (e: Exception) {
                _globalCompanies.value = emptyList()
            }

            // Cargar personales del usuario actual
            if (userId != null) {
                try {
                    val personalSnapshot = firestore.collection("users")
                        .document(userId)
                        .collection("companies")
                        .get()
                        .await()
                    val personalList = personalSnapshot.documents.mapNotNull { doc ->
                        doc.getString("name")?.let { Company(it, doc.id) }
                    }
                    _personalCompanies.value = personalList
                } catch (e: Exception) {
                    _personalCompanies.value = emptyList()
                }
            } else {
                _personalCompanies.value = emptyList()
            }
        }
    }

    fun addCompanyToUser(
        companyName: String,
        selectedCompanyName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")

                val finalName = if (companyName.isNotBlank()) {
                    val globalRef = firestore.collection("companies_global")
                    val exists = globalRef.whereEqualTo("name", companyName).get().await().isEmpty.not()
                    if (!exists) {
                        globalRef.add(mapOf("name" to companyName)).await()
                    }
                    companyName
                } else {
                    selectedCompanyName
                }

                val userCompaniesRef = firestore.collection("users")
                    .document(userId)
                    .collection("companies")
                val personalExists = userCompaniesRef.whereEqualTo("name", finalName).get().await().isEmpty.not()
                if (!personalExists) {
                    userCompaniesRef.add(mapOf("name" to finalName)).await()
                }

                fetchCompanies(userId)
                onSuccess()
            } catch (e: Exception) {
                onFailure("Error al agregar compañía: ${e.message}")
            }
        }
    }

    fun updatePersonalCompany(
        companyId: String,
        newName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: throw Exception("No se encontró usuario")
                firestore.collection("users")
                    .document(userId)
                    .collection("companies")
                    .document(companyId)
                    .update("name", newName)
                    .await()

                fetchCompanies(userId)
                onSuccess()
            } catch (e: Exception) {
                onFailure("Error al actualizar compañía personal: ${e.message}")
            }
        }
    }

    fun deletePersonalCompany(
        companyId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: throw Exception("No se encontró usuario")
                firestore.collection("users")
                    .document(userId)
                    .collection("companies")
                    .document(companyId)
                    .delete()
                    .await()

                fetchCompanies(userId)
                onSuccess()
            } catch (e: Exception) {
                onFailure("Error al eliminar compañía personal: ${e.message}")
            }
        }
    }

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

    fun updateVehicle(
        userId: String,
        vehicleId: String,
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

                val docRef = firestore.collection("users")
                    .document(userId)
                    .collection("vehicles")
                    .document(vehicleId)

                docRef.update(updatedVehicleData as Map<String, Any>).await()
                fetchVehicles(userId)
                onSuccess()
            } catch (e: Exception) {
                onFailure("Error al actualizar el vehículo: ${e.message}")
            }
        }
    }

    fun deleteVehicle(
        userId: String,
        vehicleId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("vehicles")
                    .document(vehicleId)
                    .delete()
                    .await()

                fetchVehicles(userId)
                onSuccess()
            } catch (e: Exception) {
                onFailure("Error al eliminar el vehículo: ${e.message}")
            }
        }
    }
}
