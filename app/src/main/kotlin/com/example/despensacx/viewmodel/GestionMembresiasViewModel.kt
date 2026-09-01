package com.example.despensacx.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.despensacx.data.MembresiaDao
import com.example.despensacx.data.MembresiaEntity
import com.example.despensacx.data.TiendaDao
import com.example.despensacx.data.TiendaEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GestionMembresiasViewModel @Inject constructor(
    private val membresiaDao: MembresiaDao,
    private val tiendaDao: TiendaDao
) : ViewModel() {

    val membresias: LiveData<List<MembresiaEntity>> = membresiaDao.getAll()
    val tiendas: LiveData<List<TiendaEntity>> = tiendaDao.getAllTiendas()

    fun guardarMembresia(tiendaId: Long, fotoPath: String) {
        viewModelScope.launch {
            membresiaDao.insert(MembresiaEntity(tiendaId = tiendaId, fotoPath = fotoPath))
        }
    }

    fun eliminarMembresia(membresia: MembresiaEntity) {
        viewModelScope.launch {
            membresiaDao.delete(membresia)
        }
    }
}
