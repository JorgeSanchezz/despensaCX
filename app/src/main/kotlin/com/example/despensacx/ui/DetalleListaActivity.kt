package com.example.despensacx.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.despensacx.R
import com.example.despensacx.adapter.ProductoAdapter
import com.example.despensacx.data.AppDatabase
import com.example.despensacx.data.ListaEntity
import com.example.despensacx.data.ProductoEntity
import com.example.despensacx.data.TiendaEntity
import com.example.despensacx.databinding.ActivityDetalleListaBinding
import com.example.despensacx.model.ProductoConTienda
import java.text.SimpleDateFormat
import java.util.*

class DetalleListaActivity : AppCompatActivity(), ProductoAdapter.OnProductoActionListener {

    private lateinit var binding: ActivityDetalleListaBinding
    private lateinit var db: AppDatabase
    private var listaId: Long = -1
    private var listaActual: ListaEntity? = null
    private lateinit var adapter: ProductoAdapter
    private var catalogoTiendas: List<TiendaEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleListaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarDetalle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        listaId = intent.getLongExtra("EXTRA_LISTA_ID", -1)
        if (listaId == -1L) {
            finish()
            return
        }

        db = AppDatabase.getInstance(this)

        adapter = ProductoAdapter(this)
        binding.rvProductos.layoutManager = LinearLayoutManager(this)
        binding.rvProductos.adapter = adapter

        binding.fabAgregarProducto.setOnClickListener { mostrarDialogoProducto(null) }

        binding.cbSeleccionarTodos.setOnCheckedChangeListener { _, isChecked ->
            AppDatabase.databaseWriteExecutor.execute {
                db.productoDao().setAllSeleccionadoSync(listaId, isChecked)
                actualizarFechaModificacion()
            }
        }

        binding.btnCompartirLista.setOnClickListener { compartirLista() }

        // Cargar Lista y Tiendas
        db.listaDao().getById(listaId).observe(this) { lista ->
            lista?.let {
                listaActual = it
                actualizarEncabezado()
            }
        }

