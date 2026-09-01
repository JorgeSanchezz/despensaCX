package com.example.despensacx.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.despensacx.data.CatalogoProducto
import com.example.despensacx.data.PrecioHistorico
import com.example.despensacx.model.Categoria
import com.example.despensacx.ui.components.EmptyState
import com.example.despensacx.ui.theme.DespensaCXTheme
import com.example.despensacx.utils.FormatUtils
import com.example.despensacx.viewmodel.ComparadorPreciosViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ComparadorPreciosActivity : ComponentActivity() {

    private val viewModel: ComparadorPreciosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DespensaCXTheme {
                ComparadorPreciosScreen(viewModel = viewModel, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparadorPreciosScreen(viewModel: ComparadorPreciosViewModel, onBack: () -> Unit) {
    val query by viewModel.searchQuery.collectAsState()
    val resultados by viewModel.resultados.observeAsState(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comparador de Precios", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.updateQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Busca un producto (ej. Leche, Arroz)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            if (resultados.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = "¿Buscas el mejor precio?",
                    description = "Escribe el nombre de un producto para ver dónde lo compraste más barato anteriormente."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(resultados, key = { it.barcode }) { producto ->
                        ComparadorItem(producto = producto, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun ComparadorItem(producto: CatalogoProducto, viewModel: ComparadorPreciosViewModel) {
    var precios by remember { mutableStateOf<List<PrecioHistorico>>(emptyList()) }
    val categoria = try { Categoria.valueOf(producto.categoria) } catch (e: Exception) { Categoria.GENERAL }

    LaunchedEffect(producto.barcode) {
        precios = viewModel.getPreciosHistoricos(producto.barcode)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(categoria.icono, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Text(producto.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            }
            
            Spacer(Modifier.height(12.dp))
            
            if (precios.isEmpty()) {
                Text("Sin historial de precios todavía.", fontSize = 12.sp, color = Color.Gray)
            } else {
                val mejorPrecio = precios.minByOrNull { it.precio }
                
                precios.sortedBy { it.precio }.forEach { hist ->
                    val esElMejor = hist == mejorPrecio
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                if (esElMejor) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(hist.tiendaNombre, fontWeight = if (esElMejor) FontWeight.Bold else FontWeight.Normal)
                            if (esElMejor) {
                                Text("¡MEJOR PRECIO!", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Text(FormatUtils.formatCurrency(hist.precio), fontWeight = FontWeight.ExtraBold, color = if (esElMejor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
