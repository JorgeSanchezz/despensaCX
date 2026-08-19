package com.example.despensacx.viewmodel

import androidx.lifecycle.*
import com.example.despensacx.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComparadorPreciosViewModel @Inject constructor(
    private val catalogoDao: CatalogoDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val resultados: LiveData<List<CatalogoProducto>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.length < 2) flowOf(emptyList())
            else catalogoDao.searchByName(query).asFlow()
        }.asLiveData()

    fun updateQuery(query: String) {
        _searchQuery.value = query
    }

    suspend fun getPreciosHistoricos(barcode: String): List<PrecioHistorico> {
        return catalogoDao.getPreciosHistoricos(barcode)
    }
}
