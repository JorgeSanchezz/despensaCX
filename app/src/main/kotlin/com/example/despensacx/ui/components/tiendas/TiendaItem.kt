package com.example.despensacx.ui.components.tiendas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.despensacx.data.TiendaEntity

@Composable
fun TiendaItem(tienda: TiendaEntity, onEditar: () -> Unit, onEliminar: () -> Unit) {
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
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor(tienda.color))
                        } catch (e: Exception) {
                            Color.Gray
                        }
                    )
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = tienda.nombre,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onEditar) {
                Icon(Icons.Default.Edit, contentDescription = "Editar")
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogTienda(
    tienda: TiendaEntity?,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var nombre by remember { mutableStateOf(tienda?.nombre ?: "") }
    var colorHex by remember { mutableStateOf(tienda?.color ?: "#F44336") }

    val paletaColores = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#00BCD4", "#009688",
        "#4CAF50", "#8BC34A", "#FFEB3B", "#FF9800",
        "#FF5722", "#795548", "#9E9E9E", "#607D8B"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tienda == null) "Agregar Nueva Tienda" else "Editar Tienda") },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la tienda") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                Spacer(Modifier.height(16.dp))
                Text("Seleccionar Color:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    paletaColores.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { colorHex = hex }
                                .then(
                                    if (colorHex == hex) Modifier.background(Color.Black.copy(alpha = 0.3f)) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (colorHex == hex) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (nombre.isNotBlank()) {
                    onConfirm(nombre, colorHex)
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
