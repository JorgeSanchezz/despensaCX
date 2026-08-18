package com.example.despensacx.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.despensacx.adapter.AnioAdapter
import com.example.despensacx.adapter.MesAdapter
import com.example.despensacx.data.AppDatabase
import com.example.despensacx.databinding.ActivityEstadisticasBinding
import com.example.despensacx.model.EstadisticaModel
import java.text.SimpleDateFormat
import java.util.*

class EstadisticasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEstadisticasBinding
    private lateinit var db: AppDatabase
    private var adapterAnio: AnioAdapter? = null
    private var adapterMes: MesAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEstadisticasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarEstadisticas)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Estadísticas por Año"
        }

        db = AppDatabase.getInstance(this)
        binding.rvEstadisticas.layoutManager = LinearLayoutManager(this)

        cargarDatosEstadisticas()
    }

    private fun cargarDatosEstadisticas() {
        AppDatabase.databaseWriteExecutor.execute {
            val listasActivas = db.listaDao().getListasActivasSync()
            val productos = db.productoDao().getAllSync()
            val tiendas = db.tiendaDao().getAllTiendasSync()

            val mapaTiendas = tiendas.associate { it.id to it.nombre }

            // Agrupar Totales por Lista y por Tienda
            val productosPorListaYTienda = mutableMapOf<Long, MutableMap<String, Double>>()
            val totalPorLista = mutableMapOf<Long, Double>()

            for (p in productos) {
                if (!p.seleccionado) continue

                val lId = p.listaId
                val subtotal = p.precio * p.cantidad
                val nombreTienda = mapaTiendas.getOrDefault(p.tiendaId, "Otra")

                totalPorLista[lId] = totalPorLista.getOrDefault(lId, 0.0) + subtotal

                val mTiendas = productosPorListaYTienda.getOrPut(lId) { mutableMapOf() }
                mTiendas[nombreTienda] = mTiendas.getOrDefault(nombreTienda, 0.0) + subtotal
            }

            // Agrupar por Año -> Mes
            val estructuraAnios = TreeMap<String, MutableMap<String, EstadisticaModel.MesModel>>(Collections.reverseOrder())

            val fmtLecturaFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fmtAnio = SimpleDateFormat("yyyy", Locale.getDefault())
            val fmtMesClave = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val fmtMesNombre = SimpleDateFormat("MMMM", Locale("es", "ES"))

            for (lista in listasActivas) {
                try {
                    val fecha = lista.fechaCreacion.let { 
                        if (it.isNotEmpty()) fmtLecturaFecha.parse(it) else null 
                    } ?: Date()

                    val anioStr = fmtAnio.format(fecha)
                    val mesClave = fmtMesClave.format(fecha)
                    var mesNombre = fmtMesNombre.format(fecha)

                    if (mesNombre.isNotEmpty()) {
                        mesNombre = mesNombre.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    }

                    val totalLista = totalPorLista.getOrDefault(lista.id, 0.0)
                    val desglosTiendasLista = productosPorListaYTienda.getOrDefault(lista.id, mutableMapOf())

                    val mesesDelAnio = estructuraAnios.getOrPut(anioStr) { mutableMapOf() }

                    if (mesesDelAnio.containsKey(mesClave)) {
                        val mesExistente = mesesDelAnio[mesClave]
                        if (mesExistente != null) {
                            val nuevoTotalMes = mesExistente.totalMes + totalLista
                            val tiendasConsolidadas = mesExistente.gastosPorTienda.toMutableMap()

                            for ((key, value) in desglosTiendasLista) {
                                tiendasConsolidadas[key] = tiendasConsolidadas.getOrDefault(key, 0.0) + value
                            }

                            mesesDelAnio[mesClave] = EstadisticaModel.MesModel(mesNombre, mesClave, nuevoTotalMes, tiendasConsolidadas)
                        }
                    } else {
                        mesesDelAnio[mesClave] = EstadisticaModel.MesModel(mesNombre, mesClave, totalLista, desglosTiendasLista.toMap())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Convertir Estructura a Objetos Finales
            val listaAnios = mutableListOf<EstadisticaModel.AnnoModel>()
            for ((anio, meses) in estructuraAnios) {
                val anioObj = EstadisticaModel.AnnoModel(anio)
                var totalAcumuladoAnio = 0.0

                for (mesObj in meses.values) {
                    totalAcumuladoAnio += mesObj.totalMes
                    anioObj.meses.add(mesObj)
                }

                anioObj.totalAnio = totalAcumuladoAnio
                listaAnios.add(anioObj)
            }

            runOnUiThread { mostrarAnios(listaAnios) }
        }
    }

    private fun mostrarAnios(anios: List<EstadisticaModel.AnnoModel>) {
        supportActionBar?.title = "Estadísticas por Año"
        adapterAnio = AnioAdapter(anios, object : AnioAdapter.OnAnioClickListener {
            override fun onAnioClick(anioModel: EstadisticaModel.AnnoModel) {
                mostrarMeses(anioModel)
            }
        })
        binding.rvEstadisticas.adapter = adapterAnio
    }

    private fun mostrarMeses(anio: EstadisticaModel.AnnoModel) {
        supportActionBar?.title = "Meses - ${anio.anio}"
        adapterMes = MesAdapter(anio.meses)
        binding.rvEstadisticas.adapter = adapterMes
    }

    override fun onSupportNavigateUp(): Boolean {
        if (binding.rvEstadisticas.adapter is MesAdapter) {
            cargarDatosEstadisticas()
            return true
        }
        finish()
        return true
    }
}
