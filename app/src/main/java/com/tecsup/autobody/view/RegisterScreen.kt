package com.tecsup.autobody.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tecsup.autobody.viewmodel.AuthState
import com.tecsup.autobody.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(navController: NavController, viewModel: AuthViewModel) {
    var name by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()

    // Estados de error
    var nameError by remember { mutableStateOf(false) }
    var dniError by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    // Mensaje de error DNI no único
    var dniNotUniqueError by remember { mutableStateOf(false) }

    fun validateFields(): Boolean {
        var valid = true

        if (name.isBlank()) {
            nameError = true
            valid = false
        } else {
            nameError = false
        }

        if (dni.isBlank()) {
            dniError = true
            valid = false
        } else {
            dniError = false
        }

        if (address.isBlank()) {
            addressError = true
            valid = false
        } else {
            addressError = false
        }

        if (phone.isBlank()) {
            phoneError = true
            valid = false
        } else {
            phoneError = false
        }

        if (email.isBlank()) {
            emailError = true
            valid = false
        } else {
            emailError = false
        }

        if (password.isBlank()) {
            passwordError = true
            valid = false
        } else {
            passwordError = false
        }

        return valid
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Registro", style = MaterialTheme.typography.titleLarge)

        // Campo para Nombre del Cliente
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                if (it.isNotBlank()) nameError = false
            },
            label = { Text("Nombre del cliente") },
            modifier = Modifier.fillMaxWidth(),
            isError = nameError,
            supportingText = {
                if (nameError) Text("Campo obligatorio", color = MaterialTheme.colorScheme.error)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Campo para DNI/RUC (solo números)
        OutlinedTextField(
            value = dni,
            onValueChange = {
                val filtered = it.filter { ch -> ch.isDigit() }
                dni = filtered
                if (filtered.isNotBlank()) {
                    dniError = false
                    dniNotUniqueError = false
                }
            },
            label = { Text("DNI/RUC") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = dniError || dniNotUniqueError,
            supportingText = {
                when {
                    dniError -> Text("Campo obligatorio", color = MaterialTheme.colorScheme.error)
                    dniNotUniqueError -> Text("Este DNI ya está registrado", color = MaterialTheme.colorScheme.error)
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Campo para Dirección
        OutlinedTextField(
            value = address,
            onValueChange = {
                address = it
                if (it.isNotBlank()) addressError = false
            },
            label = { Text("Dirección") },
            modifier = Modifier.fillMaxWidth(),
            isError = addressError,
            supportingText = {
                if (addressError) Text("Campo obligatorio", color = MaterialTheme.colorScheme.error)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Campo para Teléfono (solo números)
        OutlinedTextField(
            value = phone,
            onValueChange = {
                val filtered = it.filter { ch -> ch.isDigit() }
                phone = filtered
                if (filtered.isNotBlank()) phoneError = false
            },
            label = { Text("Teléfono") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = phoneError,
            supportingText = {
                if (phoneError) Text("Campo obligatorio", color = MaterialTheme.colorScheme.error)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Campo para Correo Electrónico
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                if (it.isNotBlank()) emailError = false
            },
            label = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth(),
            isError = emailError,
            supportingText = {
                if (emailError) Text("Campo obligatorio", color = MaterialTheme.colorScheme.error)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Campo para Contraseña
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (it.isNotBlank()) passwordError = false
            },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            isError = passwordError,
            supportingText = {
                if (passwordError) Text("Campo obligatorio", color = MaterialTheme.colorScheme.error)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botón de registro
        Button(
            onClick = {
                if (validateFields()) {
                    // Primero verificar si el DNI es único
                    viewModel.isDniUnique(dni) { isUnique ->
                        if (!isUnique) {
                            dniNotUniqueError = true
                        } else {
                            // Si es único, proceder con el registro
                            viewModel.registerUser(name, dni, address, phone, email, password)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrarse")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (authState) {
            is AuthState.Loading -> CircularProgressIndicator()
            is AuthState.Success -> navController.navigate("login")
            is AuthState.Error -> Text(
                (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
            else -> {}
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para volver al login
        TextButton(
            onClick = { navController.navigate("login") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("¿Ya tienes una cuenta? Inicia sesión aquí")
        }
    }
}
