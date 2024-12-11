package com.tecsup.autobody.view
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tecsup.autobody.viewmodel.AuthState
import com.tecsup.autobody.viewmodel.AuthViewModel

@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Inicio de Sesión", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Correo Electrónico") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            viewModel.loginUser(email, password, onRoleDetermined = { role ->
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                if (role == "admin") {
                    // Navegar a la vista de administrador
                    navController.navigate("admin_home") {
                        popUpTo("login") { inclusive = true }
                    }
                } else {
                    // Navegar a la vista de cliente
                    navController.navigate("home?userId=$userId") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            })
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Iniciar Sesión")
        }


        when (authState) {
            is AuthState.Success -> {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                navController.navigate("home?userId=$userId") {
                    popUpTo("login") { inclusive = true } // Evitar regresar al login
                }
            }
            is AuthState.Error -> {
                Text((authState as AuthState.Error).message, color = MaterialTheme.colorScheme.error)
            }
            else -> {}
        }


        Spacer(modifier = Modifier.height(16.dp))

        // Botón para redirigir al registro
        TextButton(
            onClick = { navController.navigate("register") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("¿No tienes cuenta? Regístrate aquí")
        }
    }
}



