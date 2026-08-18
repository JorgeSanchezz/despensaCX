package com.example.despensacx.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tiendas")
data class TiendaEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var nombre: String,
    var color: String, // Código de color hexadecimal ej "#FF5722"
    var fechaRegistro: String,
    var orden: Int
)
