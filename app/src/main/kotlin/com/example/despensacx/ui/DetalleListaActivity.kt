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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    
    val progreso = remember(productos) {
        if (productos.isEmpty()) 0f else productos.count { it.seleccionado }.toFloat() / productos.size
    }

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
                            LinearProgressIndicator(
                                progress = progreso,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer
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
    val photoFile = File(context.cacheDir, "ticket_${tiendaId}_${System.currentTimeMillis()}.jpg")
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

@Composable
fun HeaderTienda(
    nombre: String, 
    color: String, 
    subtotal: Double,
    fotoTicket: TicketFotoEntity? = null,
    compactMode: Boolean = false,
    isCollapsed: Boolean = false,
    onToggleCollapse: () -> Unit = {},
    onVerFoto: (TicketFotoEntity) -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleCollapse() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = if (compactMode) 4.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isCollapsed) Icons.Default.ChevronRight else Icons.Default.ExpandMore,
                contentDescription = if (isCollapsed) "Expandir" else "Colapsar",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(if (compactMode) 8.dp else 12.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor(color))
                        } catch (e: Exception) {
                            Color.Gray
                        }
                    )
            )
            Spacer(Modifier.width(if (compactMode) 8.dp else 12.dp))
            Text(
                text = nombre.uppercase(), 
                fontWeight = FontWeight.ExtraBold, 
                modifier = Modifier.weight(1f), 
                style = if (compactMode) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                letterSpacing = 1.sp
            )
            
            if (fotoTicket != null && !compactMode) {
                IconButton(onClick = { onVerFoto(fotoTicket) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.ReceiptLong, 
                        contentDescription = "Ver ticket real",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }

            if (!compactMode) {
                Text(
                    text = FormatUtils.formatCurrency(subtotal),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ProductoItem(
    pct: ProductoConTienda,
    compactMode: Boolean = false,
    onToggle: (Boolean) -> Unit,
    onEditar: (ProductoEntity) -> Unit,
    onEliminar: (ProductoEntity) -> Unit
) {
    val p = pct.producto
    val itemAlpha = if (p.seleccionado) 1.0f else 0.5f
    val textDecoration = if (p.seleccionado) TextDecoration.None else TextDecoration.LineThrough
    val categoria = try { Categoria.valueOf(p.categoria) } catch (e: Exception) { Categoria.GENERAL }
    val unidad = try { UnidadMedida.valueOf(p.unidad).label } catch(e: Exception) { "" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (p.seleccionado) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (p.seleccionado) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(if (compactMode) 16.dp else 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = p.seleccionado,
                onCheckedChange = onToggle,
                modifier = if (compactMode) Modifier.scale(1.5f) else Modifier
            )
            
            if (!compactMode) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = itemAlpha)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoria.icono,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = itemAlpha),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                val cantStr = if (p.cantidad % 1.0 == 0.0) p.cantidad.toInt().toString() else p.cantidad.toString()
                Text(
                    text = if (compactMode) "$cantStr $unidad ${p.descripcion}" else p.descripcion,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compactMode) 20.sp else 16.sp,
                    textDecoration = textDecoration,
                    modifier = Modifier.alpha(itemAlpha),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!compactMode) {
                    Text(
                        text = "$cantStr $unidad x ${FormatUtils.formatCurrency(p.precio)}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.alpha(itemAlpha)
                    )
                }
            }
            
            if (!compactMode) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FormatUtils.formatCurrency(p.precio * p.cantidad),
                        fontWeight = FontWeight.ExtraBold,
                        textDecoration = textDecoration,
                        modifier = Modifier.alpha(itemAlpha),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row {
                        IconButton(onClick = { onEditar(p) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp), tint = Color.Gray)
                        }
                        IconButton(onClick = { onEliminar(p) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(18.dp), tint = Color.Red.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetProducto(
    producto: ProductoEntity?,
    tiendas: List<TiendaEntity>,
    viewModel: DetalleListaViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, Long, String?, String, String) -> Unit
) {
    var desc by remember { mutableStateOf(producto?.descripcion ?: "") }
    var precio by remember { mutableStateOf(if (producto?.precio == 0.0) "" else producto?.precio?.toString() ?: "") }
    var cantidad by remember { 
        val c = producto?.cantidad ?: 1.0
        mutableStateOf(if (c % 1.0 == 0.0) c.toInt().toString() else c.toString())
    }
    var selectedTiendaId by remember { mutableStateOf(producto?.tiendaId ?: if (tiendas.isNotEmpty()) tiendas[0].id else -1L) }
    var barcode by remember { mutableStateOf(producto?.barcode) }
    var categoria by remember { mutableStateOf(producto?.categoria ?: "GENERAL") }
    var unidad by remember { mutableStateOf(producto?.unidad ?: "PZA") }
    
    var showScanner by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var preciosHistoricos by remember { mutableStateOf<List<PrecioHistorico>>(emptyList()) }
    
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (producto == null) "Nuevo Producto" else "Editar Producto",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            
            Spacer(Modifier.height(16.dp))

            if (showScanner) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                ) {
                    BarcodeScannerView(torchEnabled = torchEnabled) { code ->
                        barcode = code
                        showScanner = false
                        scope.launch {
                            viewModel.buscarEnCatalogo(code)?.let {
                                desc = it.nombre
                                categoria = it.categoria
                                unidad = it.unidad
                            }
                            preciosHistoricos = viewModel.getPreciosHistoricos(code)
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        IconButton(
                            onClick = { torchEnabled = !torchEnabled },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { showScanner = false },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("¿Qué vas a comprar?") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showScanner = !showScanner }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear")
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )
            
            if (barcode != null) {
                Text(
                    text = "Código: $barcode",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            
            // Selector de Categoría
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

            // Unidades de Medida
            Text("Unidad", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = precio,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) precio = it },
                    label = { Text("Precio") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    prefix = { Text("$ ") }
                )
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) cantidad = it },
                    label = { Text("Cant.") },
                    modifier = Modifier.weight(0.6f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (preciosHistoricos.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Precios anteriores:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    preciosHistoricos.forEach { hist ->
                        AssistChip(
                            onClick = {
                                precio = hist.precio.toString()
                                selectedTiendaId = hist.tiendaId
                            },
                            label = { Text("${hist.tiendaNombre}: ${FormatUtils.formatCurrency(hist.precio)}") }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            
            // Selector de Tienda
            var expandedTiendas by remember { mutableStateOf(false) }
            val currentTienda = tiendas.find { it.id == selectedTiendaId }
            
            ExposedDropdownMenuBox(
                expanded = expandedTiendas,
                onExpandedChange = { expandedTiendas = it }
            ) {
                OutlinedTextField(
                    value = currentTienda?.nombre ?: "Seleccionar tienda",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tienda") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTiendas) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandedTiendas,
                    onDismissRequest = { expandedTiendas = false }
                ) {
                    tiendas.forEach { tienda ->
                        DropdownMenuItem(
                            text = { Text(tienda.nombre) },
                            onClick = {
                                selectedTiendaId = tienda.id
                                expandedTiendas = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (desc.isNotBlank() && selectedTiendaId != -1L) {
                        onConfirm(desc, precio.toDoubleOrNull() ?: 0.0, cantidad.toDoubleOrNull() ?: 1.0, selectedTiendaId, barcode, categoria, unidad)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("GUARDAR PRODUCTO", fontWeight = FontWeight.Bold)
            }
        }
    }
}
