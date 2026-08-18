package com.example.despensacx.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "productos",
    foreignKeys = [
        ForeignKey(
            entity = ListaEntity::class,
            parentColumns = ["id"],
            childColumns = ["listaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TiendaEntity::class,
            parentColumns = ["id"],
            childColumns = ["tiendaId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("listaId"), Index("tiendaId")]
)
data class ProductoEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var listaId: Long,
    var tiendaId: Long,
    var descripcion: String,
    var precio: Double,
    var cantidad: Int,
    var seleccionado: Boolean
)
