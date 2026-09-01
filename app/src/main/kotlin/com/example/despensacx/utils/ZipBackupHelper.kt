package com.example.despensacx.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.despensacx.data.*
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipBackupHelper {

    private const val BACKUP_JSON_NAME = "backup_data.json"
    private const val TICKETS_DIR = "tickets"
    private const val MEMBRESIAS_DIR = "membresias"

    class BackupContainer {
        var listas: List<ListaEntity>? = null
        var tiendas: List<TiendaEntity>? = null
        var productos: List<ProductoEntity>? = null
        var catalogo: List<CatalogoProducto>? = null
        var ticketFotos: List<TicketFotoEntity>? = null
        var membresias: List<MembresiaEntity>? = null
    }

    fun generarNombreDefectoRespaldo(): String {
        val fecha = SimpleDateFormat("dd-MM-yyyy_HHmm", Locale.getDefault()).format(Date())
        return "DespensaCX_FullBackup_$fecha.zip"
    }

    fun exportarRespaldoCompleto(context: Context, targetUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { os ->
                ZipOutputStream(BufferedOutputStream(os)).use { zos ->
                    val db = AppDatabase.getInstance(context)
                    
                    // 1. Preparar el JSON de datos
                    val container = BackupContainer().apply {
                        listas = db.listaDao().getAllSync()
                        tiendas = db.tiendaDao().getAllTiendasSync()
                        productos = db.productoDao().getAllSync()
                        catalogo = db.catalogoDao().getAllSync()
                        
                        ticketFotos = db.ticketFotoDao().getFotosByListaSync().map { 
                            it.copy(fotoPath = File(it.fotoPath).name)
                        }
                        
                        membresias = db.membresiaDao().getAllSync().map {
                            it.copy(fotoPath = File(it.fotoPath).name)
                        }
                    }
                    val jsonData = Gson().toJson(container)

                    // 2. Escribir JSON al ZIP
                    zos.putNextEntry(ZipEntry(BACKUP_JSON_NAME))
                    zos.write(jsonData.toByteArray())
                    zos.closeEntry()

                    // 3. Empaquetar carpeta de tickets
                    val ticketsDir = File(context.filesDir, TICKETS_DIR)
                    if (ticketsDir.exists() && ticketsDir.isDirectory) {
                        ticketsDir.listFiles()?.filter { it.extension == "jpg" }?.forEach { file ->
                            zos.putNextEntry(ZipEntry("$TICKETS_DIR/${file.name}"))
                            file.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                    
                    // 4. Empaquetar membresías
                    val membresiasDir = File(context.filesDir, MEMBRESIAS_DIR)
                    if (membresiasDir.exists() && membresiasDir.isDirectory) {
                        membresiasDir.listFiles()?.filter { it.extension == "jpg" }?.forEach { file ->
                            zos.putNextEntry(ZipEntry("$MEMBRESIAS_DIR/${file.name}"))
                            file.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                    true
                }
            } ?: false
        } catch (e: Exception) {
            Log.e("ZipBackupHelper", "Error al exportar ZIP", e)
            false
        }
    }

    fun importarRespaldo(context: Context, sourceUri: Uri): Boolean {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(sourceUri) ?: return false
            
            val bis = BufferedInputStream(inputStream)
            bis.mark(1024)
            val header = ByteArray(4)
            bis.read(header)
            bis.reset()

            val isZip = header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()

            if (isZip) {
                importarDesdeZip(context, bis)
            } else {
                BackupHelper.importarJSONFromInputStream(context, bis)
            }
        } catch (e: Exception) {
            Log.e("ZipBackupHelper", "Error al importar", e)
            false
        }
    }

    private fun importarDesdeZip(context: Context, inputStream: InputStream): Boolean {
        return try {
            val ticketsDir = File(context.filesDir, TICKETS_DIR).apply { if (!exists()) mkdirs() }
            val membresiasDir = File(context.filesDir, MEMBRESIAS_DIR).apply { if (!exists()) mkdirs() }
            var container: BackupContainer? = null

            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    when {
                        entry.name == BACKUP_JSON_NAME -> {
                            val reader = BufferedReader(InputStreamReader(zis))
                            container = Gson().fromJson(reader, BackupContainer::class.java)
                        }
                        entry.name.startsWith("$TICKETS_DIR/") -> {
                            val fileName = entry.name.substringAfter("/")
                            if (fileName.isNotEmpty()) {
                                val destFile = File(ticketsDir, fileName)
                                destFile.outputStream().use { zis.copyTo(it) }
                            }
                        }
                        entry.name.startsWith("$MEMBRESIAS_DIR/") -> {
                            val fileName = entry.name.substringAfter("/")
                            if (fileName.isNotEmpty()) {
                                val destFile = File(membresiasDir, fileName)
                                destFile.outputStream().use { zis.copyTo(it) }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            container?.let { restoreDatabase(context, it) } ?: false
        } catch (e: Exception) {
            Log.e("ZipBackupHelper", "Error procesando ZIP", e)
            false
        }
    }

    private fun restoreDatabase(context: Context, container: BackupContainer): Boolean {
        return try {
            val db = AppDatabase.getInstance(context)
            val mapaTiendasOldToNew = mutableMapOf<Long, Long>()

            runBlocking {
                db.productoDao().deleteAll()
                db.listaDao().deleteAll()
                db.tiendaDao().deleteAll()
                db.catalogoDao().deleteAll()
                db.ticketFotoDao().deleteAll()
                db.membresiaDao().deleteAll()

                container.catalogo?.forEach { db.catalogoDao().insert(it) }

                container.tiendas?.forEach { t ->
                    val oldId = t.id
                    t.id = 0
                    mapaTiendasOldToNew[oldId] = db.tiendaDao().insert(t)
                }

                container.listas?.forEach { l ->
                    val oldListaId = l.id
                    l.id = 0
                    val newListaId = db.listaDao().insert(l)

                    container.productos?.filter { it.listaId == oldListaId }?.forEach { p ->
                        p.id = 0
                        p.listaId = newListaId
                        p.tiendaId = mapaTiendasOldToNew[p.tiendaId] ?: p.tiendaId
                        db.productoDao().insert(p)
                    }

                    container.ticketFotos?.filter { it.listaId == oldListaId }?.forEach { f ->
                        val newTiendaId = mapaTiendasOldToNew[f.tiendaId]
                        if (newTiendaId != null) {
                            val fileName = File(f.fotoPath).name
                            val persistentPath = File(File(context.filesDir, TICKETS_DIR), fileName).absolutePath
                            db.ticketFotoDao().insert(f.copy(id = 0, listaId = newListaId, tiendaId = newTiendaId, fotoPath = persistentPath))
                        }
                    }
                }
                
                // Restaurar membresías (fuera del bucle de listas ya que dependen de tiendas)
                container.membresias?.forEach { m ->
                    val newTiendaId = mapaTiendasOldToNew[m.tiendaId]
                    if (newTiendaId != null) {
                        val fileName = File(m.fotoPath).name
                        val persistentPath = File(File(context.filesDir, MEMBRESIAS_DIR), fileName).absolutePath
                        db.membresiaDao().insert(m.copy(id = 0, tiendaId = newTiendaId, fotoPath = persistentPath))
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e("ZipBackupHelper", "Error restaurando DB", e)
            false
        }
    }
}
