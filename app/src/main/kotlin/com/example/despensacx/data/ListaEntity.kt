package com.example.despensacx.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listas")
data class ListaEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var nombre: String,
    var fechaCreacion: String,
    var fechaModificacion: String,
    var archivada: Boolean,
    var presupuestoMaximo: Double
)
