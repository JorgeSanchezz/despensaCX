package com.example.despensacx.repository

import androidx.lifecycle.LiveData
import com.example.despensacx.data.ListaDao
import com.example.despensacx.data.ListaEntity
import com.example.despensacx.data.ProductoDao
import com.example.despensacx.data.ProductoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListaRepository @Inject constructor(
    private val listaDao: ListaDao,
    private val productoDao: ProductoDao
) {
    fun getListasActivas(): LiveData<List<ListaEntity>> = listaDao.getListasActivas()

    fun getListasArchivadas(): LiveData<List<ListaEntity>> = listaDao.getListasArchivadas()

    fun getListaById(id: Long): LiveData<ListaEntity> = listaDao.getById(id)

    suspend fun getListaByIdSync(id: Long): ListaEntity? = withContext(Dispatchers.IO) {
        listaDao.getByIdSync(id)
    }

    suspend fun getAllListasSync(): List<ListaEntity> = withContext(Dispatchers.IO) {
        listaDao.getAllSync()
    }

    suspend fun getListasActivasSync(): List<ListaEntity> = withContext(Dispatchers.IO) {
        listaDao.getListasActivasSync()
    }

    suspend fun insertLista(lista: ListaEntity): Long = withContext(Dispatchers.IO) {
        listaDao.insert(lista)
    }

    suspend fun updateLista(lista: ListaEntity) = withContext(Dispatchers.IO) {
        listaDao.update(lista)
    }

    suspend fun deleteLista(lista: ListaEntity) = withContext(Dispatchers.IO) {
        listaDao.delete(lista)
    }

    suspend fun guardarLista(listaEditar: ListaEntity?, nombre: String, presupuesto: Double) = withContext(Dispatchers.IO) {
        val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        if (listaEditar == null) {
            val nueva = ListaEntity(
                nombre = nombre,
                fechaCreacion = fecha,
                fechaModificacion = fecha,
                archivada = false,
                presupuestoMaximo = presupuesto
            )
            listaDao.insert(nueva)
        } else {
            listaEditar.nombre = nombre
            listaEditar.presupuestoMaximo = presupuesto
            listaEditar.fechaModificacion = fecha
            listaDao.update(listaEditar)
        }
    }

    suspend fun archivarLista(lista: ListaEntity, archivar: Boolean) = withContext(Dispatchers.IO) {
        val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        lista.archivada = archivar
        lista.fechaModificacion = fecha
        listaDao.update(lista)
    }

    suspend fun duplicarLista(lista: ListaEntity) = withContext(Dispatchers.IO) {
        val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val copia = ListaEntity(
            nombre = lista.nombre + " (Copia)",
            fechaCreacion = fecha,
            fechaModificacion = fecha,
            archivada = false,
            presupuestoMaximo = lista.presupuestoMaximo
        )
        val nuevaId = listaDao.insert(copia)

        val productos = productoDao.getProductosByListaSync(lista.id)
        for (p in productos) {
            val pCopia = ProductoEntity(
                listaId = nuevaId,
                tiendaId = p.tiendaId,
                descripcion = p.descripcion,
                precio = p.precio,
                cantidad = p.cantidad,
                seleccionado = p.seleccionado
            )
            productoDao.insert(pCopia)
        }
    }
}
