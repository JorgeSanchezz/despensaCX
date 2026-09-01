package com.example.despensacx.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.despensacx.R
import com.example.despensacx.data.*
import com.example.despensacx.model.Categoria
import com.example.despensacx.model.ProductoConTienda
import com.example.despensacx.model.UnidadMedida
import com.example.despensacx.ui.components.BarcodeScannerView
import com.example.despensacx.ui.components.EmptyState
import com.example.despensacx.ui.components.detalle.HeaderTienda
import com.example.despensacx.ui.components.detalle.ProductoItem
import com.example.despensacx.ui.components.detalle.SheetProducto
import com.example.despensacx.ui.theme.DespensaCXTheme
import com.example.despensacx.utils.FormatUtils
import com.example.despensacx.utils.PdfGenerator
import com.example.despensacx.viewmodel.DetalleListaViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class DetalleListaActivity : ComponentActivity() {

    private val viewModel: DetalleListaViewModel by viewModels()

    @Inject
    lateinit var db: AppDatabase
    
    @Inject
    lateinit var listaDao: ListaDao
    @Inject
    lateinit var productoDao: ProductoDao
    @Inject
    lateinit var tiendaDao: TiendaDao

    private var listaId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listaId = intent.getLongExtra("EXTRA_LISTA_ID", -1)
        if (listaId == -1L) {
            finish()
            return
        }

        setContent {
            DespensaCXTheme {
                DetalleListaScreen(
                    viewModel = viewModel,
                    listaId = listaId,
                    onBack = { finish() },
                    onShare = { compartirLista() }
                )
            }
        }
    }

    private fun compartirLista() {
        AppDatabase.databaseWriteExecutor.execute {
            val listaActual = listaDao.getByIdSync(listaId)
            val prods = productoDao.getProductosByListaSync(listaId)
            val tiendas = tiendaDao.getAllTiendasSync()
            val sb = StringBuilder()
            sb.append("🛒 *").append(listaActual?.nombre ?: "").append("*\n\n")

            val mapa = prods.groupBy { it.tiendaId }

            for (t in tiendas) {
                mapa[t.id]?.let { productosTienda ->
                    sb.append("📍 *").append(t.nombre).append("*\n")
                    for (p in productosTienda) {
                        val check = if (p.seleccionado) "[x]" else "[ ]"
                        val cantStr = if (p.cantidad % 1.0 == 0.0) p.cantidad.toInt().toString() else p.cantidad.toString()
                        sb.append(check).append(" ").append(cantStr).append("x ").append(p.descripcion).append("\n")
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetalleListaScreen(
    viewModel: DetalleListaViewModel,
    listaId: Long,
    onBack: () -> Unit,
    onShare: () -> Unit
) {
    val listaActual by viewModel.getLista(listaId).observeAsState()
    val tiendas by viewModel.getTiendas().observeAsState(emptyList())
    val productos by viewModel.getProductos(listaId).observeAsState(emptyList())
    val ticketFotos by viewModel.getTicketFotos(listaId).observeAsState(emptyList())

    val context = LocalContext.current
    var showAddSheet by remember { mutableStateOf(false) }
    var showNoTiendasDialog by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }
    var showStorePickerForPhoto by remember { mutableStateOf(false) }
    var supermarketMode by remember { mutableStateOf(false) }
    
    var photoUriByCamera by remember { mutableStateOf<Uri?>(null) }
    var pendingTiendaIdForPhoto by remember { mutableLongStateOf(-1L) }
    var selectedPhotoViewer by remember { mutableStateOf<TicketFotoEntity?>(null) }
    var productoParaEditar by remember { mutableStateOf<ProductoEntity?>(null) }
    
    // Estado para colapsar/expandir tiendas
    val collapsedStores = remember { mutableStateMapOf<Long, Boolean>() }

    // Launcher para la cámara (Tickets reales)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUriByCamera != null && pendingTiendaIdForPhoto != -1L) {
            viewModel.guardarFotoTicket(listaId, pendingTiendaIdForPhoto, photoUriByCamera.toString())
            Toast.makeText(context, "Foto del ticket guardada", Toast.LENGTH_SHORT).show()
        }
    }

    // Feedback visual al cambiar de modo
    LaunchedEffect(supermarketMode) {
        if (supermarketMode) {
            Toast.makeText(context, "Modo Supermercado Activado", Toast.LENGTH_SHORT).show()
        }
    }

    val agrupado = remember(productos, tiendas) {
        productos.map { p ->
            val tienda = tiendas.find { it.id == p.tiendaId }
                ?: TiendaEntity(nombre = "General", color = "#9E9E9E", fechaRegistro = "", orden = 99)
            ProductoConTienda(p, tienda)
        }.groupBy { it.tienda.id }
    }

    val totalGeneral = remember(productos) {
        productos.filter { it.seleccionado }.sumOf { it.precio * it.cantidad }
    }
    
    val progreso = remember(productos, listaActual, totalGeneral) {
        val max = listaActual?.presupuestoMaximo ?: 0.0
        if (max > 0.0) {
            (totalGeneral / max).toFloat().coerceIn(0f, 1f)
        } else {
            if (productos.isEmpty()) 0f else productos.count { it.seleccionado }.toFloat() / productos.size
        }
    }

    val progresoAnimated by animateFloatAsState(
        targetValue = progreso,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "progreso_barra"
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Detalle de Lista")
                            if (supermarketMode) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text("MODO SÚPER", modifier = Modifier.padding(horizontal = 4.dp))
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                        }
                    },
                    actions = {
                        IconButton(onClick = { supermarketMode = !supermarketMode }) {
                            Icon(
                                if (supermarketMode) Icons.Default.ShoppingCartCheckout else Icons.Default.ShoppingCart,
                                contentDescription = "Modo Supermercado"
                            )
                        }
                        if (!supermarketMode) {
                            IconButton(onClick = {
                                val tiendasEnUsoIds = agrupado.keys.toList()
                                if (tiendasEnUsoIds.isEmpty()) {
                                    Toast.makeText(context, "No hay productos en la lista", Toast.LENGTH_SHORT).show()
                                } else if (tiendasEnUsoIds.size == 1) {
                                    capturarFotoTicket(context, tiendasEnUsoIds[0]) { uri, tId ->
                                        photoUriByCamera = uri
                                        pendingTiendaIdForPhoto = tId
                                        cameraLauncher.launch(uri)
                                    }
                                } else {
                                    showStorePickerForPhoto = true
                                }
                            }) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = "Tomar foto del ticket")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (supermarketMode) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = if (supermarketMode) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = if (supermarketMode) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = if (supermarketMode) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                
                // Info y Presupuesto
                AnimatedVisibility(
                    visible = !supermarketMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    listaActual?.let { lista ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = lista.nombre + if (lista.archivada) stringResource(R.string.archivada_suffix) else "",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "Artículos: ${productos.count { it.seleccionado }}/${productos.size}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (lista.presupuestoMaximo > 0) {
                                    val excedido = totalGeneral > lista.presupuestoMaximo
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = FormatUtils.formatCurrency(totalGeneral),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (excedido) Color.Red else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "/ ${FormatUtils.formatCurrency(lista.presupuestoMaximo)}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            val maxPresupuesto = listaActual?.presupuestoMaximo ?: 0.0
                        val estaExcedido = maxPresupuesto > 0 && totalGeneral > maxPresupuesto

                        LinearProgressIndicator(
                            progress = progresoAnimated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = if (estaExcedido) Color.Red else MaterialTheme.colorScheme.primary,
                            trackColor = if (estaExcedido) Color.Red.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer
                        )
                        }
                    }
                }
                
                // Barra de Acción Global
                AnimatedVisibility(visible = !supermarketMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val allSelected = productos.isNotEmpty() && productos.all { it.seleccionado }
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { isChecked ->
                                viewModel.seleccionarTodos(listaId, isChecked, listaActual)
                            }
                        )
                        Text("Seleccionar todos", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                        TextButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Compartir")
                        }
                        IconButton(onClick = { showPdfDialog = true }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar PDF")
                        }
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !supermarketMode,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TOTAL SELECCIONADO",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Text(
                                text = FormatUtils.formatCurrency(totalGeneral),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    if (tiendas.isEmpty()) {
                        showNoTiendasDialog = true
                    } else {
                        productoParaEditar = null
                        showAddSheet = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Producto")
            }
        }
    ) { padding ->
        if (productos.isEmpty()) {
            EmptyState(
                icon = Icons.Default.PostAdd,
                title = "Lista vacía",
                description = "Presiona el botón + para agregar productos a tu lista."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                agrupado.forEach { (_, listaProds) ->
                    val firstPct = listaProds.first()
                    val subtotalTienda = listaProds.filter { it.producto.seleccionado }.sumOf { it.producto.precio * it.producto.cantidad }

                    val isCollapsed = collapsedStores[firstPct.tienda.id] ?: false

                    stickyHeader {
                        val fotoTienda = ticketFotos.find { it.tiendaId == firstPct.tienda.id }
                        HeaderTienda(
                            nombre = firstPct.tienda.nombre,
                            color = firstPct.tienda.color,
                            subtotal = subtotalTienda,
                            fotoTicket = fotoTienda,
                            compactMode = supermarketMode,
                            isCollapsed = isCollapsed,
                            onToggleCollapse = { collapsedStores[firstPct.tienda.id] = !isCollapsed },
                            onVerFoto = { selectedPhotoViewer = it }
                        )
                    }

                    if (!isCollapsed) {
                        items(listaProds, key = { it.producto.id }) { pct ->
                            ProductoItem(
                                pct = pct,
                                compactMode = supermarketMode,
                                onToggle = { seleccionado ->
                                    viewModel.toggleProducto(pct.producto, seleccionado, listaActual)
                                },
                                onEditar = {
                                    productoParaEditar = it
                                    showAddSheet = true
                                },
                                onEliminar = {
                                    viewModel.eliminarProducto(it, listaActual)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        SheetProducto(
            producto = productoParaEditar,
            tiendas = tiendas,
            viewModel = viewModel,
            onDismiss = { showAddSheet = false },
            onConfirm = { desc, precio, cant, tiendaId, barcode, categoria, unidad ->
                viewModel.guardarProducto(
                    productoParaEditar,
                    listaId,
                    tiendaId,
                    desc,
                    precio,
                    cant,
                    listaActual,
                    barcode,
                    categoria,
                    unidad
                )
                showAddSheet = false
            }
        )
    }

    if (showNoTiendasDialog) {
        AlertDialog(
            onDismissRequest = { showNoTiendasDialog = false },
            title = { Text("Tienda Requerida") },
            text = { Text("Para agregar un producto, primero debes registrar al menos una tienda (ej. Walmart, Mercado, etc.).") },
            confirmButton = {
                Button(onClick = {
                    showNoTiendasDialog = false
                    context.startActivity(Intent(context, GestionTiendasActivity::class.java))
                }) {
                    Text("Gestionar Tiendas")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoTiendasDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showPdfDialog) {
        AlertDialog(
            onDismissRequest = { showPdfDialog = false },
            title = { Text("Exportar Ticket PDF") },
            text = {
                Column {
                    Text("Selecciona el formato del ticket:")
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showPdfDialog = false
                            listaActual?.let { lista ->
                                val uri = PdfGenerator.generarTicketPDF(context, lista, agrupado)
                                uri?.let { abrirPDF(context, it) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("TICKET COMPLETO")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("O por tienda:", style = MaterialTheme.typography.labelSmall)
                    tiendas.forEach { tienda ->
                        if (agrupado.containsKey(tienda.id)) {
                            OutlinedButton(
                                onClick = {
                                    showPdfDialog = false
                                    listaActual?.let { lista ->
                                        val uri = PdfGenerator.generarTicketPDF(context, lista, agrupado, tienda.id)
                                        uri?.let { abrirPDF(context, it) }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(tienda.nombre.uppercase(), overflow = TextOverflow.Ellipsis, maxLines = 1)
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPdfDialog = false }) { Text("CANCELAR") }
            }
        )
    }

    if (showStorePickerForPhoto) {
        AlertDialog(
            onDismissRequest = { showStorePickerForPhoto = false },
            title = { Text("¿De qué tienda es el ticket?") },
            text = {
                Column {
                    agrupado.forEach { (tiendaId, prods) ->
                        val tiendaNombre = prods.firstOrNull()?.tienda?.nombre ?: "Tienda"
                        TextButton(
                            onClick = {
                                showStorePickerForPhoto = false
                                capturarFotoTicket(context, tiendaId) { uri, tId ->
                                    photoUriByCamera = uri
                                    pendingTiendaIdForPhoto = tId
                                    cameraLauncher.launch(uri)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tiendaNombre)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (selectedPhotoViewer != null) {
        AlertDialog(
            onDismissRequest = { selectedPhotoViewer = null },
            title = { Text("Ticket Real") },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                    AsyncImage(
                        model = selectedPhotoViewer!!.fotoPath,
                        contentDescription = "Foto del ticket",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPhotoViewer = null }) { Text("Cerrar") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarFotoTicket(selectedPhotoViewer!!)
                        selectedPhotoViewer = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Eliminar Foto")
                }
            }
        )
    }
}

private fun capturarFotoTicket(context: android.content.Context, tiendaId: Long, onReady: (Uri, Long) -> Unit) {
    val ticketsDir = File(context.filesDir, "tickets")
    if (!ticketsDir.exists()) ticketsDir.mkdirs()
    
    val photoFile = File(ticketsDir, "ticket_${tiendaId}_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
    onReady(uri, tiendaId)
}

private fun abrirPDF(context: android.content.Context, uri: android.net.Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Abrir Ticket"))
}
