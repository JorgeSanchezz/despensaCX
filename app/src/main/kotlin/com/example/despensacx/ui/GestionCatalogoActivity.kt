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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun CatalogoItem(producto: CatalogoProducto, onEditar: () -> Unit, onEliminar: () -> Unit) {
    val categoria = try { Categoria.valueOf(producto.categoria) } catch (e: Exception) { Categoria.GENERAL }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoria.icono,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = producto.nombre,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Código: ${producto.barcode}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onEditar) {
                Icon(Icons.Default.Edit, contentDescription = "Editar")
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogCatalogo(
    producto: CatalogoProducto?,
    onDismiss: () -> Unit,
    onConfirm: (CatalogoProducto) -> Unit
) {
    var barcode by remember { mutableStateOf(producto?.barcode ?: "") }
    var nombre by remember { mutableStateOf(producto?.nombre ?: "") }
    var categoria by remember { mutableStateOf(producto?.categoria ?: "GENERAL") }
    var unidad by remember { mutableStateOf(producto?.unidad ?: "PZA") }
    var showScanner by remember { mutableStateOf(false) }

    if (showScanner) {
        AlertDialog(
            onDismissRequest = { showScanner = false },
            text = {
                Box(modifier = Modifier.size(300.dp)) {
                    BarcodeScannerView { code ->
                        barcode = code
                        showScanner = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showScanner = false }) { Text("Cerrar") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (producto == null) "Agregar al Catálogo" else "Editar Producto") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Código de Barras") },
                        modifier = Modifier.weight(1f),
                        enabled = producto == null,
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (producto == null) {
                        IconButton(onClick = { showScanner = true }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del Producto") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(Modifier.height(16.dp))
                Text("Categoría", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Categoria.entries.forEach { cat ->
                        FilterChip(
                            selected = categoria == cat.name,
                            onClick = { categoria = cat.name },
                            label = { Text(cat.nombre) },
                            leadingIcon = { Icon(cat.icono, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                // Unidades
                Text("Unidad Predeterminada", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UnidadMedida.entries.forEach { un ->
                        FilterChip(
                            selected = unidad == un.name,
                            onClick = { unidad = un.name },
                            label = { Text(un.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (barcode.isNotBlank() && nombre.isNotBlank()) {
                    onConfirm(CatalogoProducto(barcode, nombre, categoria = categoria, unidad = unidad))
                }
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
