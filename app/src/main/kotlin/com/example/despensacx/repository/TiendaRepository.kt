package com.example.despensacx.repository

import androidx.lifecycle.LiveData
import com.example.despensacx.data.TiendaDao
import com.example.despensacx.data.TiendaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TiendaRepository @Inject constructor(
    private val tiendaDao: TiendaDao
) {
    fun getAllTiendas(): LiveData<List<TiendaEntity>> = tiendaDao.getAllTiendas()

    suspend fun getAllTiendasSync(): List<TiendaEntity> = withContext(Dispatchers.IO) {
        tiendaDao.getAllTiendasSync()
    }

    suspend fun insertTienda(tienda: TiendaEntity): Long = withContext(Dispatchers.IO) {
        tiendaDao.insert(tienda)
    }

    suspend fun updateTienda(tienda: TiendaEntity) = withContext(Dispatchers.IO) {
        tiendaDao.update(tienda)
    }

    suspend fun deleteTienda(tienda: TiendaEntity) = withContext(Dispatchers.IO) {
        tiendaDao.delete(tienda)
    }

    suspend fun countProductosByTienda(tiendaId: Long): Int = withContext(Dispatchers.IO) {
        tiendaDao.countProductosByTiendaSync(tiendaId)
    }
}
