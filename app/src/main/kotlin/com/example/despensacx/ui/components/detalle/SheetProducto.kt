package com.example.despensacx.ui.components.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
