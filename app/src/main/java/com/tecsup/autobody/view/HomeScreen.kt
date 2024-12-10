package com.tecsup.autobody.view
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tecsup.autobody.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(userId: String, viewModel: AuthViewModel, navController: NavController) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var userName by remember { mutableStateOf("Usuario") }

    // Obtener el nombre del usuario al cargar la pantalla
    LaunchedEffect(userId) {
        viewModel.getUserName(
            userId = userId,
            onSuccess = { name ->
                userName = name
            },
            onFailure = { error ->
                userName = error
            }
        )
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
                        navController.navigate("add_vehicle?userId=$userId") // Redirige a la vista para agregar vehículos
                    }) {
                        Text("Vehículos")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { navController.navigate("addCompany") }) {
                        Text("Agregar Compañía")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botón de Cerrar sesión
                    TextButton(onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.logoutUser()
                        navController.navigate("login") // Redirige al login
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
            }
        }
    }
}
