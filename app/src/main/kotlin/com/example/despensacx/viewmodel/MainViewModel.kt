package com.example.despensacx.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.despensacx.data.AppDatabase
import com.example.despensacx.data.ListaDao
import com.example.despensacx.data.ListaEntity
import com.example.despensacx.data.ProductoDao
import com.example.despensacx.data.ProductoEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val listaDao: ListaDao,
    private val productoDao: ProductoDao
) : ViewModel() {

    val listasActivas: LiveData<List<ListaEntity>> = listaDao.getListasActivas()

    fun guardarLista(lista: ListaEntity?, nombre: String, presupuesto: Double) {
        val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        viewModelScope.launch {
            if (lista == null) {
                val nueva = ListaEntity(
                    nombre = nombre,
                    fechaCreacion = fecha,
                    fechaModificacion = fecha,
                    archivada = false,
                    presupuestoMaximo = presupuesto
                )
                listaDao.insert(nueva)
            } else {
                lista.nombre = nombre
                lista.presupuestoMaximo = presupuesto
                lista.fechaModificacion = fecha
                listaDao.update(lista)
            }
        }
    }

    fun archivarLista(lista: ListaEntity, archivar: Boolean) {
        val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        lista.archivada = archivar
        lista.fechaModificacion = fecha
        viewModelScope.launch { listaDao.update(lista) }
    }

    fun duplicarLista(lista: ListaEntity) {
        viewModelScope.launch {
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

    fun eliminarLista(lista: ListaEntity) {
        viewModelScope.launch { listaDao.delete(lista) }
    }
}
