package com.tecsup.autobody.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tecsup.autobody.viewmodel.AuthState
import com.tecsup.autobody.viewmodel.AuthViewModel
import com.tecsup.autobody.viewmodel.Company
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCompanyScreen(viewModel: AuthViewModel, navController: NavController) {
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }

    val globalCompanies by viewModel.globalCompanies.collectAsState(emptyList())
    val personalCompanies by viewModel.personalCompanies.collectAsState(emptyList())

    var errorMessage by remember { mutableStateOf("") }

    // Para editar compañías personales
    var showEditDialog by remember { mutableStateOf(false) }
    var companyToEdit by remember { mutableStateOf<Company?>(null) }

    LaunchedEffect(viewModel.authState.value) {
        if (viewModel.authState.value is AuthState.Success || viewModel.authState.value is AuthState.Idle) {
            viewModel.fetchCompanies()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compañías") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Compañía")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(text = "Mis Compañías", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // Sólo mostramos las compañías personales del usuario actual.
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(personalCompanies) { company ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = company.name, style = MaterialTheme.typography.bodyLarge)
                            Row {
                                IconButton(onClick = {
                                    companyToEdit = company
                                    showEditDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        viewModel.deletePersonalCompany(
                                            companyId = company.id,
                                            onSuccess = { },
                                            onFailure = { errorMessage = it }
                                        )
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AddCompanyDialog(
                onDismiss = { showDialog = false },
                onSave = { companyName, selectedCompanyName ->
                    scope.launch {
                        viewModel.addCompanyToUser(
                            companyName = companyName,
                            selectedCompanyName = selectedCompanyName,
                            onSuccess = { showDialog = false },
                            onFailure = { errorMessage = it }
                        )
                    }
                },
                globalCompanies = globalCompanies
            )
        }

        if (showEditDialog && companyToEdit != null) {
            EditCompanyDialog(
                currentName = companyToEdit!!.name,
                onDismiss = { showEditDialog = false },
                onSave = { newName ->
                    scope.launch {
                        viewModel.updatePersonalCompany(
                            companyId = companyToEdit!!.id,
                            newName = newName,
                            onSuccess = { showEditDialog = false },
                            onFailure = { errorMessage = it }
                        )
                    }
                }
            )
        }

        if (errorMessage.isNotBlank()) {
            Snackbar(
                action = {
                    TextButton(onClick = { errorMessage = "" }) {
                        Text("Cerrar")
                    }
                }
            ) {
                Text(errorMessage)
            }
        }
    }
}

@Composable
fun AddCompanyDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    globalCompanies: List<Company>
) {
    var companyName by remember { mutableStateOf("") } // Nuevo nombre a agregar globalmente
    var selectedCompany by remember { mutableStateOf("") } // Nombre seleccionado del dropdown
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                val finalSelected = if (companyName.isNotBlank()) companyName else selectedCompany
                if (finalSelected.isNotBlank()) {
                    onSave(companyName, selectedCompany)
                    // companyName: el nuevo nombre (si lo hay)
                    // selectedCompany: el seleccionado del dropdown (si no se dio un nuevo nombre)
                }
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        title = { Text("Agregar Compañía") },
        text = {
            Column {
                // Desplegable de compañías globales existentes
                OutlinedTextField(
                    value = selectedCompany,
                    onValueChange = {},
                    label = { Text("Seleccionar Compañía Global") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Abrir menú")
                        }
                    }
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    globalCompanies.forEach { company ->
                        DropdownMenuItem(
                            text = { Text(company.name) },
                            onClick = {
                                selectedCompany = company.name
                                expanded = false
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Campo para agregar una nueva compañía global si no existe
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Nueva Compañía (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Si ingresas un nuevo nombre, se añadirá a la lista global. De lo contrario, selecciona una existente del desplegable.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    )
}

@Composable
fun EditCompanyDialog(currentName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                if (newName.isNotBlank()) {
                    onSave(newName)
                }
            }) {
                Text("Actualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        title = { Text("Editar Compañía Personal") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nuevo Nombre de la Compañía") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}
