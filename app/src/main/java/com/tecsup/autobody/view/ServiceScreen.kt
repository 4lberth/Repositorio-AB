package com.tecsup.autobody.view

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
            viewModel.fetchPersonalCompanies(userId)
        }
    }

    val vehicles by viewModel.vehicles.collectAsState(emptyList())
    val companies by viewModel.personalCompanies.collectAsState(emptyList())

    var expandedCompany by remember { mutableStateOf(false) }
    var selectedCompany by remember { mutableStateOf("") }

    var expandedVehicle by remember { mutableStateOf(false) }
    var selectedVehicle by remember { mutableStateOf("") }

    var selectedDate by remember { mutableStateOf("") }

    var hourInput by remember { mutableStateOf("") }

    val fuelLevels = listOf("E", "1/4", "1/2", "3/4", "F")
    var expandedFuel by remember { mutableStateOf(false) }
    var selectedFuel by remember { mutableStateOf("") }

    var mileage by remember { mutableStateOf("") }

    val context = LocalContext.current

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

            if (companies.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = expandedCompany,
                    onExpandedChange = { expandedCompany = !expandedCompany },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCompany,
                        onValueChange = {},
                        label = { Text("Seleccionar Compañía") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCompany)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedCompany,
                        onDismissRequest = { expandedCompany = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        companies.forEach { company ->
                            DropdownMenuItem(
                                text = { Text(company.name) },
                                onClick = {
                                    selectedCompany = company.name
                                    expandedCompany = false
                                }
                            )
                        }
                    }
                }
            }

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

            OutlinedTextField(
                value = hourInput,
                onValueChange = { hourInput = it },
                label = { Text("Hora del Servicio") },
                placeholder = { Text("Horario de atención: 8:00am - 17:30pm (Receso: 13:00pm - 14:00pm)") },
                modifier = Modifier.fillMaxWidth()
            )

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
                    if (selectedVehicle.isNotBlank() && selectedDate.isNotBlank() &&
                        hourInput.isNotBlank() && selectedFuel.isNotBlank() && mileage.isNotBlank()
                    ) {
                        scope.launch {
                            viewModel.addService(
                                userId = userId,
                                vehiclePlaca = selectedVehicle,
                                date = selectedDate,
                                hour = hourInput,
                                fuel = selectedFuel,
                                mileage = mileage,
                                companyName = if (companies.isNotEmpty()) selectedCompany else "",
                                onSuccess = {
                                    errorMessage = "Servicio guardado con éxito."
                                },
                                onFailure = {
                                    errorMessage = it
                                }
                            )
                        }
                    } else {
                        errorMessage = "Por favor, complete todos los campos."
                    }
                }
            ) {
                Text("Guardar Servicio")
            }
        }
    }
}
