package com.tecsup.autobody.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    // Usar estado recordable
    val service by remember { derivedStateOf { viewModel.services.value.find { it["id"] == serviceId } } }

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
                    }
                }

                // Información del servicio
                item {
                    SectionCard(title = "Información del Servicio") {
                        Text("Fecha: ${service!!["date"] ?: "Sin información"}")
                        Text("Hora: ${service!!["hour"] ?: "Sin información"}")
                        Text("Combustible: ${service!!["fuel"] ?: "Sin información"}")
                        Text("Kilometraje: ${service!!["mileage"] ?: "Sin información"}")
                        Text("Estado: ${service!!["status"] ?: "Sin información"}")
                        Text("Compañía: ${service!!["companyName"] ?: "Sin información"}")
                    }
                }
                // En ServiceDetailsScreen
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

