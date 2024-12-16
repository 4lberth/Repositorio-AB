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
fun ProfileScreen(userId: String, viewModel: AuthViewModel, navController: NavController) {

    var userName by remember { mutableStateOf("") }
    var userDni by remember { mutableStateOf("") }
    var userAddress by remember { mutableStateOf("") }
    var userPhone by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }

    val vehicles by viewModel.vehicles.collectAsState(emptyList())
    val personalCompanies by viewModel.personalCompanies.collectAsState(emptyList())

    // Cargar datos del usuario
    LaunchedEffect(userId) {
        // Obtener nombre
        viewModel.getUserName(
            userId = userId,
            onSuccess = { name -> userName = name },
            onFailure = { userName = "Error al obtener nombre" }
        )

        // Obtener otros datos del usuario
        viewModel.fetchUserData(userId, onSuccess = { dataMap ->
            userDni = dataMap["dni"] ?: ""
            userAddress = dataMap["address"] ?: ""
            userPhone = dataMap["phone"] ?: ""
            userEmail = dataMap["email"] ?: ""
        }, onFailure = {
            // Manejar error: Podrías poner un estado de error para mostrar algo en la UI
        })

        // Cargar vehículos y compañías
        viewModel.fetchVehicles(userId)
        viewModel.fetchCompanies(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 24.dp), // margen más generoso
            verticalArrangement = Arrangement.spacedBy(16.dp) // Espaciado entre secciones
        ) {
            item {
                Text(
                    text = "Datos del Cliente",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Información del cliente
                Text("Nombre: $userName", style = MaterialTheme.typography.bodyLarge)
                Text("DNI/RUC: $userDni", style = MaterialTheme.typography.bodyLarge)
                Text("Dirección: $userAddress", style = MaterialTheme.typography.bodyLarge)
                Text("Teléfono: $userPhone", style = MaterialTheme.typography.bodyLarge)
                Text("Email: $userEmail", style = MaterialTheme.typography.bodyLarge)
            }

            item {
                Text(
                    text = "Mis Vehículos",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Lista de vehículos, sin cartas, solo texto con espaciado entre ellos
                if (vehicles.isEmpty()) {
                    Text("No tienes vehículos registrados.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        vehicles.forEach { vehicle ->
                            Column {
                                Text("Placa: ${vehicle["placa"] ?: ""}", style = MaterialTheme.typography.bodyMedium)
                                Text("Año: ${vehicle["año"] ?: ""}", style = MaterialTheme.typography.bodyMedium)
                                Text("Marca: ${vehicle["marca"] ?: ""}", style = MaterialTheme.typography.bodyMedium)
                                Text("Modelo: ${vehicle["modelo"] ?: ""}", style = MaterialTheme.typography.bodyMedium)
                                Text("Color: ${vehicle["color"] ?: ""}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Mis Compañías",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Lista de compañías personales, sin cartas, solo texto
                if (personalCompanies.isEmpty()) {
                    Text("No tienes compañías registradas.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        personalCompanies.forEach { company ->
                            Text(company.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
