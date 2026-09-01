package com.example.despensacx.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.despensacx.data.CatalogoProducto
import com.example.despensacx.model.Categoria
import com.example.despensacx.model.UnidadMedida
import com.example.despensacx.ui.components.BarcodeScannerView
import com.example.despensacx.ui.components.EmptyState
import com.example.despensacx.ui.components.catalogo.CatalogoItem
import com.example.despensacx.ui.components.catalogo.DialogCatalogo
import com.example.despensacx.ui.theme.DespensaCXTheme
import com.example.despensacx.viewmodel.GestionCatalogoViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GestionCatalogoActivity : ComponentActivity() {

    private val viewModel: GestionCatalogoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DespensaCXTheme {
                GestionCatalogoScreen(viewModel = viewModel, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionCatalogoScreen(viewModel: GestionCatalogoViewModel, onBack: () -> Unit) {
    val catalogo by viewModel.catalogo.observeAsState(emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var productoParaEditar by remember { mutableStateOf<CatalogoProducto?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<CatalogoProducto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catálogo de Productos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { productoParaEditar = null; showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Producto")
            }
        }
    ) { padding ->
        if (catalogo.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.ListAlt,
                title = "Tu memoria de compras",
                description = "Aquí aparecerán todos los productos que escanees o registres manualmente."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(catalogo, key = { it.barcode }) { producto ->
                    CatalogoItem(
                        producto = producto,
                        onEditar = { productoParaEditar = producto; showDialog = true },
                        onEliminar = { showDeleteConfirm = producto }
                    )
                }
            }
        }
    }

    if (showDialog) {
        DialogCatalogo(
            producto = productoParaEditar,
            onDismiss = { showDialog = false },
            onConfirm = { prod ->
                viewModel.guardarProducto(prod)
                showDialog = false
            }
        )
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Eliminar del Catálogo") },
            text = { Text("¿Deseas eliminar '${showDeleteConfirm?.nombre}' del catálogo global?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarProducto(showDeleteConfirm!!)
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
