package com.example.despensacx.ui.components.catalogo

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.despensacx.data.CatalogoProducto
import com.example.despensacx.model.Categoria
import com.example.despensacx.model.UnidadMedida
import com.example.despensacx.ui.components.BarcodeScannerView

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
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
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
