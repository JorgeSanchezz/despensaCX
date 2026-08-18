package com.example.despensacx.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.GridLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.despensacx.R
import com.example.despensacx.adapter.TiendaAdapter
import com.example.despensacx.data.AppDatabase
import com.example.despensacx.data.TiendaEntity
import com.example.despensacx.databinding.ActivityGestionTiendasBinding
import java.text.SimpleDateFormat
import java.util.*

class GestionTiendasActivity : AppCompatActivity(), TiendaAdapter.OnTiendaActionListener {

    private lateinit var binding: ActivityGestionTiendasBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: TiendaAdapter
    private var colorSeleccionadoHex = "#4CAF50"

    private val paletaColores = arrayOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#00BCD4", "#009688",
        "#4CAF50", "#8BC34A", "#FFEB3B", "#FF9800",
        "#FF5722", "#795548", "#9E9E9E", "#607D8B"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGestionTiendasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarTiendas)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = AppDatabase.getInstance(this)

        adapter = TiendaAdapter(this)
        binding.rvTiendas.layoutManager = LinearLayoutManager(this)
        binding.rvTiendas.adapter = adapter

        binding.fabNuevaTienda.setOnClickListener { mostrarDialogoTienda(null) }

        db.tiendaDao().getAllTiendas().observe(this) { tiendas ->
            adapter.setTiendas(tiendas)
        }
    }

    private fun mostrarDialogoTienda(tiendaEditar: TiendaEntity?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_nueva_tienda, null)
        val etNombre = view.findViewById<EditText>(R.id.etNombreTienda)
        val gridColor = view.findViewById<GridLayout>(R.id.gridColorPicker)

        colorSeleccionadoHex = tiendaEditar?.color ?: paletaColores[0]
        tiendaEditar?.let { etNombre.setText(it.nombre) }

        for (hex in paletaColores) {
            val colorCircle = View(this)
            val params = GridLayout.LayoutParams()
            params.width = 100
            params.height = 100
            params.setMargins(12, 12, 12, 12)
            colorCircle.layoutParams = params
            colorCircle.setBackgroundColor(Color.parseColor(hex))

            colorCircle.setOnClickListener {
                colorSeleccionadoHex = hex
            }
            gridColor.addView(colorCircle)
        }

        AlertDialog.Builder(this)
            .setView(view)
            .setTitle(if (tiendaEditar == null) "Agregar Nueva Tienda" else "Editar Tienda")
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = etNombre.text.toString().trim()
                if (nombre.isEmpty()) return@setPositiveButton

                val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

                AppDatabase.databaseWriteExecutor.execute {
                    if (tiendaEditar == null) {
                        val nueva = TiendaEntity(nombre = nombre, color = colorSeleccionadoHex, fechaRegistro = fecha, orden = 99)
                        db.tiendaDao().insert(nueva)
                    } else {
                        tiendaEditar.nombre = nombre
                        tiendaEditar.color = colorSeleccionadoHex
                        db.tiendaDao().update(tiendaEditar)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onEditar(tienda: TiendaEntity) {
        mostrarDialogoTienda(tienda)
    }

    override fun onEliminar(tienda: TiendaEntity) {
        AppDatabase.databaseWriteExecutor.execute {
            val count = db.tiendaDao().countProductosByTiendaSync(tienda.id)
            runOnUiThread {
                if (count > 0) {
                    AlertDialog.Builder(this)
                        .setTitle("Acción Bloqueada")
                        .setMessage("No se puede eliminar la tienda porque tiene productos asociados en tus listas.")
                        .setPositiveButton("Aceptar", null)
                        .show()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Eliminar Tienda")
                        .setMessage("¿Deseas eliminar '${tienda.nombre}'?")
                        .setPositiveButton("Eliminar") { _, _ ->
                            AppDatabase.databaseWriteExecutor.execute { db.tiendaDao().delete(tienda) }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
