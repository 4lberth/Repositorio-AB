package com.tecsup.autobody.view

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
import com.tecsup.autobody.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleScreen(userId: String, viewModel: AuthViewModel, navController: NavController) {
    val scope = rememberCoroutineScope()

    // Estado para controlar el diálogo del formulario
    var showDialog by remember { mutableStateOf(false) }

    // Estado para la lista de vehículos
    val vehicles by viewModel.vehicles.collectAsState(initial = emptyList())

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

            // Mostrar lista de vehículos
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
                            Text(text = "Placa: ${vehicle["placa"]}", style = MaterialTheme.typography.titleSmall)
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

    // Diálogo para agregar vehículo
    if (showDialog) {
        AddVehicleDialog(
            onDismiss = { showDialog = false },
            onSave = { vehicleData ->
                scope.launch {
                    viewModel.addVehicle(userId, vehicleData)
                    showDialog = false
                }
            }
        )
    }
}

@Composable
fun AddVehicleDialog(onDismiss: () -> Unit, onSave: (Map<String, String>) -> Unit) {
    var placa by remember { mutableStateOf("") }
    var año by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }

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
                    onSave(vehicleData)
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
            }
        }
    )
}
