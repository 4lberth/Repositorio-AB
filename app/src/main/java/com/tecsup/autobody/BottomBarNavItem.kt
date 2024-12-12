package com.tecsup.autobody

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomBarNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomBarItems = listOf(
    BottomBarNavItem(
        route = "home",
        label = "Home",
        icon = Icons.Default.Home
    ),
    BottomBarNavItem(
        route = "add_vehicle",
        label = "Autos",
        icon = Icons.Default.DirectionsCar
    ),
    BottomBarNavItem(
        route = "addCompany",
        label = "Compañías",
        icon = Icons.Default.Business
    ),
    BottomBarNavItem(
        route = "service",
        label = "Servicio",
        icon = Icons.Default.Build
    )
)

