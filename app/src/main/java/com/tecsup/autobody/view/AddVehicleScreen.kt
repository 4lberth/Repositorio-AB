package com.tecsup.autobody.view

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.tecsup.autobody.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleScreen(userId: String, viewModel: AuthViewModel, navController: NavController) {
    val scope = rememberCoroutineScope()
    val vehicles by viewModel.vehicles.collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Variables para edición y eliminación
    var vehicleToEdit by remember { mutableStateOf<Map<String, String>?>(null) }
    var vehicleToDelete by remember { mutableStateOf<Map<String, String>?>(null) }

    // Carga vehículos al abrir la pantalla
    LaunchedEffect(userId) {
        viewModel.fetchVehicles(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vehículos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Vehículo")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(text = "Lista de Vehículos", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(vehicles.size) { index ->
                    val vehicle = vehicles[index]
                    val vehicleId = vehicle["id"] ?: return@items
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            AsyncImage(
                                model = vehicle["imageUrl"],
                                contentDescription = "Imagen del Vehículo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Placa: ${vehicle["placa"] ?: ""}")
                            Text(text = "Año: ${vehicle["año"] ?: ""}")
                            Text(text = "Marca: ${vehicle["marca"] ?: ""}")
                            Text(text = "Modelo: ${vehicle["modelo"] ?: ""}")
                            Text(text = "Color: ${vehicle["color"] ?: ""}")

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = {
                                    vehicleToEdit = vehicle
                                    showEditDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                                }
                                IconButton(onClick = {
                                    vehicleToDelete = vehicle
                                    showDeleteDialog = true
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddVehicleDialog(
            onDismiss = { showAddDialog = false },
            onSave = { vehicleData, imageUri, setPlacaError ->
                scope.launch {
                    viewModel.addVehicleWithImage(
                        userId = userId,
                        vehicleData = vehicleData,
                        imageUri = imageUri,
                        onSuccess = { showAddDialog = false },
                        onFailure = { errorMsg ->
                            Log.e("AddVehicle", "Error al agregar el vehículo: $errorMsg")
                            if (errorMsg.contains("Ya existe un vehículo con la placa")) {
                                setPlacaError(errorMsg)
                            }
                        }
                    )
                }
            }
        )
    }

    if (showEditDialog && vehicleToEdit != null) {
        EditVehicleDialog(
            vehicle = vehicleToEdit!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedData, newImageUri, setPlacaError ->
                val vehicleId = vehicleToEdit!!["id"] ?: return@EditVehicleDialog
                scope.launch {
                    viewModel.updateVehicle(
                        userId = userId,
                        vehicleId = vehicleId,
                        vehicleData = updatedData,
                        imageUri = newImageUri,
                        onSuccess = { showEditDialog = false },
                        onFailure = { errorMsg ->
                            Log.e("AddVehicle", "Error al actualizar: $errorMsg")
                            if (errorMsg.contains("Ya existe un vehículo con la placa")) {
                                setPlacaError(errorMsg)
                            }
                        }
                    )
                }
            }
        )
    }

    if (showDeleteDialog && vehicleToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar este vehículo?") },
            confirmButton = {
                TextButton(onClick = {
                    val vehicleId = vehicleToDelete!!["id"] ?: ""
                    scope.launch {
                        viewModel.deleteVehicle(
                            userId = userId,
                            vehicleId = vehicleId,
                            onSuccess = { showDeleteDialog = false },
                            onFailure = { errorMsg ->
                                Log.e("AddVehicle", "Error al eliminar: $errorMsg")
                            }
                        )
                    }
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AddVehicleDialog(
    onDismiss: () -> Unit,
    onSave: (Map<String, String>, Uri?, (String) -> Unit) -> Unit
) {
    VehicleFormDialog(
        title = "Agregar Vehículo",
        initialData = mapOf("placa" to "", "año" to "", "marca" to "ninguno", "modelo" to "ninguno", "color" to "ninguno"),
        onDismiss = onDismiss,
        onSave = onSave
    )
}

@Composable
fun EditVehicleDialog(
    vehicle: Map<String, String>,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>, Uri?, (String) -> Unit) -> Unit
) {
    VehicleFormDialog(
        title = "Editar Vehículo",
        initialData = vehicle.filterKeys { it != "id" }, // excluir el id al editar
        onDismiss = onDismiss,
        onSave = onSave
    )
}

@Composable
fun VehicleFormDialog(
    title: String,
    initialData: Map<String, String>,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>, Uri?, (String) -> Unit) -> Unit
) {
    var placa by remember { mutableStateOf(initialData["placa"] ?: "") }
    var año by remember { mutableStateOf(initialData["año"] ?: "") }

    // Marca, Modelo, Color con valor "ninguno" por defecto
    var marca by remember { mutableStateOf(initialData["marca"] ?: "ninguno") }
    var modelo by remember { mutableStateOf(initialData["modelo"] ?: "ninguno") }
    var color by remember { mutableStateOf(initialData["color"] ?: "ninguno") }

    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Estados de error
    var placaError by remember { mutableStateOf(false) }
    var añoError by remember { mutableStateOf(false) }
    var errorMessagePlaca by remember { mutableStateOf("") }
    var añoErrorMessage by remember { mutableStateOf("") }

    val setPlacaError: (String) -> Unit = { msg ->
        placaError = true
        errorMessagePlaca = msg
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        imageUri = it
    }

    fun validateFields(): Boolean {
        var valid = true

        // Validar placa
        if (placa.isBlank()) {
            placaError = true
            errorMessagePlaca = "La placa es obligatoria."
            valid = false
        } else {
            placaError = false
            errorMessagePlaca = ""
        }

        // Validar año
        if (año.isBlank()) {
            añoError = true
            añoErrorMessage = "El año es obligatorio."
            valid = false
        } else {
            añoError = false
            añoErrorMessage = ""
        }

        return valid
    }

    // Función para limpiar "ninguno" al escribir
    fun clearNinguno(oldVal: String, newVal: String): String {
        return if (oldVal == "ninguno" && newVal != "ninguno") {
            // Si el valor era "ninguno" y el usuario teclea algo,
            // removemos "ninguno" y nos quedamos solo con la parte nueva
            if (newVal.startsWith("ninguno")) {
                newVal.removePrefix("ninguno")
            } else {
                newVal
            }
        } else {
            newVal
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                if (validateFields()) {
                    val vehicleData = mapOf(
                        "placa" to placa,
                        "año" to año,
                        "marca" to marca,
                        "modelo" to modelo,
                        "color" to color
                    )
                    onSave(vehicleData, imageUri, setPlacaError)
                }
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = placa,
                    onValueChange = {
                        placa = it
                        placaError = false
                        errorMessagePlaca = ""
                    },
                    label = { Text("Placa (única)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = placaError,
                    supportingText = {
                        if (placaError) {
                            Text(errorMessagePlaca, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = año,
                    onValueChange = { input ->
                        // Solo dígitos
                        val filtered = input.filter { it.isDigit() }
                        año = filtered
                        añoError = false
                        añoErrorMessage = ""
                    },
                    label = { Text("Año") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = añoError,
                    supportingText = {
                        if (añoError) {
                            Text(añoErrorMessage, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Marca
                OutlinedTextField(
                    value = marca,
                    onValueChange = { newVal ->
                        // Si era "ninguno", lo removemos al escribir
                        marca = clearNinguno(marca, newVal)
                    },
                    label = { Text("Marca") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Modelo
                OutlinedTextField(
                    value = modelo,
                    onValueChange = { newVal ->
                        modelo = clearNinguno(modelo, newVal)
                    },
                    label = { Text("Modelo") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Color
                OutlinedTextField(
                    value = color,
                    onValueChange = { newVal ->
                        color = clearNinguno(color, newVal)
                    },
                    label = { Text("Color") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Seleccionar Imagen")
                }
                if (imageUri != null) {
                    Text(text = "Imagen seleccionada: ${imageUri?.lastPathSegment}")
                }
            }
        }
    )
}
