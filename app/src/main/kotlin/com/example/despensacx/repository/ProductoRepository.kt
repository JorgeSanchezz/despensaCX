package com.example.despensacx.repository

import androidx.lifecycle.LiveData
import com.example.despensacx.data.ProductoDao
import com.example.despensacx.data.ProductoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductoRepository @Inject constructor(
    private val productoDao: ProductoDao
) {
    fun getProductosByLista(listaId: Long): LiveData<List<ProductoEntity>> =
        productoDao.getProductosByLista(listaId)

    suspend fun getProductosByListaSync(listaId: Long): List<ProductoEntity> =
        withContext(Dispatchers.IO) {
            productoDao.getProductosByListaSync(listaId)
        }

    suspend fun getAllProductosSync(): List<ProductoEntity> =
        withContext(Dispatchers.IO) {
            productoDao.getAllSync()
        }

    suspend fun insertProducto(producto: ProductoEntity): Long =
        withContext(Dispatchers.IO) {
            productoDao.insert(producto)
        }

    suspend fun updateProducto(producto: ProductoEntity) =
        withContext(Dispatchers.IO) {
            productoDao.update(producto)
        }

    suspend fun deleteProducto(producto: ProductoEntity) =
        withContext(Dispatchers.IO) {
            productoDao.delete(producto)
        }

    suspend fun setAllSeleccionado(listaId: Long, seleccionado: Boolean) =
        withContext(Dispatchers.IO) {
            productoDao.setAllSeleccionadoSync(listaId, seleccionado)
        }
}
