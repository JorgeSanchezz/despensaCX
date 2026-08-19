package com.example.despensacx.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class Categoria(val nombre: String, val icono: ImageVector) {
    GENERAL("General", Icons.Default.Inventory),
    FRUTAS_VERDURAS("Frutas y Verduras", Icons.Default.Eco),
    LACTEOS("Lácteos", Icons.Default.Egg),
    CARNES("Carnes", Icons.Default.Restaurant),
    ABARROTES("Abarrotes", Icons.Default.ShoppingBasket),
    LIMPIEZA("Limpieza", Icons.Default.CleaningServices),
    HIGIENE("Higiene Personal", Icons.Default.Face),
    BEBIDAS("Bebidas", Icons.Default.LocalDrink),
    MASCOTAS("Mascotas", Icons.Default.Pets),
    OTROS("Otros", Icons.Default.MoreHoriz)
}
