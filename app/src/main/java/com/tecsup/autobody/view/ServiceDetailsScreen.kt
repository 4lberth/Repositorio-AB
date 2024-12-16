package com.tecsup.autobody.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tecsup.autobody.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailsScreen(
    serviceId: String,
    viewModel: AuthViewModel,
    navController: NavController
) {
    var refreshTrigger by remember { mutableStateOf(0) } // Estado local para forzar recomposición
    val service by remember(refreshTrigger) {
        derivedStateOf { viewModel.services.value.find { it["id"] == serviceId } }
    }
    var showDialog by remember { mutableStateOf(false) }
    var showVehicleDialog by remember { mutableStateOf(false) } // Estado para editar vehículo

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles del Servicio") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (service != null) {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Información del cliente
                item {
                    SectionCard(title = "Información del Cliente") {
                        Text("Nombre: ${service!!["clientName"] ?: "Desconocido"}")
                        Text("DNI/RUC: ${service!!["clientDniRuc"] ?: "Sin información"}")
                        Text("Dirección: ${service!!["clientAddress"] ?: "Sin información"}")
                        Text("Teléfono: ${service!!["clientPhone"] ?: "Sin información"}")
                        Text("Correo: ${service!!["clientEmail"] ?: "Sin información"}")
                    }
                }

                // Información del vehículo
                item {
                    SectionCard(title = "Información del Vehículo") {
                        Text("Placa: ${service!!["vehiclePlaca"] ?: "Sin información"}")
                        Text("Marca: ${service!!["vehicleBrand"] ?: "Sin información"}")
                        Text("Modelo: ${service!!["vehicleModel"] ?: "Sin información"}")
                        Text("Año: ${service!!["vehicleYear"] ?: "Sin información"}")
                        Text("Color: ${service!!["vehicleColor"] ?: "Sin información"}")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showVehicleDialog = true }) {
                                Text("Editar")
                            }
                        }
                    }
                }

                // Información del servicio
                item {
                    SectionCard(title = "Información del Servicio") {
                        Text("Fecha: ${service!!["date"] ?: "Sin información"}")
                        Text("Hora: ${service!!["hour"] ?: "Sin información"}")
                        Text("Combustible: ${service!!["fuel"] ?: "Sin información"}")
                        Text("Kilometraje: ${service!!["mileage"] ?: "Sin información"}")
                        Text("Estado: ${service!!["status"] ?: "pendiente"}")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showDialog = true }) {
                                Text("Editar")
                            }
                        }
                    }
                }

                // Detalles de trabajo
                item {
                    SectionCard(title = "Detalles de Trabajo") {
                        val workDetails = service!!["workDetails"] as? List<*> ?: emptyList<String>()
                        if (workDetails.isEmpty()) {
                            Text("No se encontraron detalles de trabajo.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            workDetails.forEach { detail ->
                                Text("- $detail", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // Diálogo de edición del servicio
            if (showDialog) {
                EditServiceDialog(
                    service = service!!,
                    onDismiss = { showDialog = false },
                    onSave = { updatedData ->
                        val userId = service!!["userId"] ?: ""
                        viewModel.updateService(
                            userId = userId,
                            serviceId = service!!["id"] ?: "",
                            updatedData = updatedData,
                            onSuccess = {
                                println("Servicio actualizado correctamente.")
                                refreshTrigger++ // Incrementa el trigger para recomponer la UI
                            },
                            onFailure = { error -> println("Error al actualizar servicio: $error") }
                        )
                        showDialog = false
                    }
                )
            }

            // Diálogo de edición del vehículo
            if (showVehicleDialog) {
                EditVehicleDialog(
                    vehicleData = service!!,
                    onDismiss = { showVehicleDialog = false },
                    onSave = { updatedData ->
                        val userId = service!!["userId"] ?: ""
                        viewModel.updateVehicle(
                            userId = userId,
                            vehicleId = service!!["vehicleId"] ?: "",
                            vehicleData = updatedData,
                            imageUri = null,
                            onSuccess = {
                                println("Vehículo actualizado correctamente.")
                                refreshTrigger++ // Recomponer la UI
                            },
                            onFailure = { error -> println("Error al actualizar vehículo: $error") }
                        )
                        showVehicleDialog = false
                    }
                )
            }
        } else {
            Text(
                text = "Servicio no encontrado.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVehicleDialog(
    vehicleData: Map<String, String>,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    var placa by remember { mutableStateOf(vehicleData["vehiclePlaca"] ?: "") }
    var marca by remember { mutableStateOf(vehicleData["vehicleBrand"] ?: "") }
    var modelo by remember { mutableStateOf(vehicleData["vehicleModel"] ?: "") }
    var year by remember { mutableStateOf(vehicleData["vehicleYear"] ?: "") }
    var color by remember { mutableStateOf(vehicleData["vehicleColor"] ?: "") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Editar Vehículo", style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(value = placa, onValueChange = { placa = it }, label = { Text("Placa") })
                OutlinedTextField(value = marca, onValueChange = { marca = it }, label = { Text("Marca") })
                OutlinedTextField(value = modelo, onValueChange = { modelo = it }, label = { Text("Modelo") })
                OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Año") })
                OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Color") })

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Button(onClick = {
                        onSave(
                            mapOf(
                                "vehiclePlaca" to placa,
                                "vehicleBrand" to marca,
                                "vehicleModel" to modelo,
                                "vehicleYear" to year,
                                "vehicleColor" to color
                            )
                        )
                    }) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditServiceDialog(
    service: Map<String, String>,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    var date by remember { mutableStateOf(service["date"] ?: "") }
    var hour by remember { mutableStateOf(service["hour"] ?: "") }
    var fuel by remember { mutableStateOf(service["fuel"] ?: "") }
    var mileage by remember { mutableStateOf(service["mileage"] ?: "") }
    var status by remember { mutableStateOf(service["status"] ?: "pendiente") }
    var expanded by remember { mutableStateOf(false) }

    // Fondo oscuro semi-transparente
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f))
            .windowInsetsPadding(WindowInsets.systemBars) // Ajustar a los bordes
            .imePadding(), // Ajuste cuando se muestra el teclado
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f) // Ocupa el 90% del ancho
                .fillMaxHeight(0.75f) // Ajuste más conservador del alto
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Editar Servicio", style = MaterialTheme.typography.titleLarge)

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Fecha") })
                OutlinedTextField(value = hour, onValueChange = { hour = it }, label = { Text("Hora") })
                OutlinedTextField(value = fuel, onValueChange = { fuel = it }, label = { Text("Combustible") })
                OutlinedTextField(value = mileage, onValueChange = { mileage = it }, label = { Text("Kilometraje") })

                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text("Estado: ${status.replaceFirstChar { it.uppercaseChar() }}")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("pendiente", "confirmado", "cancelado").forEach { newState ->
                            DropdownMenuItem(
                                onClick = {
                                    status = newState
                                    expanded = false
                                },
                                text = { Text(newState.replaceFirstChar { it.uppercaseChar() }) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Button(onClick = {
                        onSave(
                            mapOf(
                                "date" to date,
                                "hour" to hour,
                                "fuel" to fuel,
                                "mileage" to mileage,
                                "status" to status
                            )
                        )
                    }) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

