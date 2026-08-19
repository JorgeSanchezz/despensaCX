package com.example.despensacx.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalogo_productos")
data class CatalogoProducto(
    @PrimaryKey
    val barcode: String,
    var nombre: String,
    var descripcion: String = "",
    var categoria: String = "GENERAL",
    var unidad: String = "PZA"
)
