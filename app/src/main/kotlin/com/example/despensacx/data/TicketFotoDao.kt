package com.example.despensacx.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TicketFotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ticketFoto: TicketFotoEntity)

    @Query("SELECT * FROM ticket_fotos WHERE listaId = :listaId AND tiendaId = :tiendaId LIMIT 1")
    fun getFoto(listaId: Long, tiendaId: Long): LiveData<TicketFotoEntity?>

    @Query("SELECT * FROM ticket_fotos WHERE listaId = :listaId")
    fun getFotosByLista(listaId: Long): LiveData<List<TicketFotoEntity>>

    @Query("SELECT * FROM ticket_fotos")
    fun getFotosByListaSync(): List<TicketFotoEntity>

    @Delete
    suspend fun delete(ticketFoto: TicketFotoEntity)

    @Query("DELETE FROM ticket_fotos")
    suspend fun deleteAll()
}