        db.tiendaDao().getAllTiendas().observe(this) { tiendas ->
            catalogoTiendas = tiendas
            cargarProductosYAgrupar()
        }
    }

    private fun cargarProductosYAgrupar() {
        db.productoDao().getProductosByLista(listaId).observe(this) { productos ->
            val agrupado = mutableMapOf<Long, MutableList<ProductoConTienda>>()
            var totalGeneral = 0.0

            for (p in productos) {
                val tEncontrada = catalogoTiendas.find { it.id == p.tiendaId } 
                    ?: TiendaEntity(nombre = "General", color = "#9E9E9E", fechaRegistro = "", orden = 99)

                val pct = ProductoConTienda(p, tEncontrada)
                agrupado.getOrPut(pct.tienda.id) { mutableListOf() }.add(pct)

                if (p.seleccionado) {
                    totalGeneral += p.precio * p.cantidad
                }
            }

            // Construir Lista plana para Adapter con Headers
            val itemsDisplay = mutableListOf<ProductoAdapter.DisplayItem>()
            for ((_, listaProds) in agrupado) {
                val subtotalTienda = listaProds.filter { it.producto.seleccionado }.sumOf { it.producto.precio * it.producto.cantidad }

                val firstPct = listaProds[0]
                itemsDisplay.add(ProductoAdapter.DisplayItem(firstPct.tienda.nombre, firstPct.tienda.color, subtotalTienda))

                for (pct in listaProds) {
                    itemsDisplay.add(ProductoAdapter.DisplayItem(pct))
                }
            }

            adapter.setItems(itemsDisplay)
            binding.tvGranTotal.text = String.format(Locale.getDefault(), "$%,.2f MXN", totalGeneral)
            actualizarBarraPresupuesto(totalGeneral)
        }
    }

    private fun actualizarEncabezado() {
        listaActual?.let {
            val titulo = it.nombre + if (it.archivada) getString(R.string.archivada_suffix) else ""
            binding.tvNombreDetalle.text = titulo
            binding.tvModificadoDetalle.text = getString(R.string.modificado_con_fecha, it.fechaModificacion)
        }
    }

    private fun actualizarBarraPresupuesto(totalAcumulado: Double) {
        val max = listaActual?.presupuestoMaximo ?: 0.0

        if (max > 0) {
            binding.tvTextoPresupuesto.text = String.format(Locale.getDefault(), "$%,.2f / $%,.2f MXN", totalAcumulado, max)
            val porcentaje = ((totalAcumulado / max) * 100).toInt()
            binding.progressPresupuesto.progress = Math.min(porcentaje, 100)

            if (totalAcumulado > max) {
                binding.tvEstadoPresupuesto.setText(R.string.excedido)
                binding.tvEstadoPresupuesto.setTextColor(Color.RED)
                binding.progressPresupuesto.setIndicatorColor(Color.RED)
            } else {
                binding.tvEstadoPresupuesto.setText(R.string.dentro_del_limite)
                binding.tvEstadoPresupuesto.setTextColor(Color.parseColor("#2E7D32"))
                binding.progressPresupuesto.setIndicatorColor(Color.parseColor("#2E7D32"))
            }
        } else {
            binding.progressPresupuesto.progress = 100
            binding.progressPresupuesto.setIndicatorColor(Color.GRAY)
        }
    }

    private fun mostrarDialogoProducto(productoEditar: ProductoEntity?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_nuevo_producto, null)

        val etDesc = view.findViewById<EditText>(R.id.etDescripcionProducto)
        val etPrecio = view.findViewById<EditText>(R.id.etPrecioProducto)
        val etCantidad = view.findViewById<EditText>(R.id.etCantidadProducto)
        val spinner = view.findViewById<Spinner>(R.id.spinnerTiendasProducto)

        val nombresTiendas = mutableListOf("Seleccionar tienda")
        nombresTiendas.addAll(catalogoTiendas.map { it.nombre })

        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresTiendas)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapterSpinner

        productoEditar?.let {
            etDesc.setText(it.descripcion)
            etPrecio.setText(it.precio.toString())
            etCantidad.setText(it.cantidad.toString())
            val index = catalogoTiendas.indexOfFirst { t -> t.id == it.tiendaId }
            if (index != -1) spinner.setSelection(index + 1)
        }

        AlertDialog.Builder(this)
            .setView(view)
            .setTitle(if (productoEditar == null) "Agregar Producto" else "Editar Producto")
            .setPositiveButton("Guardar") { _, _ ->
                val desc = etDesc.text.toString().trim()
                val precioStr = etPrecio.text.toString().trim()
                val cantStr = etCantidad.text.toString().trim()
                val posTienda = spinner.selectedItemPosition

                if (desc.isEmpty()) {
                    Toast.makeText(this, "Descripción requerida", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (posTienda == 0) {
                    Toast.makeText(this, "Debes seleccionar una tienda válida", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val precio = if (precioStr.isEmpty()) 0.0 else precioStr.toDouble()
                val cantidad = if (cantStr.isEmpty()) 1 else cantStr.toInt()
                val tiendaIdSelected = catalogoTiendas[posTienda - 1].id

                AppDatabase.databaseWriteExecutor.execute {
                    if (productoEditar == null) {
                        val p = ProductoEntity(listaId = listaId, tiendaId = tiendaIdSelected, descripcion = desc, precio = precio, cantidad = cantidad, seleccionado = true)
                        db.productoDao().insert(p)
                    } else {
                        productoEditar.descripcion = desc
                        productoEditar.precio = precio
                        productoEditar.cantidad = cantidad
                        productoEditar.tiendaId = tiendaIdSelected
                        db.productoDao().update(productoEditar)
                    }
                    actualizarFechaModificacion()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarFechaModificacion() {
        listaActual?.let {
            val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            it.fechaModificacion = fecha
            db.listaDao().update(it)
        }
    }

    private fun compartirLista() {
        AppDatabase.databaseWriteExecutor.execute {
            val prods = db.productoDao().getProductosByListaSync(listaId)
            val sb = StringBuilder()
            sb.append("🛒 *").append(listaActual?.nombre ?: "").append("*\n\n")

            val mapa = prods.groupBy { it.tiendaId }

            for (t in catalogoTiendas) {
                mapa[t.id]?.let { productosTienda ->
                    sb.append("📍 *").append(t.nombre).append("*\n")
                    for (p in productosTienda) {
                        val check = if (p.seleccionado) "[x]" else "[ ]"
                        sb.append(check).append(" ").append(p.cantidad).append("x ").append(p.descripcion).append("\n")
                    }
                    sb.append("\n")
                }
            }

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, sb.toString())
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, "Compartir Lista"))
        }
    }

    override fun onToggleSeleccion(producto: ProductoEntity, seleccionado: Boolean) {
        producto.seleccionado = seleccionado
        AppDatabase.databaseWriteExecutor.execute {
            db.productoDao().update(producto)
            actualizarFechaModificacion()
        }
    }

    override fun onEditar(producto: ProductoEntity) {
        mostrarDialogoProducto(producto)
    }

    override fun onEliminar(producto: ProductoEntity) {
        AppDatabase.databaseWriteExecutor.execute {
            db.productoDao().delete(producto)
            actualizarFechaModificacion()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
