package com.example.despensacx.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.despensacx.data.CatalogoDao
import com.example.despensacx.data.CatalogoProducto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GestionCatalogoViewModel @Inject constructor(
    private val catalogoDao: CatalogoDao
) : ViewModel() {

    val catalogo: LiveData<List<CatalogoProducto>> = catalogoDao.getAll()

    fun guardarProducto(producto: CatalogoProducto) {
        viewModelScope.launch {
            catalogoDao.insert(producto)
        }
    }

    fun eliminarProducto(producto: CatalogoProducto) {
        viewModelScope.launch {
            catalogoDao.delete(producto)
        }
    }
}
