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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleScreen(userId: String, viewModel: AuthViewModel, navController: NavController) {
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    val vehicles by viewModel.vehicles.collectAsState(initial = emptyList())

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
            FloatingActionButton(onClick = { showDialog = true }) {
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
                            Text(text = "Placa: ${vehicle["placa"]}")
                            Text(text = "Año: ${vehicle["año"]}")
                            Text(text = "Marca: ${vehicle["marca"]}")
                            Text(text = "Modelo: ${vehicle["modelo"]}")
                            Text(text = "Color: ${vehicle["color"]}")
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(userId) {
        viewModel.fetchVehicles(userId) // Vuelve a cargar los vehículos
    }

    if (showDialog) {
        AddVehicleDialog(
            onDismiss = { showDialog = false },
            onSave = { vehicleData, imageUri ->
                scope.launch {
                    viewModel.addVehicleWithImage(
                        userId = userId,
                        vehicleData = vehicleData,
                        imageUri = imageUri,
                        onSuccess = { showDialog = false },
                        onFailure = { errorMsg ->
                            Log.e("AddVehicle", "Error al agregar el vehículo: $errorMsg")
                            // Aquí se puede mostrar un Snackbar u otra UI de error.
                        }
                    )
                }
            }
        )
    }
}

@Composable
fun AddVehicleDialog(
    onDismiss: () -> Unit,
    onSave: (Map<String, String>, Uri?) -> Unit
) {
    var placa by remember { mutableStateOf("") }
    var año by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        imageUri = it
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val vehicleData = mapOf(
                        "placa" to placa,
                        "año" to año,
                        "marca" to marca,
                        "modelo" to modelo,
                        "color" to color
                    )
                    onSave(vehicleData, imageUri)
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        title = { Text("Agregar Vehículo") },
        text = {
            Column {
                OutlinedTextField(
                    value = placa,
                    onValueChange = { placa = it },
                    label = { Text("Placa (única)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = año,
                    onValueChange = { año = it },
                    label = { Text("Año") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = marca,
                    onValueChange = { marca = it },
                    label = { Text("Marca") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelo,
                    onValueChange = { modelo = it },
                    label = { Text("Modelo") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
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
