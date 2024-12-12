package com.tecsup.autobody.view
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tecsup.autobody.BottomBarNavItem
import com.tecsup.autobody.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(userId: String, viewModel: AuthViewModel, navController: NavController) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var userName by remember { mutableStateOf("Usuario") }
    var userRole by remember { mutableStateOf("cliente") }

    val services by viewModel.services.collectAsState()

    // Estado para el diálogo de confirmación
    var showDeleteDialog by remember { mutableStateOf(false) }
    var serviceToDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        viewModel.getUserName(
            userId = userId,
            onSuccess = { name -> userName = name },
            onFailure = { userName = "Error al obtener nombre" }
        )
        viewModel.fetchUserRole(userId) {
            userRole = viewModel.userRole.value // Actualizar el rol
        }
    }

    // Función para eliminar el servicio
    fun deleteService(serviceId: String) {
        scope.launch {
            viewModel.deleteService(
                userId = userId,
                serviceId = serviceId,
                onSuccess = { serviceToDelete = null }, // Reiniciar estado tras éxito
                onFailure = { error -> /* Manejar error */ }
            )
        }
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
                        text = "Menú de Navegación",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("add_vehicle?userId=$userId")
                    }) {
                        Text("Vehículos")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("addCompany")
                    }) {
                        Text("Agregar Compañía")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mostrar solo si el usuario es admin
                    if (userRole == "admin") {
                        TextButton(onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("admin_home") // Navegar al panel de admin
                        }) {
                            Text("Regresar al Panel de Administración")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                    title = { Text(text = "Auto Body") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch { drawerState.open() }
                            }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            navController.navigate("profile?userId=$userId")
                        }) {
                            Icon(Icons.Default.Person, contentDescription = "Perfil")
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
                Text(text = "Bienvenido, $userName", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "¡Has iniciado sesión correctamente!")

                Spacer(modifier = Modifier.height(16.dp))

                Text("Servicios:", style = MaterialTheme.typography.titleMedium)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(services) { service ->
                        val serviceId = service["id"] ?: ""
                        ServiceCard(
                            userId = userId,
                            serviceId = serviceId,
                            placa = service["vehiclePlaca"] ?: "Sin Placa",
                            date = service["date"] ?: "Sin Fecha",
                            hour = service["hour"] ?: "Sin Hora",
                            viewModel = viewModel,
                            onEdit = { id, _ ->
                                // Navegar a la pantalla de edición
                                navController.navigate("edit_service?serviceId=$id")
                            },
                            onDelete = { id ->
                                serviceToDelete = id
                                showDeleteDialog = true
                            }
                        )
                    }
                }

                // Diálogo de confirmación
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Confirmar eliminación") },
                        text = { Text("¿Estás seguro de que deseas eliminar este servicio?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    serviceToDelete?.let { deleteService(it) }
                                    showDeleteDialog = false
                                }
                            ) {
                                Text("Eliminar")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showDeleteDialog = false }
                            ) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceCard(
    userId: String,
    serviceId: String,
    placa: String,
    date: String,
    hour: String,
    viewModel: AuthViewModel,
    onEdit: (serviceId: String, currentData: Map<String, String>) -> Unit,
    onDelete: (serviceId: String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Placa: $placa", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Fecha: $date", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Hora: $hour", style = MaterialTheme.typography.bodyMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // Botón para editar
                IconButton(
                    onClick = {
                        onEdit(serviceId, mapOf("vehiclePlaca" to placa, "date" to date, "hour" to hour))
                    }
                ) {
                    Icon(Icons.Default.Build, contentDescription = "Editar")
                }

                // Botón para eliminar
                IconButton(onClick = {
                    onDelete(serviceId)
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                }
            }
        }
    }
}



