package com.tecsup.autobody.view

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tecsup.autobody.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceScreen(navController: NavController, viewModel: AuthViewModel) {
    val userId = viewModel.auth.currentUser?.uid ?: ""

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            viewModel.fetchVehicles(userId)
        }
    }

    val vehicles by viewModel.vehicles.collectAsState(emptyList())

    // Estado para vehículo
    var expandedVehicle by remember { mutableStateOf(false) }
    var selectedVehicle by remember { mutableStateOf("") }

    // Estado para fecha
    var selectedDate by remember { mutableStateOf("") }

    // Estado para hora
    val hours = generateHalfHourIntervals(8, 0, 17, 30)
    var expandedHour by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableStateOf("") }

    // Estado para combustible
    val fuelLevels = listOf("E", "1/4", "1/2", "3/4", "F")
    var expandedFuel by remember { mutableStateOf(false) }
    var selectedFuel by remember { mutableStateOf("") }

    // Estado para kilometraje
    var mileage by remember { mutableStateOf("") }

    val context = LocalContext.current

    // DatePickerDialog
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(context, { _, y, m, d ->
            val c = Calendar.getInstance()
            c.set(y, m, d)
            selectedDate = dateFormat.format(c.time)
        }, year, month, day).show()
    }

    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Servicios") },
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
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Agregar Servicio", style = MaterialTheme.typography.titleLarge)

            // Seleccionar Vehículo
            ExposedDropdownMenuBox(
                expanded = expandedVehicle,
                onExpandedChange = { expandedVehicle = !expandedVehicle },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedVehicle,
                    onValueChange = {},
                    label = { Text("Seleccionar Vehículo") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicle)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = expandedVehicle,
                    onDismissRequest = { expandedVehicle = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (vehicles.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No hay vehículos registrados") },
                            onClick = { expandedVehicle = false }
                        )
                    } else {
                        vehicles.forEach { vehicle ->
                            val placa = vehicle["placa"] ?: ""
                            DropdownMenuItem(
                                text = { Text(placa) },
                                onClick = {
                                    selectedVehicle = placa
                                    expandedVehicle = false
                                }
                            )
                        }
                    }
                }
            }

            // Seleccionar Fecha
            OutlinedTextField(
                value = selectedDate,
                onValueChange = {},
                label = { Text("Fecha del Servicio") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker() }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar Fecha")
                    }
                }
            )

            // Seleccionar Hora
            ExposedDropdownMenuBox(
                expanded = expandedHour,
                onExpandedChange = { expandedHour = !expandedHour },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedHour,
                    onValueChange = {},
                    label = { Text("Seleccionar Hora") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedHour)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = expandedHour,
                    onDismissRequest = { expandedHour = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (hours.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No hay horas disponibles") },
                            onClick = { expandedHour = false }
                        )
                    } else {
                        hours.forEach { hour ->
                            DropdownMenuItem(
                                text = { Text(hour) },
                                onClick = {
                                    selectedHour = hour
                                    expandedHour = false
                                }
                            )
                        }
                    }
                }
            }

            // Seleccionar Combustible
            ExposedDropdownMenuBox(
                expanded = expandedFuel,
                onExpandedChange = { expandedFuel = !expandedFuel },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedFuel,
                    onValueChange = {},
                    label = { Text("Nivel de Combustible") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFuel)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = expandedFuel,
                    onDismissRequest = { expandedFuel = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    fuelLevels.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level) },
                            onClick = {
                                selectedFuel = level
                                expandedFuel = false
                            }
                        )
                    }
                }
            }

            // Ingresar Kilometraje
            OutlinedTextField(
                value = mileage,
                onValueChange = { mileage = it },
                label = { Text("Kilometraje") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    if (selectedVehicle.isNotBlank() && selectedDate.isNotBlank() && selectedHour.isNotBlank() && selectedFuel.isNotBlank() && mileage.isNotBlank()) {
                        scope.launch {
                            viewModel.addService(
                                userId = userId,
                                vehiclePlaca = selectedVehicle,
                                date = selectedDate,
                                hour = selectedHour,
                                fuel = selectedFuel,
                                mileage = mileage,
                                onSuccess = {
                                    errorMessage = "Servicio guardado con éxito."
                                    // Limpiar campos si lo deseas
                                },
                                onFailure = {
                                    errorMessage = it
                                }
                            )
                        }
                    } else {
                        errorMessage = "Por favor, complete todos los campos."
                    }
                },
                enabled = selectedVehicle.isNotBlank() && selectedDate.isNotBlank() && selectedHour.isNotBlank() && selectedFuel.isNotBlank() && mileage.isNotBlank()
            ) {
                Text("Guardar Servicio")
            }
        }
    }
}

// Generar intervalos de media hora entre 8:00 y 17:30, excluyendo la hora 13:00 a 14:00
fun generateHalfHourIntervals(startHour: Int, startMin: Int, endHour: Int, endMin: Int): List<String> {
    val times = mutableListOf<String>()
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, startHour)
    calendar.set(Calendar.MINUTE, startMin)
    calendar.set(Calendar.SECOND, 0)

    val endCalendar = Calendar.getInstance()
    endCalendar.set(Calendar.HOUR_OF_DAY, endHour)
    endCalendar.set(Calendar.MINUTE, endMin)
    endCalendar.set(Calendar.SECOND, 0)

    val format = SimpleDateFormat("HH:mm", Locale.getDefault())

    while (!calendar.after(endCalendar)) {
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        // Excluir las horas entre 13:00 y 14:00
        if (currentHour == 13) {
            calendar.add(Calendar.MINUTE, 30)
            continue
        }
        times.add(format.format(calendar.time))
        calendar.add(Calendar.MINUTE, 30)
    }
    return times
}
