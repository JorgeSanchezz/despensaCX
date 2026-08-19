package com.example.despensacx.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.despensacx.model.EstadisticaModel
import com.example.despensacx.ui.components.EmptyState
import com.example.despensacx.ui.theme.DespensaCXTheme
import com.example.despensacx.utils.FormatUtils
import com.example.despensacx.viewmodel.EstadisticasViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class EstadisticasActivity : ComponentActivity() {

    private val viewModel: EstadisticasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DespensaCXTheme {
                EstadisticasScreen(viewModel = viewModel, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasScreen(viewModel: EstadisticasViewModel, onBack: () -> Unit) {
    val anios by viewModel.estadisticas.observeAsState(emptyList<EstadisticaModel.AnnoModel>())
    val loading by viewModel.loading.observeAsState(true)
    var selectedAnio by remember { mutableStateOf<EstadisticaModel.AnnoModel?>(null) }

    LaunchedEffect(Unit) {
        viewModel.cargarEstadisticas()
    }

    BackHandler(enabled = selectedAnio != null) {
        selectedAnio = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedAnio == null) "Estadísticas Anuales" else "Meses de ${selectedAnio!!.anio}", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { if (selectedAnio != null) selectedAnio = null else onBack() }) {
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
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (anios.isEmpty()) {
            EmptyState(
                icon = Icons.Default.BarChart,
                title = "Sin datos suficientes",
                description = "Completa algunas listas de compras para ver tus estadísticas y ahorros por mes."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                if (selectedAnio == null) {
                    item {
                        Text(
                            "Resumen de Gastos", 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(anios) { anio ->
                        AnioItem(anio = anio, onClick = { selectedAnio = anio })
                    }
                } else {
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Tendencia Mensual", 
                                style = MaterialTheme.typography.titleLarge, 
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(16.dp))
                            MonthlyBarChart(meses = selectedAnio!!.meses)
                        }
                    }
                    items(selectedAnio!!.meses) { mes ->
                        MesItem(mes = mes)
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyBarChart(meses: List<EstadisticaModel.MesModel>) {
    val sortedMeses = meses.sortedBy { it.mesAnioClave }
    val maxGasto = sortedMeses.maxOfOrNull { it.totalMes } ?: 1.0
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        sortedMeses.forEach { mes ->
            val heightFactor = (mes.totalMes / maxGasto).toFloat()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .fillMaxHeight(heightFactor.coerceAtLeast(0.05f))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = mes.mesNombre.take(3), 
                    fontSize = 10.sp, 
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AnioItem(anio: EstadisticaModel.AnnoModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = anio.anio, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "${anio.meses.size} meses registrados", fontSize = 12.sp, color = Color.Gray)
            }
            Text(
                text = FormatUtils.formatCurrency(anio.totalAnio), 
                fontSize = 20.sp, 
                color = MaterialTheme.colorScheme.primary, 
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun MesItem(mes: EstadisticaModel.MesModel) {
    var expanded by remember { mutableStateOf(false) }
    val topTienda = mes.gastosPorTienda.maxByOrNull { it.value }
    val topCat = mes.gastosPorCategoria.maxByOrNull { it.value }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = mes.mesNombre, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    mes.variacionMesAnterior?.let { variacion ->
                        val color = if (variacion > 0) Color.Red else Color(0xFF2E7D32)
                        val sign = if (variacion > 0) "+" else ""
                        Text(
                            text = "$sign${String.format(Locale.getDefault(), "%.1f", variacion)}% vs mes anterior", 
                            fontSize = 11.sp, 
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(text = FormatUtils.formatCurrency(mes.totalMes), fontSize = 18.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Insights rápidos
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (topTienda != null) {
                    InsightChip(icon = Icons.Default.Store, label = topTienda.key)
                }
                if (topCat != null) {
                    InsightChip(icon = Icons.Default.Category, label = topCat.key)
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text("Gastos por Tienda", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    mes.gastosPorTienda.entries.sortedByDescending { it.value }.forEach { (tienda, monto) ->
                        StatRow(name = tienda, amount = monto)
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Gastos por Categoría", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    mes.gastosPorCategoria.entries.sortedByDescending { it.value }.forEach { (cat, monto) ->
                        StatRow(name = cat, amount = monto)
                    }
                }
            }
            
            if (!expanded) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ExpandMore, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun InsightChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
fun StatRow(name: String, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(text = FormatUtils.formatCurrency(amount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
