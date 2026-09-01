package com.example.despensacx.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.despensacx.data.ListaEntity
import com.example.despensacx.ui.components.EmptyState
import com.example.despensacx.ui.components.listas.ListaItem
import com.example.despensacx.ui.theme.DespensaCXTheme

import androidx.activity.viewModels
import com.example.despensacx.viewmodel.ListasArchivadasViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ListasArchivadasActivity : ComponentActivity() {

    private val viewModel: ListasArchivadasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DespensaCXTheme {
                ListasArchivadasScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onNavigateToDetalle = { id ->
                        val intent = Intent(this, DetalleListaActivity::class.java).apply {
                            putExtra("EXTRA_LISTA_ID", id)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListasArchivadasScreen(
    viewModel: ListasArchivadasViewModel,
    onBack: () -> Unit,
    onNavigateToDetalle: (Long) -> Unit
) {
    val listasArchivadas by viewModel.listasArchivadas.observeAsState(emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf<ListaEntity?>(null) }
    var showRestoreConfirm by remember { mutableStateOf<ListaEntity?>(null) }

    val filtradas = remember(listasArchivadas, searchQuery) {
        listasArchivadas.filter {
            it.nombre.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listas Archivadas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                placeholder = { Text("Buscar en archivados...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar")
                        }
                    }
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            if (listasArchivadas.isEmpty() && searchQuery.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Archive,
                    title = "Archivo vacío",
                    description = "Aquí aparecerán las listas que ya no necesites tener a la mano."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filtradas, key = { it.id }) { lista ->
                        ListaItem(
                            lista = lista,
                            onClick = { onNavigateToDetalle(lista.id) },
                            onEliminar = { showDeleteConfirm = lista }
                        )
                        // Additional Actions for Archived Items (Restore)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showRestoreConfirm = lista }) {
                                Text("DESARCHIVAR")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRestoreConfirm != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = null },
            title = { Text("Desarchivar Lista") },
            text = { Text("¿Deseas devolver la lista '${showRestoreConfirm?.nombre}' a las activas?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.desarchivarLista(showRestoreConfirm!!)
                    showRestoreConfirm = null
                }) {
                    Text("Desarchivar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = null }) { Text("Cancelar") }
            }
        )
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Eliminar definitivamente") },
            text = { Text("¿Estás seguro de eliminar '${showDeleteConfirm?.nombre}'? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarLista(showDeleteConfirm!!)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancelar") }
            }
        )
    }
}
