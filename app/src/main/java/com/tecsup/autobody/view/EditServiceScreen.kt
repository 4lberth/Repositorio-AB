package com.tecsup.autobody.view

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tecsup.autobody.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditServiceScreen(
    serviceId: String,
    userId: String,
    viewModel: AuthViewModel,
    navController: NavController
) {
    val scope = rememberCoroutineScope()

    // Estados para los campos del formulario
    var placa by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("Horario de atención: 8:00am - 17:30pm (Receso: 13:00pm - 14:00pm)") }
    var fuel by remember { mutableStateOf("E") }
    var mileage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val context = LocalContext.current

    // Cargar datos actuales del servicio
    LaunchedEffect(serviceId) {
        try {
            var service = viewModel.services.value.find { it["id"] == serviceId }
            if (service == null) {
                val snapshot = viewModel.firestore.collection("users")
                    .document(userId)
                    .collection("services")
                    .document(serviceId)
                    .get()
                    .await()

                service = snapshot.data as? Map<String, String>
            }

            if (service != null) {
                placa = service["vehiclePlaca"] ?: ""
                date = service["date"] ?: ""
                hour = service["hour"] ?: ""
                fuel = service["fuel"] ?: "E"
                mileage = service["mileage"] ?: ""
            } else {
                errorMessage = "No se encontraron datos para este servicio."
            }
        } catch (e: Exception) {
            errorMessage = "Error al cargar los datos del servicio: ${e.message}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Servicio") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = placa,
                onValueChange = { placa = it },
                label = { Text("Placa del Vehículo") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    label = { Text("Fecha del Servicio") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            val calendar = Calendar.getInstance()
                            val datePickerDialog = DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                                    date = selectedDate.format(dateFormatter)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            )
                            datePickerDialog.show()
                        }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar Fecha")
                        }
                    }
                )
            }

            OutlinedTextField(
                value = hour,
                onValueChange = { hour = it },
                label = { Text("Hora del Servicio") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Horario de atención: 8:00am - 17:30pm (Receso: 13:00pm - 14:00pm)") }
            )

            // Selector de nivel de combustible
            DropdownMenuField(
                label = "Nivel de Combustible",
                options = listOf("E", "1/4", "1/2", "3/4", "F"),
                selectedOption = fuel,
                onOptionSelected = { fuel = it },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mileage,
                onValueChange = { mileage = it },
                label = { Text("Kilometraje") },
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    if (placa.isNotBlank() && date.isNotBlank() && hour.isNotBlank() && fuel.isNotBlank() && mileage.isNotBlank()) {
                        scope.launch {
                            viewModel.updateService(
                                userId = userId,
                                serviceId = serviceId,
                                updatedData = mapOf(
                                    "vehiclePlaca" to placa,
                                    "date" to date,
                                    "hour" to hour,
                                    "fuel" to fuel,
                                    "mileage" to mileage
                                ),
                                onSuccess = {
                                    navController.popBackStack()
                                },
                                onFailure = { error -> errorMessage = error }
                            )
                        }
                    } else {
                        errorMessage = "Por favor, completa todos los campos."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Cambios")
            }
        }
    }
}

@Composable
fun DropdownMenuField(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}