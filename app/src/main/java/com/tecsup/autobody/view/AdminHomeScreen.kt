package com.tecsup.autobody.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.tecsup.autobody.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(viewModel: AuthViewModel, navController: NavController) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val allServices by viewModel.services.collectAsState() // Servicios cargados
    var searchQuery by remember { mutableStateOf("") } // Filtro por placa

    val filteredServices = allServices.filter { service ->
        service["vehiclePlaca"]?.contains(searchQuery, ignoreCase = true) == true ||
                (service["workDetails"] as? List<String>)?.any { detail ->
                    detail.contains(searchQuery, ignoreCase = true)
                } == true
    }

    // Cargar servicios
    LaunchedEffect(Unit) {
        viewModel.fetchAllServices()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        scrimColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.32f),
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Panel de Administración",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = {
                        scope.launch { drawerState.close() }
                        // Navegar a la vista de cliente
                        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        navController.navigate("home?userId=$userId")
                    }) {
                        Text("Ver como Cliente")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botón para cerrar sesión
                    TextButton(onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.logoutUser()
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    }) {
                        Text("Cerrar sesión")
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Panel de Administración") },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Barra de búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar por placa") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            // Se activa el filtro al presionar Enter
                        }
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Lista de servicios
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    val filteredServices = allServices.filter {
                        it["vehiclePlaca"]?.contains(searchQuery, ignoreCase = true) == true
                    }

                    items(filteredServices) { service ->
                        AdminServiceCard(
                            service = service,
                            navController = navController,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminServiceCard(
    service: Map<String, String>,
    navController: NavController,
    viewModel: AuthViewModel
) {
    val vehicleImage = service["vehicleImageUrl"] ?: ""
    val clientName = service["clientName"] ?: "Cliente desconocido"
    val companyName = service["companyName"]
    val placa = service["vehiclePlaca"] ?: "Sin placa"
    val date = service["date"] ?: "Sin fecha"
    val hour = service["hour"] ?: "Sin hora"
    val serviceId = service["id"] ?: ""
    val currentStatus = service["status"] ?: "pendiente"
    val scope = rememberCoroutineScope()

    // Formatear el timestamp de createdAt
    val createdAtTimestamp = service["createdAt"]?.toLongOrNull()
    val createdAt = createdAtTimestamp?.let {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        formatter.format(Date(it))
    } ?: "Fecha de creación desconocida"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                // Imagen del vehículo
                Image(
                    painter = rememberImagePainter(data = vehicleImage),
                    contentDescription = "Imagen del vehículo",
                    modifier = Modifier
                        .size(100.dp)
                        .padding(end = 16.dp)
                )

                Column {
                    Text(text = "Placa: $placa", style = MaterialTheme.typography.bodyLarge)
                    Text(text = "Cliente: $clientName", style = MaterialTheme.typography.bodyMedium)
                    // Mostrar compañía solo si tiene valor
                    if (!companyName.isNullOrEmpty()) {
                        Text(text = "Compañía: $companyName", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(text = "Fecha: $date", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Hora: $hour", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Creado el: $createdAt", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Estado: $currentStatus",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (currentStatus) {
                            "confirmado" -> MaterialTheme.colorScheme.primary
                            "pendiente" -> MaterialTheme.colorScheme.secondary
                            "cancelado" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onBackground
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DropdownMenuStatus(
                    currentStatus = currentStatus,
                    onStatusChange = { newStatus ->
                        scope.launch {
                            val userId = service["userId"]
                            if (userId.isNullOrEmpty()) {
                                println("Error: userId no válido para el servicio $serviceId")
                                return@launch
                            }

                            println("Actualizando estado a: $newStatus para el servicio: $serviceId del usuario: $userId")
                            viewModel.updateService(
                                userId = userId,
                                serviceId = serviceId,
                                updatedData = mapOf("status" to newStatus),
                                onSuccess = {
                                    println("Estado actualizado correctamente a $newStatus")

                                },
                                onFailure = { error -> println("Error al actualizar estado: $error") }
                            )
                        }
                    }
                )

                // Botón para navegar a los detalles del servicio
                TextButton(onClick = {
                    navController.navigate("service_details?serviceId=$serviceId")
                }) {
                    Text("Ver más detalles")
                }
            }

        }
    }
}



@Composable
fun DropdownMenuStatus(currentStatus: String, onStatusChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val statuses = listOf("pendiente", "confirmado", "cancelado")

    Box {
        TextButton(onClick = { expanded = true }) {
            Text("Cambiar Estado")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            statuses.forEach { status ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onStatusChange(status)
                    },
                    text = { Text(text = status.capitalize()) }
                )
            }
        }
    }
}
