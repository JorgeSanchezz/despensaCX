package com.example.despensacx.ui.components.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.despensacx.data.PrecioHistorico
import com.example.despensacx.data.ProductoEntity
import com.example.despensacx.data.TiendaEntity
import com.example.despensacx.model.Categoria
import com.example.despensacx.model.UnidadMedida
import com.example.despensacx.ui.components.BarcodeScannerView
import com.example.despensacx.utils.FormatUtils
import com.example.despensacx.viewmodel.DetalleListaViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetProducto(
    producto: ProductoEntity?,
    tiendas: List<TiendaEntity>,
    viewModel: DetalleListaViewModel,
    defaultTiendaId: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        productoExistente: ProductoEntity?,
        descripcion: String,
        precio: Double,
        cantidad: Double,
        tiendaId: Long,
        barcode: String?,
        categoria: String,
        unidad: String,
        onResult: (ProductoEntity) -> Unit
    ) -> Unit
) {
    var desc by remember { mutableStateOf(producto?.descripcion ?: "") }
    var precio by remember { mutableStateOf(if (producto?.precio == 0.0) "" else producto?.precio?.toString() ?: "") }
    var cantidad by remember { 
        val c = producto?.cantidad ?: 1.0
        mutableStateOf(if (c % 1.0 == 0.0) c.toInt().toString() else c.toString())
    }
    val initialTiendaId = remember(producto, tiendas, defaultTiendaId) {
        producto?.tiendaId
            ?: (if (defaultTiendaId != null && tiendas.any { it.id == defaultTiendaId }) defaultTiendaId else tiendas.firstOrNull()?.id ?: -1L)
    }
    var selectedTiendaId by remember(initialTiendaId) { mutableLongStateOf(initialTiendaId) }
    var barcode by remember { mutableStateOf(producto?.barcode) }
    var categoria by remember { mutableStateOf(producto?.categoria ?: "GENERAL") }
    var unidad by remember { mutableStateOf(producto?.unidad ?: "PZA") }
    
    var showScanner by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var preciosHistoricos by remember { mutableStateOf<List<PrecioHistorico>>(emptyList()) }

    var modoRafaga by remember { mutableStateOf(false) }
    val productosAgregadosEnSesion = remember { mutableStateListOf<ProductoEntity>() }
    var productoEnEdicion by remember { mutableStateOf<ProductoEntity?>(null) }
    var productoParaEliminar by remember { mutableStateOf<ProductoEntity?>(null) }
    val descFocusRequester = remember { FocusRequester() }
    
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val cancelarEdicionRafaga = {
        productoEnEdicion = null
        desc = ""
        precio = ""
        cantidad = "1"
        barcode = null
        preciosHistoricos = emptyList()
    }

    val guardarEIncrementar = {
        if (desc.isNotBlank() && selectedTiendaId != -1L) {
            val pDouble = precio.toDoubleOrNull() ?: 0.0
            val cDouble = cantidad.toDoubleOrNull() ?: 1.0
            onConfirm(
                productoEnEdicion,
                desc,
                pDouble,
                cDouble,
                selectedTiendaId,
                barcode,
                categoria,
                unidad
            ) { productoGuardado ->
                if (productoEnEdicion != null) {
                    val index = productosAgregadosEnSesion.indexOfFirst { it.id == productoGuardado.id }
                    if (index != -1) {
                        productosAgregadosEnSesion[index] = productoGuardado
                    } else {
                        productosAgregadosEnSesion.add(productoGuardado)
                    }
                    productoEnEdicion = null
                } else {
                    productosAgregadosEnSesion.add(productoGuardado)
                }

                // Limpiar campos para el siguiente producto pero CONSERVAR la tienda seleccionada
                desc = ""
                precio = ""
                cantidad = "1"
                barcode = null
                preciosHistoricos = emptyList()

                scope.launch {
                    descFocusRequester.requestFocus()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            torchEnabled = false
            showScanner = false
            onDismiss()
        },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (producto != null) {
                        "Editar Producto"
                    } else if (productoEnEdicion != null) {
                        "Editar en Ráfaga"
                    } else {
                        "Nuevo Producto"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (producto == null) {
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = modoRafaga,
                        onClick = {
                            modoRafaga = !modoRafaga
                            if (!modoRafaga) {
                                cancelarEdicionRafaga()
                            }
                        },
                        label = { Text("Modo Ráfaga", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.FlashOn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (modoRafaga) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            if (modoRafaga && productoEnEdicion != null) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Editando: ${productoEnEdicion?.descripcion}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(
                            onClick = { cancelarEdicionRafaga() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
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
                        torchEnabled = false
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
                            onClick = { 
                                torchEnabled = false
                                showScanner = false 
                            },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(descFocusRequester),
                trailingIcon = {
                    IconButton(onClick = { showScanner = !showScanner }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear")
                    }
                },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
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
                    prefix = { Text("$ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) cantidad = it },
                    label = { Text("Cant.") },
                    modifier = Modifier.weight(0.6f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
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

            if (modoRafaga && productosAgregadosEnSesion.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Agregados en esta sesión (${productosAgregadosEnSesion.size}):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    productosAgregadosEnSesion.forEach { item ->
                        val isEditing = productoEnEdicion?.id == item.id
                        InputChip(
                            selected = isEditing,
                            onClick = {
                                if (isEditing) {
                                    cancelarEdicionRafaga()
                                } else {
                                    productoEnEdicion = item
                                    desc = item.descripcion
                                    precio = if (item.precio == 0.0) "" else item.precio.toString()
                                    cantidad = if (item.cantidad % 1.0 == 0.0) item.cantidad.toInt().toString() else item.cantidad.toString()
                                    selectedTiendaId = item.tiendaId
                                    categoria = item.categoria
                                    unidad = item.unidad
                                    barcode = item.barcode
                                }
                            },
                            label = {
                                Text(
                                    text = if (item.precio > 0.0)
                                        "${item.descripcion} (${FormatUtils.formatCurrency(item.precio)})"
                                    else
                                        item.descripcion,
                                    fontWeight = if (isEditing) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { productoParaEliminar = item },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Eliminar",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            if (modoRafaga && producto == null) {
                if (productoEnEdicion != null) {
                    Button(
                        onClick = { guardarEIncrementar() },
                        enabled = desc.isNotBlank() && selectedTiendaId != -1L,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("GUARDAR CAMBIOS DEL PRODUCTO", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (desc.isNotBlank() && selectedTiendaId != -1L) {
                                    val pDouble = precio.toDoubleOrNull() ?: 0.0
                                    val cDouble = cantidad.toDoubleOrNull() ?: 1.0
                                    onConfirm(
                                        null,
                                        desc,
                                        pDouble,
                                        cDouble,
                                        selectedTiendaId,
                                        barcode,
                                        categoria,
                                        unidad
                                    ) { }
                                }
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = if (productosAgregadosEnSesion.isEmpty()) "CANCELAR" else "FINALIZAR (${productosAgregadosEnSesion.size})",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { guardarEIncrementar() },
                            enabled = desc.isNotBlank() && selectedTiendaId != -1L,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("SIGUIENTE ➔", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (desc.isNotBlank() && selectedTiendaId != -1L) {
                            val pDouble = precio.toDoubleOrNull() ?: 0.0
                            val cDouble = cantidad.toDoubleOrNull() ?: 1.0
                            onConfirm(
                                producto,
                                desc,
                                pDouble,
                                cDouble,
                                selectedTiendaId,
                                barcode,
                                categoria,
                                unidad
                            ) { }
                            onDismiss()
                        }
                    },
                    enabled = desc.isNotBlank() && selectedTiendaId != -1L,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (producto == null) "GUARDAR PRODUCTO" else "ACTUALIZAR PRODUCTO",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (productoParaEliminar != null) {
        AlertDialog(
            onDismissRequest = { productoParaEliminar = null },
            title = { Text("Eliminar producto") },
            text = { Text("¿Deseas eliminar '${productoParaEliminar?.descripcion}' de la lista?") },
            confirmButton = {
                Button(
                    onClick = {
                        val p = productoParaEliminar
                        if (p != null) {
                            viewModel.eliminarProducto(p)
                            productosAgregadosEnSesion.removeAll { it.id == p.id }
                            if (productoEnEdicion?.id == p.id) {
                                cancelarEdicionRafaga()
                            }
                        }
                        productoParaEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { productoParaEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
