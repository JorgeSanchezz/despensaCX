package com.example.despensacx.ui.components.listas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.despensacx.R
import com.example.despensacx.data.ListaEntity
import java.util.*

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
