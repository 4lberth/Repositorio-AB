package com.tecsup.autobody

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.google.firebase.auth.FirebaseAuth
import com.tecsup.autobody.view.*
import com.tecsup.autobody.viewmodel.AuthViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel = remember { AuthViewModel() }

    var startDestination by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Verificar estado de usuario y definir la pantalla inicial
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            authViewModel.fetchUserRole(currentUser.uid) {
                startDestination = if (authViewModel.userRole.value == "admin") {
                    "admin_home"
                } else {
                    "home"
                }
                isLoading = false
            }
        } else {
            startDestination = "login"
            isLoading = false
        }
    }

    if (isLoading || startDestination == null) {
        LoadingScreen()
    } else {
        // Observa la ruta actual
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = currentBackStackEntry?.destination
        val currentRoute = currentDestination?.route

        // Determina si mostrar el BottomBar
        val showBottomBar = currentRoute !in listOf(
            "login", "register", "admin_home", "service_details?serviceId={serviceId}"
        )

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomBarItems.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.route,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            launchSingleTop = true
                                            restoreState = true
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                        }
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination!!,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("login") { LoginScreen(navController, authViewModel) }
                composable("register") { RegisterScreen(navController, authViewModel) }
                composable("home") {
                    HomeScreen(
                        userId = currentUser?.uid.orEmpty(),
                        viewModel = authViewModel,
                        navController = navController
                    )
                }
                composable("add_vehicle") {
                    AddVehicleScreen(
                        userId = currentUser?.uid.orEmpty(),
                        viewModel = authViewModel,
                        navController = navController
                    )
                }
                composable("addCompany") {
                    AddCompanyScreen(viewModel = authViewModel, navController = navController)
                }
                composable("profile") {
                    ProfileScreen(
                        userId = currentUser?.uid.orEmpty(),
                        viewModel = authViewModel,
                        navController = navController
                    )
                }
                composable("service") {
                    ServiceScreen(navController = navController, viewModel = authViewModel)
                }
                composable("admin_home") {
                    AdminHomeScreen(viewModel = authViewModel, navController = navController)
                }
                composable("edit_service?serviceId={serviceId}") { backStackEntry ->
                    val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
                    EditServiceScreen(
                        serviceId = serviceId,
                        userId = currentUser?.uid.orEmpty(),
                        viewModel = authViewModel,
                        navController = navController
                    )
                }
                composable("service_details?serviceId={serviceId}") { backStackEntry ->
                    val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
                    ServiceDetailsScreen(
                        serviceId = serviceId,
                        viewModel = authViewModel,
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
