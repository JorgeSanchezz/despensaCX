package com.example.despensacx.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.despensacx.data.MembresiaEntity
import com.example.despensacx.ui.components.EmptyState
import com.example.despensacx.ui.components.membresias.MembresiaItem
import com.example.despensacx.ui.theme.DespensaCXTheme
import com.example.despensacx.viewmodel.GestionMembresiasViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class GestionMembresiasActivity : ComponentActivity() {

    private val viewModel: GestionMembresiasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DespensaCXTheme {
                GestionMembresiasScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionMembresiasScreen(
    viewModel: GestionMembresiasViewModel,
    onBack: () -> Unit
) {
    val membresias by viewModel.membresias.observeAsState(emptyList())
    val tiendas by viewModel.tiendas.observeAsState(emptyList())
    val context = LocalContext.current

    var showStorePicker by remember { mutableStateOf(false) }
    var showSourcePicker by remember { mutableStateOf(false) }
    var selectedTiendaIdForPhoto by remember { mutableLongStateOf(-1L) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMembresiaForView by remember { mutableStateOf<MembresiaEntity?>(null) }
    var membresiaParaEliminar by remember { mutableStateOf<MembresiaEntity?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null && selectedTiendaIdForPhoto != -1L) {
            viewModel.guardarMembresia(selectedTiendaIdForPhoto, photoUri.toString())
            Toast.makeText(context, "Membresía guardada", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && selectedTiendaIdForPhoto != -1L) {
            val persistentUri = copyUriToInternalStorage(context, uri, selectedTiendaIdForPhoto)
            if (persistentUri != null) {
                viewModel.guardarMembresia(selectedTiendaIdForPhoto, persistentUri.toString())
                Toast.makeText(context, "Membresía guardada desde galería", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Membresías", fontWeight = FontWeight.ExtraBold) },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    if (tiendas.isEmpty()) {
                        Toast.makeText(context, "Primero debes registrar una tienda", Toast.LENGTH_LONG).show()
                    } else {
                        showStorePicker = true 
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Membresía")
            }
        }
    ) { padding ->
        if (membresias.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Badge,
                title = "Tarjetas de Membresía",
                description = "Guarda fotos de tus tarjetas de puntos o membresías para tenerlas siempre a la mano al pagar."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(membresias, key = { it.id }) { membresia ->
                    val tienda = tiendas.find { it.id == membresia.tiendaId }
                    MembresiaItem(
                        membresia = membresia,
                        tienda = tienda,
                        onClick = { selectedMembresiaForView = membresia },
                        onEliminar = { membresiaParaEliminar = membresia }
                    )
                }
            }
        }
    }

    if (showStorePicker) {
        AlertDialog(
            onDismissRequest = { showStorePicker = false },
            title = { Text("Seleccionar Tienda") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(tiendas) { tienda ->
                        TextButton(
                            onClick = {
                                showStorePicker = false
                                selectedTiendaIdForPhoto = tienda.id
                                showSourcePicker = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tienda.nombre)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStorePicker = false }) { Text("Cancelar") }
            }
        )
    }

    if (showSourcePicker) {
        AlertDialog(
            onDismissRequest = { showSourcePicker = false },
            title = { Text("Origen de la foto") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showSourcePicker = false
                            val uri = generarUriMembresia(context, selectedTiendaIdForPhoto)
                            photoUri = uri
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoCamera, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Tomar Foto con Cámara")
                    }
                    TextButton(
                        onClick = {
                            showSourcePicker = false
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Collections, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Seleccionar de Galería")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSourcePicker = false }) { Text("Cancelar") }
            }
        )
    }

    if (selectedMembresiaForView != null) {
        val t = tiendas.find { it.id == selectedMembresiaForView!!.tiendaId }
        MembresiaViewerDialog(
            fotoPath = selectedMembresiaForView!!.fotoPath,
            tiendaNombre = t?.nombre ?: "Membresía",
            onDismiss = { selectedMembresiaForView = null }
        )
    }

    if (membresiaParaEliminar != null) {
        AlertDialog(
            onDismissRequest = { membresiaParaEliminar = null },
            title = { Text("Eliminar Membresía") },
            text = { Text("¿Deseas eliminar la foto de esta membresía?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarMembresia(membresiaParaEliminar!!)
                        membresiaParaEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { membresiaParaEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

private fun generarUriMembresia(context: android.content.Context, tiendaId: Long): Uri {
    val dir = File(context.filesDir, "membresias")
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, "membresia_${tiendaId}_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun copyUriToInternalStorage(context: android.content.Context, uri: Uri, tiendaId: Long): Uri? {
    return try {
        val dir = File(context.filesDir, "membresias")
        if (!dir.exists()) dir.mkdirs()
        val destFile = File(dir, "membresia_${tiendaId}_${System.currentTimeMillis()}.jpg")
        
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(destFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        Uri.fromFile(destFile)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun MembresiaViewerDialog(
    fotoPath: String,
    tiendaNombre: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // Maximizar el brillo de la pantalla mientras la ventana esté abierta
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        val layoutParams = window?.attributes
        val originalBrightness = layoutParams?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        
        layoutParams?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        window?.attributes = layoutParams
        
        onDispose {
            layoutParams?.screenBrightness = originalBrightness
            window?.attributes = layoutParams
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.8f, 5f)
                            offset += pan
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = fotoPath,
                    contentDescription = "Membresía $tiendaNombre",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            rotationZ = rotationAngle,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tiendaNombre,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { rotationAngle = (rotationAngle + 90f) % 360f }) {
                    Icon(
                        imageVector = Icons.Default.RotateRight,
                        contentDescription = "Rotar 90°",
                        tint = Color.White
                    )
                }

                IconButton(onClick = {
                    scale = 1f
                    offset = Offset.Zero
                    rotationAngle = 0f
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restablecer",
                        tint = Color.White
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
