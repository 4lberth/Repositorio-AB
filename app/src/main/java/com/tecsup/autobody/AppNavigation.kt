package com.tecsup.autobody

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tecsup.autobody.view.AddCompanyScreen
import com.tecsup.autobody.view.AddVehicleScreen
import com.tecsup.autobody.view.HomeScreen
import com.tecsup.autobody.view.LoginScreen
import com.tecsup.autobody.view.ProfileScreen
import com.tecsup.autobody.view.RegisterScreen
import com.tecsup.autobody.view.ServiceScreen
import com.tecsup.autobody.viewmodel.AuthViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel = AuthViewModel()

    // Observa la ruta actual
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    // Determina si mostrar el BottomBar
    val showBottomBar = currentRoute !in listOf("login", "register")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    // Si es una ruta con userId, necesitamos manejarlo,
                    // puedes obtener el userId del ViewModel si el usuario ya inició sesión
                    val userId = authViewModel.auth.currentUser?.uid ?: ""
                    val adjustedItems = bottomBarItems.map { item ->
                        // Ajustar las rutas que requieren userId
                        if (item.route == "home") {
                            item.copy(route = "home?userId=$userId")
                        } else if (item.route == "add_vehicle") {
                            item.copy(route = "add_vehicle?userId=$userId")
                        } else {
                            item
                        }
                    }

                    adjustedItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute?.startsWith(item.route.substringBefore("?")) == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    // popUpTo("home") { inclusive = false } // Opcional
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
            composable("home?userId={userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                HomeScreen(userId = userId, viewModel = authViewModel, navController = navController)
            }
            composable("add_vehicle?userId={userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                AddVehicleScreen(userId, authViewModel, navController)
            }
            composable("addCompany") {
                AddCompanyScreen(viewModel = authViewModel, navController = navController)
            }
            composable("profile?userId={userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                ProfileScreen(userId = userId, viewModel = authViewModel, navController = navController)
            }
            composable("service") {
                ServiceScreen(navController = navController)
            }
        }
    }
}




