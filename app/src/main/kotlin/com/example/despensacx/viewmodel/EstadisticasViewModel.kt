package com.example.despensacx.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.despensacx.data.AppDatabase
import com.example.despensacx.data.ListaDao
import com.example.despensacx.data.ProductoDao
import com.example.despensacx.data.TiendaDao
import com.example.despensacx.model.Categoria
import com.example.despensacx.model.EstadisticaModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EstadisticasViewModel @Inject constructor(
    private val listaDao: ListaDao,
    private val productoDao: ProductoDao,
    private val tiendaDao: TiendaDao
) : ViewModel() {

    private val _estadisticas = MutableLiveData<List<EstadisticaModel.AnnoModel>>()
    val estadisticas: LiveData<List<EstadisticaModel.AnnoModel>> = _estadisticas

    private val _loading = MutableLiveData<Boolean>(true)
    val loading: LiveData<Boolean> = _loading

    fun cargarEstadisticas() {
        viewModelScope.launch {
            _loading.value = true
            val data = withContext(Dispatchers.IO) {
                calcularEstadisticas()
            }
            _estadisticas.value = data
            _loading.value = false
        }
    }

    private fun calcularEstadisticas(): List<EstadisticaModel.AnnoModel> {
        val listas = listaDao.getAllSync()
        val productos = productoDao.getAllSync()
        val tiendas = tiendaDao.getAllTiendasSync()

        val mapaTiendas = tiendas.associate { it.id to it.nombre }
        val productosPorListaYTienda = mutableMapOf<Long, MutableMap<String, Double>>()
        val productosPorListaYCategoria = mutableMapOf<Long, MutableMap<String, Double>>()
        val totalPorLista = mutableMapOf<Long, Double>()

        for (p in productos) {
            if (!p.seleccionado) continue
            val lId = p.listaId
            val subtotal = p.precio * p.cantidad
            
            // Por Tienda
            val nombreTienda = mapaTiendas.getOrDefault(p.tiendaId, "Otra")
            val mTiendas = productosPorListaYTienda.getOrPut(lId) { mutableMapOf() }
            mTiendas[nombreTienda] = mTiendas.getOrDefault(nombreTienda, 0.0) + subtotal
            
            // Por Categoría
            val catNombre = try { Categoria.valueOf(p.categoria).nombre } catch(e: Exception) { "General" }
            val mCats = productosPorListaYCategoria.getOrPut(lId) { mutableMapOf() }
            mCats[catNombre] = mCats.getOrDefault(catNombre, 0.0) + subtotal
            
            totalPorLista[lId] = totalPorLista.getOrDefault(lId, 0.0) + subtotal
        }

        val estructuraAnios = TreeMap<String, MutableMap<String, EstadisticaModel.MesModel>>(Collections.reverseOrder())
        val fmtLecturaFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fmtAnio = SimpleDateFormat("yyyy", Locale.getDefault())
        val fmtMesClave = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val fmtMesNombre = SimpleDateFormat("MMMM", Locale("es", "ES"))

        for (lista in listas) {
            try {
                val fecha = lista.fechaCreacion.let { if (it.isNotEmpty()) fmtLecturaFecha.parse(it) else null } ?: Date()
                val anioStr = fmtAnio.format(fecha)
                val mesClave = fmtMesClave.format(fecha)
                var mesNombre = fmtMesNombre.format(fecha)
                if (mesNombre.isNotEmpty()) {
                    mesNombre = mesNombre.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }

                val totalLista = totalPorLista.getOrDefault(lista.id, 0.0)
                val desglosTiendasLista = productosPorListaYTienda.getOrDefault(lista.id, mutableMapOf())
                val desglosCatsLista = productosPorListaYCategoria.getOrDefault(lista.id, mutableMapOf())
                
                val mesesDelAnio = estructuraAnios.getOrPut(anioStr) { mutableMapOf() }

                if (mesesDelAnio.containsKey(mesClave)) {
                    val mesExistente = mesesDelAnio[mesClave]!!
                    val nuevoTotalMes = mesExistente.totalMes + totalLista
                    
                    // Consolidar Tiendas
                    val tiendasConsolidadas = mesExistente.gastosPorTienda.toMutableMap()
                    for ((key, value) in desglosTiendasLista) {
                        tiendasConsolidadas[key] = tiendasConsolidadas.getOrDefault(key, 0.0) + value
                    }
                    
                    // Consolidar Categorías
                    val catsConsolidadas = mesExistente.gastosPorCategoria.toMutableMap()
                    for ((key, value) in desglosCatsLista) {
                        catsConsolidadas[key] = catsConsolidadas.getOrDefault(key, 0.0) + value
                    }
                    
                    mesesDelAnio[mesClave] = EstadisticaModel.MesModel(
                        mesNombre, mesClave, nuevoTotalMes, 
                        tiendasConsolidadas, catsConsolidadas
                    )
                } else {
                    mesesDelAnio[mesClave] = EstadisticaModel.MesModel(
                        mesNombre, mesClave, totalLista, 
                        desglosTiendasLista.toMap(), desglosCatsLista.toMap()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val listaAnios = mutableListOf<EstadisticaModel.AnnoModel>()
        for ((anio, mesesMap) in estructuraAnios) {
            val anioObj = EstadisticaModel.AnnoModel(anio)
            // Ordenar meses cronológicamente para calcular variación
            val mesesList = mesesMap.values.sortedBy { it.mesAnioClave }
            
            for (i in mesesList.indices) {
                val mesActual = mesesList[i]
                if (i > 0) {
                    val mesAnterior = mesesList[i-1]
                    if (mesAnterior.totalMes > 0) {
                        mesActual.variacionMesAnterior = ((mesActual.totalMes - mesAnterior.totalMes) / mesAnterior.totalMes) * 100
                    }
                }
                anioObj.totalAnio += mesActual.totalMes
            }
            
            // Volver a ordenar por clave descendente para la vista
            anioObj.meses.addAll(mesesList.sortedByDescending { it.mesAnioClave })
            listaAnios.add(anioObj)
        }
        return listaAnios
    }
}
