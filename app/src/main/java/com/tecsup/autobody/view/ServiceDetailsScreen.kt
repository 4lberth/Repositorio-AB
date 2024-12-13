package com.tecsup.autobody.view

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.navigation.NavController
import com.tecsup.autobody.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailsScreen(
    serviceId: String,
    viewModel: AuthViewModel,
    navController: NavController
) {
    val service = viewModel.services.value.find { it["id"] == serviceId }
    val scope = rememberCoroutineScope()
    var newVehicleState by remember { mutableStateOf("") }
    val vehicleStates = remember { mutableStateListOf<String>() }
    var selectedState by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var showAddStateDialog by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    // Cargar estados existentes desde Firestore
    LaunchedEffect(Unit) {
        scope.launch {
            val states = viewModel.fetchVehicleStates() // Método en ViewModel
            vehicleStates.clear()
            vehicleStates.addAll(states)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles del Servicio") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Add, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddStateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Estado")
            }
        }
    ) { paddingValues ->
        if (service != null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text("Detalles del Servicio", style = MaterialTheme.typography.titleLarge)

                Spacer(modifier = Modifier.height(16.dp))

                // Datos del cliente
                Text("Datos del Cliente", style = MaterialTheme.typography.titleMedium)
                Text("Nombre: ${service["clientName"] ?: "Desconocido"}")
                Text("DNI/RUC: ${service["clientDniRuc"] ?: "Sin información"}")
                Text("Dirección: ${service["clientAddress"] ?: "Sin información"}")
                Text("Teléfono: ${service["clientPhone"] ?: "Sin información"}")
                Text("Correo Electrónico: ${service["clientEmail"] ?: "Sin información"}")

                Spacer(modifier = Modifier.height(16.dp))

                // Datos del vehículo
                Text("Datos del Vehículo", style = MaterialTheme.typography.titleMedium)
                Text("Placa: ${service["vehiclePlaca"] ?: "Sin placa"}")
                Text("Marca: ${service["vehicleBrand"] ?: "Sin información"}")
                Text("Modelo: ${service["vehicleModel"] ?: "Sin información"}")
                Text("Año: ${service["vehicleYear"] ?: "Sin información"}")
                Text("Color: ${service["vehicleColor"] ?: "Sin información"}")

                Spacer(modifier = Modifier.height(16.dp))

                // Datos del servicio
                Text("Datos del Servicio", style = MaterialTheme.typography.titleMedium)
                Text("Fecha: ${service["date"] ?: "Sin información"}")
                Text("Hora: ${service["hour"] ?: "Sin información"}")
                Text("Kilometraje: ${service["mileage"] ?: "Sin información"}")
                Text("Combustible: ${service["fuel"] ?: "Sin información"}")

                Spacer(modifier = Modifier.height(16.dp))

                // Lista desplegable para seleccionar estado
                OutlinedTextField(
                    value = selectedState,
                    onValueChange = {},
                    label = { Text("Seleccionar Estado") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned {
                            textFieldSize = it.size.toSize()
                        },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Default.Add, contentDescription = "Abrir lista")
                        }
                    }
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(with(LocalDensity.current) { textFieldSize.width.toDp() })
                ) {
                    vehicleStates.forEach { state ->
                        DropdownMenuItem(
                            text = { Text(state) },
                            onClick = {
                                selectedState = state
                                expanded = false
                                // Lógica para guardar el estado seleccionado
                                scope.launch {
                                    viewModel.updateService(
                                        userId = service["userId"] ?: "",
                                        serviceId = serviceId,
                                        updatedData = mapOf("vehicleState" to state),
                                        onSuccess = { println("Estado actualizado a $state") },
                                        onFailure = { println("Error al actualizar estado: $it") }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        } else {
            Text(
                text = "Servicio no encontrado.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        // Diálogo para agregar estado
        if (showAddStateDialog) {
            AlertDialog(
                onDismissRequest = { showAddStateDialog = false },
                title = { Text("Agregar Estado") },
                text = {
                    OutlinedTextField(
                        value = newVehicleState,
                        onValueChange = { newVehicleState = it },
                        label = { Text("Nuevo Estado") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newVehicleState.isNotBlank()) {
                            scope.launch {
                                viewModel.addVehicleState(newVehicleState) // Método en ViewModel
                                vehicleStates.add(newVehicleState)
                                newVehicleState = ""
                                showAddStateDialog = false
                            }
                        }
                    }) {
                        Text("Agregar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddStateDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
