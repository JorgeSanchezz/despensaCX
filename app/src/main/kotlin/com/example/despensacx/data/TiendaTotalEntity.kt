package com.example.despensacx.data

import androidx.room.Entity

@Entity(
    tableName = "tienda_totales",
    primaryKeys = ["listaId", "tiendaId"]
)
data class TiendaTotalEntity(
    val listaId: Long,
    val tiendaId: Long,
    val totalManual: Double
)
