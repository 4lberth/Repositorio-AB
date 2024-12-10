package com.tecsup.autobody

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tecsup.autobody.view.HomeScreen
import com.tecsup.autobody.view.LoginScreen
import com.tecsup.autobody.view.RegisterScreen
import com.tecsup.autobody.viewmodel.AuthViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel = AuthViewModel()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController, authViewModel) }
        composable("register") { RegisterScreen(navController, authViewModel) }
        composable("home?name={name}") { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name")
            HomeScreen(name, authViewModel, navController)
        }
    }
}



