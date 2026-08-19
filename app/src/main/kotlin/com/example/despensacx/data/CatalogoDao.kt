package com.example.despensacx.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CatalogoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(producto: CatalogoProducto)

    @Update
    suspend fun update(producto: CatalogoProducto)

    @Delete
    suspend fun delete(producto: CatalogoProducto)

    @Query("SELECT * FROM catalogo_productos ORDER BY nombre ASC")
    fun getAll(): LiveData<List<CatalogoProducto>>

    @Query("SELECT * FROM catalogo_productos")
    fun getAllSync(): List<CatalogoProducto>

    @Query("DELETE FROM catalogo_productos")
    suspend fun deleteAll()

    @Query("SELECT * FROM catalogo_productos WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): CatalogoProducto?

    @Query("SELECT * FROM catalogo_productos WHERE nombre LIKE '%' || :query || '%' ORDER BY nombre ASC")
    fun searchByName(query: String): LiveData<List<CatalogoProducto>>

    @Query("""
        SELECT p.precio, t.nombre as tiendaNombre, t.id as tiendaId 
        FROM productos p 
        INNER JOIN tiendas t ON p.tiendaId = t.id 
        WHERE p.barcode = :barcode 
        GROUP BY t.id 
        ORDER BY p.id DESC
    """)
    suspend fun getPreciosHistoricos(barcode: String): List<PrecioHistorico>
}

data class PrecioHistorico(
    val precio: Double,
    val tiendaNombre: String,
    val tiendaId: Long
)
