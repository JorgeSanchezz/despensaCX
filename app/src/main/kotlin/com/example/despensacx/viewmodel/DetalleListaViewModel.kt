package com.example.despensacx.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.despensacx.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DetalleListaViewModel @Inject constructor(
    private val listaDao: ListaDao,
    private val productoDao: ProductoDao,
    private val tiendaDao: TiendaDao,
    private val catalogoDao: CatalogoDao,
    private val ticketFotoDao: TicketFotoDao,
    private val tiendaTotalDao: TiendaTotalDao
) : ViewModel() {

    fun getLista(id: Long): LiveData<ListaEntity> = listaDao.getById(id)
    fun getTiendas(): LiveData<List<TiendaEntity>> = tiendaDao.getAllTiendas()
    fun getProductos(listaId: Long): LiveData<List<ProductoEntity>> = productoDao.getProductosByLista(listaId)
    fun getTicketFotos(listaId: Long): LiveData<List<TicketFotoEntity>> = ticketFotoDao.getFotosByLista(listaId)
    fun getTotalesTienda(listaId: Long): LiveData<List<TiendaTotalEntity>> = tiendaTotalDao.getTotalesByLista(listaId)

    fun setTotalManualTienda(listaId: Long, tiendaId: Long, totalManual: Double?, lista: ListaEntity?) {
        viewModelScope.launch {
            if (totalManual != null) {
                tiendaTotalDao.insertOrUpdate(TiendaTotalEntity(listaId, tiendaId, totalManual))
            } else {
                tiendaTotalDao.delete(listaId, tiendaId)
            }
            actualizarFechaModificacion(lista)
        }
    }

    fun setTotalManualLista(lista: ListaEntity?, totalManual: Double?) {
        viewModelScope.launch {
            lista?.let {
                listaDao.update(it.copy(totalManual = totalManual))
            }
        }
    }

    fun guardarFotoTicket(listaId: Long, tiendaId: Long, path: String) {
        viewModelScope.launch {
            ticketFotoDao.insert(TicketFotoEntity(listaId = listaId, tiendaId = tiendaId, fotoPath = path))
        }
    }
    
    fun eliminarFotoTicket(foto: TicketFotoEntity) {
        viewModelScope.launch {
            ticketFotoDao.delete(foto)
        }
    }

    suspend fun buscarEnCatalogo(barcode: String): CatalogoProducto? {
        return catalogoDao.getByBarcode(barcode)
    }

    suspend fun getPreciosHistoricos(barcode: String): List<PrecioHistorico> {
        return catalogoDao.getPreciosHistoricos(barcode)
    }

    fun toggleProducto(producto: ProductoEntity, seleccionado: Boolean, lista: ListaEntity?) {
        viewModelScope.launch {
            productoDao.update(producto.copy(seleccionado = seleccionado))
            actualizarFechaModificacion(lista)
        }
    }

    fun guardarProducto(
        productoExistente: ProductoEntity?,
        listaId: Long,
        tiendaId: Long,
        descripcion: String,
        precio: Double,
        cantidad: Double,
        lista: ListaEntity?,
        barcode: String? = null,
        categoria: String = "GENERAL",
        unidad: String = "PZA",
        onSuccess: ((ProductoEntity) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val productoGuardado = if (productoExistente == null) {
                val p = ProductoEntity(
                    listaId = listaId,
                    tiendaId = tiendaId,
                    descripcion = descripcion,
                    precio = precio,
                    cantidad = cantidad,
                    seleccionado = true,
                    barcode = barcode,
                    categoria = categoria,
                    unidad = unidad
                )
                val newId = productoDao.insert(p)
                p.copy(id = newId)
            } else {
                val p = productoExistente.copy(
                    descripcion = descripcion,
                    precio = precio,
                    cantidad = cantidad,
                    tiendaId = tiendaId,
                    barcode = barcode,
                    categoria = categoria,
                    unidad = unidad
                )
                productoDao.update(p)
                p
            }

            // Actualizar o insertar en el catálogo global si hay código de barras
            barcode?.let {
                catalogoDao.insert(CatalogoProducto(it, descripcion, categoria = categoria, unidad = unidad))
            }

            actualizarFechaModificacion(lista)
            onSuccess?.invoke(productoGuardado)
        }
    }

    fun eliminarProducto(producto: ProductoEntity, lista: ListaEntity? = null) {
        viewModelScope.launch {
            productoDao.delete(producto)
            actualizarFechaModificacion(lista)
        }
    }

    fun eliminarSeleccionados(listaId: Long, lista: ListaEntity?) {
        viewModelScope.launch {
            productoDao.deleteSeleccionadosByLista(listaId)
            actualizarFechaModificacion(lista)
        }
    }

    fun seleccionarTodos(listaId: Long, seleccionado: Boolean, lista: ListaEntity?) {
        viewModelScope.launch {
            productoDao.setAllSeleccionadoSync(listaId, seleccionado)
            actualizarFechaModificacion(lista)
        }
    }

    private suspend fun actualizarFechaModificacion(lista: ListaEntity?) {
        lista?.let {
            val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            listaDao.update(it.copy(fechaModificacion = fecha))
        }
    }
}
