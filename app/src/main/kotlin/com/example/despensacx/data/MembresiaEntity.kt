package com.example.despensacx.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "membresias",
    foreignKeys = [
        ForeignKey(
            entity = TiendaEntity::class,
            parentColumns = ["id"],
            childColumns = ["tiendaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tiendaId")]
)
data class MembresiaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tiendaId: Long,
    val fotoPath: String,
    val notas: String = ""
)
