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

    private val _services = MutableStateFlow<List<Map<String, String>>>(emptyList())
    val services: StateFlow<List<Map<String, String>>> = _services

    private val _userRole = MutableStateFlow<String>("")
    val userRole: StateFlow<String> = _userRole

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
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
                        "email" to email,
                        "role" to "cliente" // Por defecto todos son clientes
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

    fun fetchUserRole(userId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val document = firestore.collection("users").document(userId).get().await()
                val role = document.getString("role") ?: "cliente" // Por defecto "cliente"
                _userRole.value = role
                onComplete()
            } catch (e: Exception) {
                _userRole.value = "cliente" // Valor por defecto en caso de error
                onComplete()
            }
        }
    }

    // En AuthViewModel
    fun isDniUnique(dni: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users")
                    .whereEqualTo("dni", dni)
                    .get()
                    .await()
                // Si no hay documentos, el DNI es único
                onResult(snapshot.isEmpty)
            } catch (e: Exception) {
                // En caso de error, por seguridad decimos que no es único, o manejar el error
                onResult(false)
            }
        }
    }

    fun fetchUserData(
        userId: String,
        onSuccess: (Map<String,String>) -> Unit,
        onFailure: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("users").document(userId).get().await()
                val data = doc.data as? Map<String, String> ?: emptyMap()
                onSuccess(data)
            } catch (e: Exception) {
                onFailure()
            }
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            authRepository.logoutUser()
            _authState.value = AuthState.LoggedOut
        }
    }fun loginUser(email: String, password: String, onRoleDetermined: (String) -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = authRepository.loginUser(email, password)
                if (result.isSuccess) {
                    val currentUser = auth.currentUser
                    val userId = currentUser?.uid ?: throw Exception("UID no encontrado")

                    // Verificar el rol del usuario después de iniciar sesión
                    fetchUserRole(userId) {
                        onRoleDetermined(_userRole.value) // Llama al callback con el rol obtenido
                    }
                    _authState.value = AuthState.Success
                } else {
                    throw Exception(result.exceptionOrNull()?.message ?: "Error al iniciar sesión")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error desconocido")
            }
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

    fun fetchPersonalCompanies(userId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("companies")
                    .get()
                    .await()
                val personalList = snapshot.documents.mapNotNull { doc ->
                    doc.getString("name")?.let { Company(it, doc.id) }
                }
                _personalCompanies.value = personalList
            } catch (e: Exception) {
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
                // Verificar si ya existe un vehículo con la misma placa en toda la colección
                val placa = vehicleData["placa"] ?: ""
                if (placa.isBlank()) {
                    onFailure("La placa no puede estar vacía.")
                    return@launch
                }

                val querySnapshot = firestore.collectionGroup("vehicles")
                    .whereEqualTo("placa", placa)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    // Ya existe un vehículo con esa placa
                    onFailure("Ya existe un vehículo con la placa $placa.")
                    return@launch
                }

                // Si no existe, continuamos con el proceso de subida de imagen y agregado
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
                val placa = updatedVehicleData["placa"] ?: ""

                // Verificar si la placa ya existe en otro usuario
                val querySnapshot = firestore.collectionGroup("vehicles")
                    .whereEqualTo("placa", placa)
                    .get()
                    .await()

                if (querySnapshot.documents.any { it.id != vehicleId }) {
                    onFailure("La placa $placa ya está registrada.")
                    return@launch
                }

                if (imageUri != null) {
                    val imageUrl = uploadVehicleImage(userId, imageUri)
                    updatedVehicleData["imageUrl"] = imageUrl
                }

                firestore.collection("users")
                    .document(userId)
                    .collection("vehicles")
                    .document(vehicleId)
                    .set(updatedVehicleData)
                    .await()

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

    fun addService(
        userId: String,
        vehiclePlaca: String,
        date: String,
        hour: String,
        fuel: String,
        mileage: String,
        companyName: String?,
        workDetails: List<String>, // Lista de detalles de trabajo
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val serviceData = mapOf(
                    "vehiclePlaca" to vehiclePlaca,
                    "date" to date,
                    "hour" to hour,
                    "fuel" to fuel,
                    "mileage" to mileage,
                    "companyName" to (companyName ?: ""),
                    "workDetails" to workDetails, // Guardar la lista en Firestore
                    "createdAt" to System.currentTimeMillis().toString()
                )
                firestore.collection("users")
                    .document(userId)
                    .collection("services")
                    .add(serviceData)
                    .await()

                onSuccess()
            } catch (e: Exception) {
                onFailure("Error al guardar el servicio: ${e.message}")
            }
        }
    }



    fun fetchServices(userId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("services")
                    .get()
                    .await()

                val serviceList = snapshot.documents.mapNotNull { doc ->
                    (doc.data as? Map<String, String>)?.toMutableMap()?.apply {
                        this["id"] = doc.id
                    }
                } ?: emptyList()

                _services.value = serviceList

                // Agregar log para depuración
                println("Servicios recuperados: $serviceList")
            } catch (e: Exception) {
                _services.value = emptyList()
                println("Error al recuperar servicios: ${e.message}")
            }
        }
    }

    fun deleteService(userId: String, serviceId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("services")
                    .document(serviceId)
                    .delete()
                    .await()
                fetchServices(userId) // Actualizar la lista de servicios
                onSuccess()
            } catch (e: Exception) {
                onFailure("Error al eliminar el servicio: ${e.message}")
            }
        }
    }

    fun updateService(
        userId: String?,
        serviceId: String,
        updatedData: Map<String, Any>, // Cambiado a Any para permitir listas
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (userId.isNullOrEmpty()) {
                onFailure("Error: userId no válido")
                return@launch
            }

            try {
                if (updatedData.isEmpty()) {
                    onFailure("Los datos a actualizar están vacíos")
                    return@launch
                }

                firestore.collection("users")
                    .document(userId)
                    .collection("services")
                    .document(serviceId)
                    .update(updatedData)
                    .await()

                fetchServices(userId) // Actualiza la lista después de un cambio
                onSuccess()
            } catch (e: Exception) {
                onFailure("Error al actualizar servicio: ${e.message}")
            }
        }
    }


