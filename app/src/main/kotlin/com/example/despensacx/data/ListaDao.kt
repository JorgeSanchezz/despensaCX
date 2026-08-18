package com.example.despensacx.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ListaDao {
    @Insert
    fun insert(lista: ListaEntity): Long

    @Update
    fun update(lista: ListaEntity)

    @Delete
    fun delete(lista: ListaEntity)

    @Query("SELECT * FROM listas WHERE id = :id LIMIT 1")
    fun getByIdSync(id: Long): ListaEntity?

    @Query("SELECT * FROM listas WHERE id = :id LIMIT 1")
    fun getById(id: Long): LiveData<ListaEntity>

    @Query("SELECT * FROM listas WHERE archivada = 1 ORDER BY id DESC")
    fun getListasArchivadas(): LiveData<List<ListaEntity>>

    @Query("SELECT * FROM listas")
    fun getAllSync(): List<ListaEntity>

    @Query("SELECT * FROM listas WHERE archivada = 0")
    fun getListasActivasSync(): List<ListaEntity>

    @Query("SELECT * FROM listas WHERE archivada = 0")
    fun getListasActivas(): LiveData<List<ListaEntity>>
}
