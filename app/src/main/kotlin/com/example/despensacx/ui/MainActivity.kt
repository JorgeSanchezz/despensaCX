package com.example.despensacx.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.despensacx.R
import com.example.despensacx.adapter.ListaAdapter
import com.example.despensacx.data.AppDatabase
import com.example.despensacx.data.BackupHelper
import com.example.despensacx.data.ListaEntity
import com.example.despensacx.data.ProductoEntity
import com.example.despensacx.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), ListaAdapter.OnListaClickListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: ListaAdapter
    private var listaOriginal: List<ListaEntity> = emptyList()
    private var ordenActual = 0 // 0: Fecha, 1: Alfabetico, 2: Presupuesto

    // Lanzador para Exportación (SAF Create Document)
    private val exportarJsonLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    AppDatabase.databaseWriteExecutor.execute {
                        val ok = BackupHelper.exportarJSONToUri(this, uri)
                        runOnUiThread {
                            Toast.makeText(this, if (ok) R.string.respaldo_ok else R.string.error_respaldo, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

    // Lanzador para Importación (SAF Open Document)
    private val importarJsonLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    AppDatabase.databaseWriteExecutor.execute {
                        val ok = BackupHelper.importarJSONFromUri(this, uri)
                        runOnUiThread {
                            Toast.makeText(this, if (ok) "Respaldo restaurado con éxito" else "Error al leer respaldo", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarMain)

        db = AppDatabase.getInstance(this)

        setupRecyclerView()
        setupSearchView()

        binding.fabNuevaLista.setOnClickListener { mostrarDialogoCrearLista(null) }

        // Cargar listas activas desde Room
        db.listaDao().getListasActivas().observe(this) { listas ->
            listaOriginal = listas
            aplicarFiltroYOrden(binding.searchViewListas.query.toString())
        }
    }

    private fun setupRecyclerView() {
        adapter = ListaAdapter(this)
        binding.rvListas.layoutManager = LinearLayoutManager(this)
        binding.rvListas.adapter = adapter

        // Gestos Swipe (Derecha: Archivar, Izquierda: Eliminar)
        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val lista = adapter.getListaAt(position)

                if (direction == ItemTouchHelper.RIGHT) {
                    // Archivar
                    archivarLista(lista, true)
                    Snackbar.make(binding.root, "Lista archivada", Snackbar.LENGTH_LONG)
                        .setAction("DESHACER") { archivarLista(lista, false) }
                        .show()
                } else {
                    // Eliminar
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Eliminar lista")
                        .setMessage("¿Deseas eliminar la lista '${lista.nombre}'?")
                        .setPositiveButton("Eliminar") { _, _ ->
                            AppDatabase.databaseWriteExecutor.execute { db.listaDao().delete(lista) }
                        }
                        .setNegativeButton("Cancelar") { _, _ -> adapter.notifyItemChanged(position) }
                        .show()
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvListas)
    }

    private fun setupSearchView() {
        binding.searchViewListas.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                aplicarFiltroYOrden(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                aplicarFiltroYOrden(newText ?: "")
                return true
            }
        })
    }

    private fun aplicarFiltroYOrden(query: String) {
        val filtradas = listaOriginal.filter {
            it.nombre.contains(query, ignoreCase = true) || it.fechaCreacion.contains(query)
        }.toMutableList()

        when (ordenActual) {
            0 -> filtradas.sortByDescending { it.fechaCreacion }
            1 -> filtradas.sortBy { it.nombre.lowercase() }
            2 -> filtradas.sortByDescending { it.presupuestoMaximo }
        }

        adapter.setListas(filtradas)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_tiendas -> {
                startActivity(Intent(this, GestionTiendasActivity::class.java))
                true
            }
            R.id.action_archivados -> {
                startActivity(Intent(this, ListasArchivadasActivity::class.java))
                true
            }
            R.id.action_estadisticas -> {
                startActivity(Intent(this, EstadisticasActivity::class.java))
                true
            }
            R.id.action_ordenar -> {
                mostrarDialogoOrden()
                true
            }
            R.id.action_respaldo -> {
                gestionarRespaldo()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun mostrarDialogoOrden() {
        val opciones = arrayOf("Fecha de Creación", "Orden Alfabético", "Presupuesto Total")
        AlertDialog.Builder(this)
            .setTitle("Ordenar Listas Por")
            .setSingleChoiceItems(opciones, ordenActual) { dialog, which ->
                ordenActual = which
                aplicarFiltroYOrden(binding.searchViewListas.query.toString())
                dialog.dismiss()
            }
            .show()
    }

    private fun gestionarRespaldo() {
        val opciones = arrayOf("Exportar Respaldo (JSON)", "Importar Respaldo (JSON)")
        AlertDialog.Builder(this)
            .setTitle("Respaldo y Restauración")
            .setItems(opciones) { _, which ->
                if (which == 0) abrirExportadorSAF() else abrirImportadorSAF()
            }
            .show()
    }

    private fun abrirExportadorSAF() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, BackupHelper.generarNombreDefectoRespaldo())
        }
        exportarJsonLauncher.launch(intent)
    }

    private fun abrirImportadorSAF() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        importarJsonLauncher.launch(intent)
    }

    private fun mostrarDialogoCrearLista(listaEditar: ListaEntity?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_nueva_lista, null)
        val etNombre = view.findViewById<EditText>(R.id.etNombreLista)
        val etPresupuesto = view.findViewById<EditText>(R.id.etPresupuestoLista)

        listaEditar?.let {
            etNombre.setText(it.nombre)
            if (it.presupuestoMaximo > 0) {
                etPresupuesto.setText(it.presupuestoMaximo.toString())
            }
        }

        AlertDialog.Builder(this)
            .setView(view)
            .setTitle(if (listaEditar == null) "Nueva Lista de Compras" else "Editar Lista")
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = etNombre.text.toString().trim()
                val presupuestoStr = etPresupuesto.text.toString().trim()
                val presupuesto = if (presupuestoStr.isEmpty()) 0.0 else presupuestoStr.toDouble()

                if (nombre.isEmpty()) {
                    Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val fechaHoraActual = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

                AppDatabase.databaseWriteExecutor.execute {
                    if (listaEditar == null) {
                        val nueva = ListaEntity(nombre = nombre, fechaCreacion = fechaHoraActual, fechaModificacion = fechaHoraActual, archivada = false, presupuestoMaximo = presupuesto)
                        db.listaDao().insert(nueva)
                    } else {
                        listaEditar.nombre = nombre
                        listaEditar.presupuestoMaximo = presupuesto
                        listaEditar.fechaModificacion = fechaHoraActual
                        db.listaDao().update(listaEditar)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun archivarLista(lista: ListaEntity, archivar: Boolean) {
        val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        lista.archivada = archivar
        lista.fechaModificacion = fecha
        AppDatabase.databaseWriteExecutor.execute { db.listaDao().update(lista) }
    }

    override fun onClick(lista: ListaEntity) {
        val intent = Intent(this, DetalleListaActivity::class.java).apply {
            putExtra("EXTRA_LISTA_ID", lista.id)
        }
        startActivity(intent)
    }

    override fun onLongClick(lista: ListaEntity, view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add("Archivar Lista")
        popup.menu.add("Duplicar Lista")
        popup.menu.add("Eliminar Lista")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Archivar Lista" -> archivarLista(lista, true)
                "Duplicar Lista" -> onDuplicar(lista)
                "Eliminar Lista" -> onEliminar(lista)
            }
            true
        }
        popup.show()
    }

    override fun onEditar(lista: ListaEntity) {
        mostrarDialogoCrearLista(lista)
    }

    override fun onDuplicar(lista: ListaEntity) {
        AppDatabase.databaseWriteExecutor.execute {
            val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val copia = ListaEntity(nombre = lista.nombre + " (Copia)", fechaCreacion = fecha, fechaModificacion = fecha, archivada = false, presupuestoMaximo = lista.presupuestoMaximo)
            val nuevaId = db.listaDao().insert(copia)

            val productos = db.productoDao().getProductosByListaSync(lista.id)
            for (p in productos) {
                val pCopia = ProductoEntity(listaId = nuevaId, tiendaId = p.tiendaId, descripcion = p.descripcion, precio = p.precio, cantidad = p.cantidad, seleccionado = p.seleccionado)
                db.productoDao().insert(pCopia)
            }
        }
    }

    override fun onEliminar(lista: ListaEntity) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar lista")
            .setMessage("¿Estás seguro de eliminar esta lista?")
            .setPositiveButton("Eliminar") { _, _ ->
                AppDatabase.databaseWriteExecutor.execute { db.listaDao().delete(lista) }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
