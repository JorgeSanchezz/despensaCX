package com.example.despensacx.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.despensacx.data.ListaDao
import com.example.despensacx.data.ListaEntity
import com.example.despensacx.data.ListaResumen
import com.example.despensacx.data.ProductoDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListasArchivadasViewModel @Inject constructor(
    private val listaDao: ListaDao,
    private val productoDao: ProductoDao
) : ViewModel() {

    val listasArchivadas: LiveData<List<ListaEntity>> = listaDao.getListasArchivadas()

    val resumenesListas: LiveData<Map<Long, ListaResumen>> = productoDao.getResumenListas().map { list ->
        list.associateBy { it.listaId }
    }

    fun desarchivarLista(lista: ListaEntity) {
        viewModelScope.launch {
            lista.archivada = false
            listaDao.update(lista)
        }
    }

    fun eliminarLista(lista: ListaEntity) {
        viewModelScope.launch {
            listaDao.delete(lista)
        }
    }
}
