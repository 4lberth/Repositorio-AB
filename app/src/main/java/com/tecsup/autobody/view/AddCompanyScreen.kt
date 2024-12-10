package com.tecsup.autobody.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tecsup.autobody.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCompanyScreen(viewModel: AuthViewModel, navController: NavController) {
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var editCompanyDialog by remember { mutableStateOf<String?>(null) }
    val companies by viewModel.companies.collectAsState(initial = emptyList())
    var errorMessage by remember { mutableStateOf("") }

    // Cargar compañías al abrir la pantalla
    LaunchedEffect(Unit) {
        viewModel.fetchCompanies()
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
            Text(text = "Lista de Compañías", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(companies.size) { index ->
                    val company = companies[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = company, style = MaterialTheme.typography.bodyLarge)
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Botón para editar compañía
                                TextButton(onClick = { editCompanyDialog = company }) {
                                    Text("Editar")
                                }
                                // Botón para eliminar compañía
                                TextButton(onClick = {
                                    scope.launch {
                                        viewModel.deleteCompany(
                                            companyName = company,
                                            onSuccess = { errorMessage = "Compañía eliminada con éxito" },
                                            onFailure = { errorMessage = it }
                                        )
                                    }
                                }) {
                                    Text("Eliminar")
                                }
                            }
                        }
                    }
                }
            }
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

    // Diálogo para agregar compañía
    if (showDialog) {
        AddCompanyDialog(
            onDismiss = { showDialog = false },
            onSave = { companyName ->
                scope.launch {
                    viewModel.addCompany(
                        companyName,
                        onSuccess = {
                            showDialog = false
                            errorMessage = "Compañía agregada con éxito"
                        },
                        onFailure = { errorMessage = it }
                    )
                }
            },
            companies = companies
        )
    }

    // Diálogo para editar compañía
    if (editCompanyDialog != null) {
        EditCompanyDialog(
            currentName = editCompanyDialog!!,
            onDismiss = { editCompanyDialog = null },
            onSave = { newName ->
                scope.launch {
                    viewModel.updateCompany(
                        oldName = editCompanyDialog!!,
                        newName = newName,
                        onSuccess = {
                            editCompanyDialog = null
                            errorMessage = "Compañía actualizada con éxito"
                        },
                        onFailure = { errorMessage = it }
                    )
                }
            }
        )
    }
}


@Composable
fun AddCompanyDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    companies: List<String>
) {
    var companyName by remember { mutableStateOf("") }
    var selectedCompany by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                val finalCompanyName = selectedCompany.ifBlank { companyName }
                if (finalCompanyName.isNotBlank()) {
                    onSave(finalCompanyName)
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
        title = { Text("Agregar o Seleccionar Compañía") },
        text = {
            Column {
                OutlinedTextField(
                    value = selectedCompany,
                    onValueChange = {},
                    label = { Text("Seleccionar Compañía") },
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
                    companies.forEach { company ->
                        DropdownMenuItem(
                            text = { Text(company) },
                            onClick = {
                                selectedCompany = company
                                expanded = false
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Nueva Compañía (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
fun EditCompanyDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
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
        title = { Text("Editar Compañía") },
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


