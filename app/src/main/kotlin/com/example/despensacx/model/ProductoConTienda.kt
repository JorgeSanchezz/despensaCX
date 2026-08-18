package com.example.despensacx.model

import com.example.despensacx.data.ProductoEntity
import com.example.despensacx.data.TiendaEntity

data class ProductoConTienda(
    val producto: ProductoEntity,
    val tienda: TiendaEntity
)
