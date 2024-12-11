package com.tecsup.autobody.view

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.tecsup.autobody.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(viewModel: AuthViewModel, navController: NavController) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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

                    // Aquí puedes añadir más botones o navegaciones para el administrador.
                    TextButton(onClick = {
                        scope.launch { drawerState.close() }
                        // Ejemplo: navegar a una pantalla de gestión de usuarios
                        // navController.navigate("admin_users")
                    }) {
                        Text("Gestión de Usuarios")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = {
                        scope.launch { drawerState.close() }
                        // Ejemplo: navegar a una pantalla de gestión de servicios
                        // navController.navigate("admin_services")
                    }) {
                        Text("Gestión de Servicios")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = {
                        scope.launch { drawerState.close() }
                        // Navegar a la vista de cliente
                        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        navController.navigate("home?userId=$userId")
                    }) {
                        Text("Ver como Cliente")
                    }

                    Spacer(modifier = Modifier.height(8.dp))


                    TextButton(onClick = {
                        scope.launch { drawerState.close() }
                        // Cerrar sesión
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
                Text(text = "Bienvenido, Administrador", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Aquí puedes gestionar usuarios, servicios, y más.")
            }
        }
    }
}
