package com.example.despensacx.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.despensacx.data.TiendaDao
import com.example.despensacx.data.TiendaEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GestionTiendasViewModel @Inject constructor(
    private val tiendaDao: TiendaDao
) : ViewModel() {

    val tiendas: LiveData<List<TiendaEntity>> = tiendaDao.getAllTiendas()

    fun guardarTienda(tiendaExistente: TiendaEntity?, nombre: String, color: String) {
        viewModelScope.launch {
            val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            if (tiendaExistente == null) {
                val nueva = TiendaEntity(nombre = nombre, color = color, fechaRegistro = fecha, orden = 99)
                tiendaDao.insert(nueva)
            } else {
                val actualizada = tiendaExistente.copy(nombre = nombre, color = color)
                tiendaDao.update(actualizada)
            }
        }
    }

    suspend fun countProductosByTienda(tiendaId: Long): Int {
        return tiendaDao.countProductosByTiendaSync(tiendaId)
    }

    fun eliminarTienda(tienda: TiendaEntity) {
        viewModelScope.launch {
            tiendaDao.delete(tienda)
        }
    }
}
