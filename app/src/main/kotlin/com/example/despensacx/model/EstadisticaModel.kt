package com.example.despensacx.model

class EstadisticaModel {
    data class AnnoModel(
        val anio: String,
        var totalAnio: Double = 0.0,
        val meses: MutableList<MesModel> = mutableListOf()
    )

    data class MesModel(
        val mesNombre: String,
        val mesAnioClave: String, // e.g. "2026-08"
        val totalMes: Double,
        val gastosPorTienda: Map<String, Double>,
        val gastosPorCategoria: Map<String, Double> = emptyMap(),
        var variacionMesAnterior: Double? = null // Porcentaje de cambio
    )
}
