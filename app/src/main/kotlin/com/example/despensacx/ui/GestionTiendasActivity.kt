package com.example.despensacx.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.despensacx.data.AppDatabase
import com.example.despensacx.data.TiendaEntity
import com.example.despensacx.ui.components.EmptyState
import com.example.despensacx.ui.theme.DespensaCXTheme
import java.text.SimpleDateFormat
import java.util.*

import androidx.activity.viewModels
import com.example.despensacx.viewmodel.GestionTiendasViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GestionTiendasActivity : ComponentActivity() {

    private val viewModel: GestionTiendasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DespensaCXTheme {
                GestionTiendasScreen(viewModel = viewModel, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionTiendasScreen(viewModel: GestionTiendasViewModel, onBack: () -> Unit) {
    val tiendas by viewModel.tiendas.observeAsState(emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var tiendaParaEditar by remember { mutableStateOf<TiendaEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<TiendaEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestionar Tiendas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { tiendaParaEditar = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Tienda")
            }
        }
    ) { padding ->
        if (tiendas.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Store,
                title = "¿A dónde vamos hoy?",
                description = "Registra tus tiendas favoritas para organizar mejor tus compras por pasillos o locales."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(tiendas, key = { it.id }) { tienda ->
                    TiendaItem(
                        tienda = tienda,
                        onEditar = { tiendaParaEditar = tienda; showDialog = true },
                        onEliminar = { showDeleteConfirm = tienda }
                    )
                }
            }
        }
    }

    if (showDialog) {
        DialogTienda(
            tienda = tiendaParaEditar,
            onDismiss = { showDialog = false },
            onConfirm = { nombre, color ->
                viewModel.guardarTienda(tiendaParaEditar, nombre, color)
                showDialog = false
            }
        )
    }

    if (showDeleteConfirm != null) {
        val tienda = showDeleteConfirm!!
        var hasProducts by remember { mutableStateOf<Boolean?>(null) }

        LaunchedEffect(tienda) {
            val count = viewModel.countProductosByTienda(tienda.id)
            hasProducts = count > 0
        }

        if (hasProducts != null) {
            if (hasProducts == true) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = null },
                    title = { Text("Acción Bloqueada") },
                    text = { Text("No se puede eliminar la tienda porque tiene productos asociados en tus listas.") },
                    confirmButton = {
                        Button(onClick = { showDeleteConfirm = null }) { Text("Aceptar") }
                    }
                )
            } else {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = null },
                    title = { Text("Eliminar Tienda") },
                    text = { Text("¿Deseas eliminar '${tienda.nombre}'?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.eliminarTienda(tienda)
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
        }
    }
}

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
                    modifier = Modifier.fillMaxWidth()
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
