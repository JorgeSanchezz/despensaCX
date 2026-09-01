package com.example.despensacx.ui.components.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.despensacx.data.TicketFotoEntity
import com.example.despensacx.utils.FormatUtils

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
                    .clip(CircleShape)
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
