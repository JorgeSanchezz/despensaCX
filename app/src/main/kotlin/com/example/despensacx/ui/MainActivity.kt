package com.example.despensacx.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.despensacx.R
import com.example.despensacx.data.AppDatabase
import com.example.despensacx.data.ListaEntity
import com.example.despensacx.ui.components.EmptyState
import com.example.despensacx.ui.components.listas.ListaItem
import com.example.despensacx.ui.components.listas.SwipeableListaItem
import com.example.despensacx.ui.theme.DespensaCXTheme
import com.example.despensacx.utils.ZipBackupHelper
import com.example.despensacx.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var db: AppDatabase

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted
            } else {
                Toast.makeText(this, "El permiso de cámara es necesario para el escáner", Toast.LENGTH_LONG).show()
            }
        }

    private val exportarZipLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    AppDatabase.databaseWriteExecutor.execute {
                        val ok = ZipBackupHelper.exportarRespaldoCompleto(this, uri)
                        runOnUiThread {
                            Toast.makeText(this, if (ok) "Respaldo completo (.zip) exportado con éxito" else "Error al exportar respaldo", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

    private val importarRespaldoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    AppDatabase.databaseWriteExecutor.execute {
                        val ok = ZipBackupHelper.importarRespaldo(this, uri)
                        runOnUiThread {
                            Toast.makeText(this, if (ok) "Respaldo restaurado con éxito" else "Error al leer respaldo", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            DespensaCXTheme {
                MainScreen(
                    viewModel = viewModel,
                    onExport = { abrirExportadorSAF() },
                    onImport = { abrirImportadorSAF() },
                    onNavigateToTiendas = { startActivity(Intent(this, GestionTiendasActivity::class.java)) },
                    onNavigateToCatalogo = { startActivity(Intent(this, GestionCatalogoActivity::class.java)) },
                    onNavigateToMembresias = { startActivity(Intent(this, GestionMembresiasActivity::class.java)) },
                    onNavigateToArchivados = { startActivity(Intent(this, ListasArchivadasActivity::class.java)) },
                    onNavigateToEstadisticas = { startActivity(Intent(this, EstadisticasActivity::class.java)) },
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

    private fun abrirExportadorSAF() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, ZipBackupHelper.generarNombreDefectoRespaldo())
        }
        exportarZipLauncher.launch(intent)
    }

    private fun abrirImportadorSAF() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/json", "application/octet-stream"))
        }
        importarRespaldoLauncher.launch(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onNavigateToTiendas: () -> Unit,
    onNavigateToCatalogo: () -> Unit,
    onNavigateToMembresias: () -> Unit,
    onNavigateToArchivados: () -> Unit,
    onNavigateToEstadisticas: () -> Unit,
    onNavigateToDetalle: (Long) -> Unit
) {
    val context = LocalContext.current
    val listasActivas by viewModel.listasActivas.observeAsState(emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var ordenActual by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showOrderDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var listaParaEditar by remember { mutableStateOf<ListaEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<ListaEntity?>(null) }
    var showDuplicateConfirm by remember { mutableStateOf<ListaEntity?>(null) }

    val filtradas = remember(listasActivas, searchQuery, ordenActual) {
        listasActivas.filter {
            it.nombre.contains(searchQuery, ignoreCase = true) || it.fechaCreacion.contains(searchQuery)
        }.let {
            when (ordenActual) {
                0 -> it.sortedByDescending { l -> l.fechaCreacion }
                1 -> it.sortedBy { l -> l.nombre.lowercase() }
                2 -> it.sortedByDescending { l -> l.presupuestoMaximo }
                else -> it
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mis Listas de Compras", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = { showOrderDialog = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Ordenar")
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Gestionar Tiendas") },
                            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                            onClick = { showMenu = false; onNavigateToTiendas() }
                        )
                        DropdownMenuItem(
                            text = { Text("Gestionar Catálogo") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null) },
                            onClick = { showMenu = false; onNavigateToCatalogo() }
                        )
                        DropdownMenuItem(
                            text = { Text("Gestionar Membresías") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            onClick = { showMenu = false; onNavigateToMembresias() }
                        )
                        DropdownMenuItem(
                            text = { Text("Listas Archivadas") },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                            onClick = { showMenu = false; onNavigateToArchivados() }
                        )
                        DropdownMenuItem(
                            text = { Text("Estadísticas") },
                            leadingIcon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                            onClick = { showMenu = false; onNavigateToEstadisticas() }
                        )
                        DropdownMenuItem(
                            text = { Text("Comparar Precios") },
                            leadingIcon = { Icon(Icons.Default.CompareArrows, contentDescription = null) },
                            onClick = { 
                                showMenu = false
                                context.startActivity(Intent(context, ComparadorPreciosActivity::class.java)) 
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Respaldo y Restauración") },
                            leadingIcon = { Icon(Icons.Default.Backup, contentDescription = null) },
                            onClick = { showMenu = false; showBackupDialog = true }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { listaParaEditar = null; showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Lista")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                placeholder = { Text("Buscar por nombre o fecha...") },
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

            if (listasActivas.isEmpty() && searchQuery.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.ShoppingBag,
                    title = "¿Todo listo para comprar?",
                    description = "Aún no tienes listas activas. Crea una nueva para empezar a organizar tus compras."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(filtradas, key = { it.id }) { lista ->
                        SwipeableListaItem(
                            lista = lista,
                            onClick = { onNavigateToDetalle(lista.id) },
                            onEditar = { listaParaEditar = it; showCreateDialog = true },
                            onDuplicar = { showDuplicateConfirm = it },
                            onEliminar = { showDeleteConfirm = it },
                            onArchivar = { 
                                viewModel.archivarLista(it, true)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Lista archivada",
                                        actionLabel = "DESHACER",
                                        duration = SnackbarDuration.Long
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.archivarLista(it, false)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Diálogos
    if (showOrderDialog) {
        AlertDialog(
            onDismissRequest = { showOrderDialog = false },
            title = { Text("Ordenar Listas Por") },
            text = {
                Column {
                    val opciones = listOf("Fecha de Creación", "Orden Alfabético", "Presupuesto Total")
                    opciones.forEachIndexed { index, title ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    ordenActual = index
                                    showOrderDialog = false
                                }
                                .padding(16.dp)
                        ) {
                            RadioButton(selected = ordenActual == index, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(title)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Respaldo y Restauración") },
            text = {
                Column {
                    TextButton(onClick = { showBackupDialog = false; onExport() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Exportar Respaldo Completo (.zip)")
                    }
                    TextButton(onClick = { showBackupDialog = false; onImport() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Importar Respaldo (.zip o .json)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showCreateDialog) {
        var nombre by remember { mutableStateOf(listaParaEditar?.nombre ?: "") }
        var presupuesto by remember { 
            val p = listaParaEditar?.presupuestoMaximo ?: 0.0
            mutableStateOf(if (p == 0.0) "" else if (p % 1.0 == 0.0) p.toInt().toString() else p.toString())
        }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(if (listaParaEditar == null) "Nueva Lista de Compras" else "Editar Lista") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre de la lista") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = presupuesto,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) presupuesto = it },
                        label = { Text("Presupuesto Máximo (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        prefix = { Text("$ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nombre.isNotBlank()) {
                        viewModel.guardarLista(listaParaEditar, nombre, presupuesto.toDoubleOrNull() ?: 0.0)
                        showCreateDialog = false
                    }
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Eliminar lista") },
            text = { Text("¿Deseas eliminar la lista '${showDeleteConfirm?.nombre}'?") },
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

    if (showDuplicateConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDuplicateConfirm = null },
            title = { Text("Duplicar lista") },
            text = { Text("¿Deseas crear una copia de la lista '${showDuplicateConfirm?.nombre}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.duplicarLista(showDuplicateConfirm!!)
                        showDuplicateConfirm = null
                    }
                ) {
                    Text("Duplicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateConfirm = null }) { Text("Cancelar") }
            }
        )
    }
}
