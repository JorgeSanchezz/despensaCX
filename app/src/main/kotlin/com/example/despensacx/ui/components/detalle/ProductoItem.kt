package com.example.despensacx.ui.components.detalle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.despensacx.data.ProductoEntity
import com.example.despensacx.model.Categoria
import com.example.despensacx.model.ProductoConTienda
import com.example.despensacx.model.UnidadMedida
import com.example.despensacx.utils.FormatUtils

@Composable
fun ProductoItem(
    pct: ProductoConTienda,
    compactMode: Boolean = false,
    onToggle: (Boolean) -> Unit,
    onEditar: (ProductoEntity) -> Unit,
    onEliminar: (ProductoEntity) -> Unit
) {
    val p = pct.producto
    val sinPrecio = p.precio == 0.0
    
    // Jerarquía de opacidad
    val itemAlpha = when {
        !p.seleccionado -> 0.5f
        sinPrecio -> 0.75f
        else -> 1.0f
    }
    
    val textDecoration = TextDecoration.None
    val categoria = try { Categoria.valueOf(p.categoria) } catch (e: Exception) { Categoria.GENERAL }
    val unidad = try { UnidadMedida.valueOf(p.unidad).label } catch(e: Exception) { "" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !p.seleccionado -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                sinPrecio -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (p.seleccionado || sinPrecio) 0.dp else 3.dp
        ),
        border = if (sinPrecio && p.seleccionado) BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)) else null
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
                    color = if (sinPrecio && p.seleccionado) Color.Red else MaterialTheme.colorScheme.onSurface
                )
                if (!compactMode) {
                    Text(
                        text = if (sinPrecio) "¡Poner Precio!" else "$cantStr $unidad x ${FormatUtils.formatCurrency(p.precio)}",
                        fontSize = 12.sp,
                        color = if (sinPrecio) Color(0xFFE65100) else Color.Gray,
                        fontWeight = if (sinPrecio) FontWeight.ExtraBold else FontWeight.Normal,
                        modifier = Modifier.alpha(if (sinPrecio) 1f else itemAlpha)
                    )
                }
            }
            
            if (!compactMode) {
                Column(horizontalAlignment = Alignment.End) {
                    if (sinPrecio) {
                        Text(
                            text = "$ ???",
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE65100),
                            fontSize = 18.sp
                        )
                    } else {
                        Text(
                            text = FormatUtils.formatCurrency(p.precio * p.cantidad),
                            fontWeight = FontWeight.ExtraBold,
                            textDecoration = textDecoration,
                            modifier = Modifier.alpha(itemAlpha),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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