//    suspend fun fetchVehicleStates(): List<String> {
//        return try {
//            val snapshot = firestore.collection("vehicleStates").get().await()
//            snapshot.documents.mapNotNull { it.getString("state") }
//        } catch (e: Exception) {
//            println("Error al obtener estados del vehículo: ${e.message}")
//            emptyList()
//        }
//    }
//
//    fun addVehicleState(newState: String) {
//        viewModelScope.launch {
//            try {
//                val data = mapOf("state" to newState)
//                firestore.collection("vehicleStates").add(data).await()
//                println("Estado agregado exitosamente: $newState")
//            } catch (e: Exception) {
//                println("Error al agregar estado del vehículo: ${e.message}")
//            }
//        }
//    }


    // Métodos relacionados con usuarios
    fun fetchAllServices() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collectionGroup("services").get().await()
                val serviceList = snapshot.documents.mapNotNull { doc ->
                    val serviceData = doc.data as? MutableMap<String, String> ?: return@mapNotNull null
                    val userId = doc.reference.parent.parent?.id
                    if (userId.isNullOrEmpty()) {
                        println("Error: userId no encontrado para el documento ${doc.id}")
                        return@mapNotNull null
                    }

                    // Obtener información adicional del usuario (Cliente)
                    val userDoc = firestore.collection("users").document(userId).get().await()
                    val clientName = userDoc.getString("name") ?: "Sin información"
                    val clientDniRuc = userDoc.getString("dni") ?: "Sin información"
                    val clientAddress = userDoc.getString("address") ?: "Sin información"
                    val clientPhone = userDoc.getString("phone") ?: "Sin información"
                    val clientEmail = userDoc.getString("email") ?: "Sin información"

                    // Agregar datos del cliente al servicio
                    serviceData["clientName"] = clientName
                    serviceData["clientDniRuc"] = clientDniRuc
                    serviceData["clientAddress"] = clientAddress
                    serviceData["clientPhone"] = clientPhone
                    serviceData["clientEmail"] = clientEmail

                    // Obtener información del vehículo
                    val vehiclePlaca = serviceData["vehiclePlaca"] ?: "Sin información"
                    val vehicleDoc = firestore.collection("users")
                        .document(userId)
                        .collection("vehicles")
                        .whereEqualTo("placa", vehiclePlaca)
                        .get()
                        .await()
                        .documents.firstOrNull()

                    val vehicleBrand = vehicleDoc?.getString("marca") ?: "Sin información"
                    val vehicleModel = vehicleDoc?.getString("modelo") ?: "Sin información"
                    val vehicleYear = vehicleDoc?.getString("año") ?: "Sin información"
                    val vehicleColor = vehicleDoc?.getString("color") ?: "Sin información"

                    // Agregar datos del vehículo al servicio
                    serviceData["vehicleBrand"] = vehicleBrand
                    serviceData["vehicleModel"] = vehicleModel
                    serviceData["vehicleYear"] = vehicleYear
                    serviceData["vehicleColor"] = vehicleColor

                    // Agregar identificador del usuario y del servicio
                    serviceData["userId"] = userId
                    serviceData["id"] = doc.id

                    serviceData
                }
                _services.value = serviceList
            } catch (e: Exception) {
                _services.value = emptyList()
                println("Error al recuperar servicios: ${e.message}")
            }
        }
    }

}
