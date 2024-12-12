package com.tecsup.autobody

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tecsup.autobody.view.AddCompanyScreen
import com.tecsup.autobody.view.AddVehicleScreen
import com.tecsup.autobody.view.AdminHomeScreen
import com.tecsup.autobody.view.EditServiceScreen
import com.tecsup.autobody.view.HomeScreen
import com.tecsup.autobody.view.LoginScreen
import com.tecsup.autobody.view.ProfileScreen
import com.tecsup.autobody.view.RegisterScreen
import com.tecsup.autobody.view.ServiceScreen
import com.tecsup.autobody.viewmodel.AuthViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel = remember { AuthViewModel() } // El ViewModel se conserva entre las pantallas

    // Observa la ruta actual
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    // Determina si mostrar el BottomBar
    val showBottomBar = currentRoute !in listOf("login", "register", "admin_home")

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
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") { LoginScreen(navController, authViewModel) }
            composable("register") { RegisterScreen(navController, authViewModel) }
            composable("home") {
                HomeScreen(
                    userId = authViewModel.auth.currentUser?.uid.orEmpty(),
                    viewModel = authViewModel,
                    navController = navController
                )
            }
            composable("add_vehicle") {
                AddVehicleScreen(
                    userId = authViewModel.auth.currentUser?.uid.orEmpty(),
                    viewModel = authViewModel,
                    navController = navController
                )
            }
            composable("addCompany") {
                AddCompanyScreen(viewModel = authViewModel, navController = navController)
            }
            composable("profile") {
                ProfileScreen(
                    userId = authViewModel.auth.currentUser?.uid.orEmpty(),
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
                    userId = authViewModel.auth.currentUser?.uid.orEmpty(),
                    viewModel = authViewModel,
                    navController = navController
                )
            }
        }
    }
}

