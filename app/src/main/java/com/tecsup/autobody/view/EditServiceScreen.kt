package com.tecsup.autobody.view

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tecsup.autobody.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
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

    var placa by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("") }
    var fuel by remember { mutableStateOf("E") }
    var mileage by remember { mutableStateOf("") }
    var selectedCompany by remember { mutableStateOf("") }
    var workDetails = remember { mutableStateListOf<String>() }
    var editingDetailIndex by remember { mutableStateOf(-1) }
    var editingDetail by remember { mutableStateOf("") }
    var newDetail by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var deletingDetailIndex by remember { mutableStateOf(-1) } // Índice del detalle que se eliminará

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val context = LocalContext.current

    val fuelOptions = listOf("E", "1/4", "1/2", "3/4", "F")
    val personalCompanies by viewModel.personalCompanies.collectAsState(initial = emptyList())
    val companyOptions = personalCompanies.map { it.name }

    // Cargar datos del servicio al iniciar
    LaunchedEffect(serviceId) {
        try {
            viewModel.fetchPersonalCompanies(userId)
            val service = viewModel.services.value.find { it["id"] == serviceId }
            if (service != null) {
                placa = service["vehiclePlaca"] ?: ""
                date = service["date"] ?: ""
                hour = service["hour"] ?: ""
                fuel = service["fuel"] ?: "E"
                mileage = service["mileage"] ?: ""
                selectedCompany = service["companyName"] ?: ""
                val details = service["workDetails"] as? List<String> ?: emptyList()
                workDetails.clear()
                workDetails.addAll(details)
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
        // Hacer que todo el contenido sea desplazable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Campos del formulario
            OutlinedTextField(
                value = placa,
                onValueChange = { placa = it },
                label = { Text("Placa del Vehículo") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = date,
                onValueChange = {},
                label = { Text("Fecha del Servicio") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                                date = selectedDate.format(dateFormatter)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar Fecha")
                    }
                }
            )

            OutlinedTextField(
                value = hour,
                onValueChange = { hour = it },
                label = { Text("Hora del Servicio") },
                modifier = Modifier.fillMaxWidth()
            )

            DropdownMenuField(
                label = "Nivel de Combustible",
                options = fuelOptions,
                selectedOption = fuel,
                onOptionSelected = { fuel = it },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mileage,
                onValueChange = { mileage = it },
                label = { Text("Kilometraje") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            DropdownMenuField(
                label = "Compañía Relacionada",
                options = companyOptions,
                selectedOption = selectedCompany,
                onOptionSelected = { selectedCompany = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Lista de Detalles de Trabajo
            Text("Detalles de Trabajo:", style = MaterialTheme.typography.titleMedium)
            workDetails.forEachIndexed { index, detail ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(detail, modifier = Modifier.weight(1f))
                    Row {
                        IconButton(onClick = {
                            editingDetailIndex = index
                            editingDetail = detail
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar Detalle")
                        }
                        IconButton(onClick = {
                            deletingDetailIndex = index // Establecer el índice para eliminar
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar Detalle")
                        }
                    }
                }
            }

            // Campo para añadir un nuevo detalle
            OutlinedTextField(
                value = newDetail,
                onValueChange = { newDetail = it },
                label = { Text("Nuevo Detalle") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (newDetail.isNotBlank()) {
                        workDetails.add(newDetail)
                        newDetail = ""
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Añadir Detalle")
            }

            // Botón para guardar cambios
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
                                    "mileage" to mileage,
                                    "companyName" to selectedCompany,
                                    "workDetails" to workDetails.toList() // Esto puede ser List<String>
                                ),
                                onSuccess = { navController.popBackStack() },
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

            // Mostrar errores
            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    // Ventana flotante para editar un detalle
    if (editingDetailIndex != -1) {
        AlertDialog(
            onDismissRequest = {
                editingDetailIndex = -1
                editingDetail = ""
            },
            title = { Text("Editar Detalle") },
            text = {
                OutlinedTextField(
                    value = editingDetail,
                    onValueChange = { editingDetail = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Detalle de Trabajo") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editingDetail.isNotBlank()) {
                        workDetails[editingDetailIndex] = editingDetail
                        editingDetailIndex = -1
                        editingDetail = ""
                    }
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    editingDetailIndex = -1
                    editingDetail = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Ventana de confirmación para eliminar un detalle
    if (deletingDetailIndex != -1) {
        AlertDialog(
            onDismissRequest = { deletingDetailIndex = -1 },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar este detalle?") },
            confirmButton = {
                TextButton(onClick = {
                    workDetails.removeAt(deletingDetailIndex)
                    deletingDetailIndex = -1
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingDetailIndex = -1 }) {
                    Text("Cancelar")
                }
            }
        )
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
