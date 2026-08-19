package com.example.despensacx.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ticket_fotos",
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
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listaId"), Index("tiendaId")]
)
data class TicketFotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listaId: Long,
    val tiendaId: Long,
    val fotoPath: String
)
