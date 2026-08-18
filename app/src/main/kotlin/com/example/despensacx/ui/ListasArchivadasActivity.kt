package com.example.despensacx.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.despensacx.adapter.ListaAdapter
import com.example.despensacx.data.AppDatabase
import com.example.despensacx.data.ListaEntity
import com.example.despensacx.databinding.ActivityListasArchivadasBinding

class ListasArchivadasActivity : AppCompatActivity(), ListaAdapter.OnListaClickListener {

    private lateinit var binding: ActivityListasArchivadasBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: ListaAdapter
    private var listaArchivadasOriginal: List<ListaEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListasArchivadasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarArchivadas)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = AppDatabase.getInstance(this)

        adapter = ListaAdapter(this)
        binding.rvListasArchivadas.layoutManager = LinearLayoutManager(this)
        binding.rvListasArchivadas.adapter = adapter

        db.listaDao().getListasArchivadas().observe(this) { listas ->
            listaArchivadasOriginal = listas
            filtrar(binding.searchViewArchivadas.query.toString())
        }

        binding.searchViewArchivadas.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filtrar(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filtrar(newText ?: "")
                return true
            }
        })
    }

    private fun filtrar(query: String) {
        val res = listaArchivadasOriginal.filter {
            it.nombre.contains(query, ignoreCase = true)
        }
        adapter.setListas(res)
    }

    override fun onClick(lista: ListaEntity) {
        val intent = Intent(this, DetalleListaActivity::class.java).apply {
            putExtra("EXTRA_LISTA_ID", lista.id)
        }
        startActivity(intent)
    }

    override fun onLongClick(lista: ListaEntity, view: View) {
        AlertDialog.Builder(this)
            .setTitle("Opciones de Archivada")
            .setItems(arrayOf("Desarchivar Lista", "Eliminar definitivamente")) { _, which ->
                if (which == 0) {
                    lista.archivada = false
                    AppDatabase.databaseWriteExecutor.execute { db.listaDao().update(lista) }
                } else {
                    onEliminar(lista)
                }
            }
            .show()
    }

    override fun onEditar(lista: ListaEntity) {}

    override fun onDuplicar(lista: ListaEntity) {}

    override fun onEliminar(lista: ListaEntity) {
        AppDatabase.databaseWriteExecutor.execute { db.listaDao().delete(lista) }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
