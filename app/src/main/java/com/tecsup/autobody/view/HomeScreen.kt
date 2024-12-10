package com.tecsup.autobody.view

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tecsup.autobody.viewmodel.AuthState
import com.tecsup.autobody.viewmodel.AuthViewModel

@Composable
fun HomeScreen(name: String?, viewModel: AuthViewModel, navController: NavController) {
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.LoggedOut) {
            navController.navigate("login") {
                popUpTo("home") { inclusive = true } // Elimina la pantalla de Home del stack
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Bienvenido, $name", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "¡Has iniciado sesión correctamente!")

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.logoutUser()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar sesión")
        }
    }
}


