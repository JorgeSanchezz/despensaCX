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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.despensacx.R
import com.example.despensacx.data.AppDatabase
import com.example.despensacx.data.BackupHelper
import com.example.despensacx.data.ListaEntity
import com.example.despensacx.ui.components.EmptyState
import com.example.despensacx.ui.theme.DespensaCXTheme
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

    private val exportarJsonLauncher =
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

    private val importarJsonLauncher =
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
        
        // Pedir permiso de cámara al inicio si no se tiene
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onNavigateToTiendas: () -> Unit,
    onNavigateToCatalogo: () -> Unit,
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
                            leadingIcon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                            onClick = { showMenu = false; onNavigateToCatalogo() }
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
                singleLine = true
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
                        Text("Exportar Respaldo (JSON)")
                    }
                    TextButton(onClick = { showBackupDialog = false; onImport() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Importar Respaldo (JSON)")
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
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = presupuesto,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) presupuesto = it },
                        label = { Text("Presupuesto Máximo (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        prefix = { Text("$ ") }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableListaItem(
    lista: ListaEntity,
    onClick: () -> Unit,
    onEditar: (ListaEntity) -> Unit,
    onDuplicar: (ListaEntity) -> Unit,
    onEliminar: (ListaEntity) -> Unit,
    onArchivar: (ListaEntity) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onArchivar(lista)
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onEliminar(lista)
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50)
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFF44336)
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .background(color, androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                val icon = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Archive else Icons.Default.Delete
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        },
        content = {
            ListaItem(lista, onClick, onEditar, onDuplicar, onEliminar)
        }
    )
}

@Composable
fun ListaItem(
    lista: ListaEntity,
    onClick: () -> Unit,
    onEditar: ((ListaEntity) -> Unit)? = null,
    onDuplicar: ((ListaEntity) -> Unit)? = null,
    onEliminar: (ListaEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val title = lista.nombre + if (lista.archivada) stringResource(R.string.archivada_suffix) else ""
                Text(
                    text = title, 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Bold, 
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    Icons.Default.ChevronRight, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
            
            Spacer(Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = lista.fechaCreacion,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (lista.presupuestoMaximo > 0) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "Presupuesto: $%,.2f MXN", lista.presupuestoMaximo),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (onEditar != null) {
                    IconButton(onClick = { onEditar(lista) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                if (onDuplicar != null) {
                    IconButton(onClick = { onDuplicar(lista) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicar", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                IconButton(onClick = { onEliminar(lista) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
