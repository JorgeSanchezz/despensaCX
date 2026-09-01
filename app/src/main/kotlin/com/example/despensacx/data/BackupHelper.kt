package com.example.despensacx.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import java.io.*
import java.util.*

object BackupHelper {

    class BackupContainer {
        var listas: List<ListaEntity>? = null
        var tiendas: List<TiendaEntity>? = null
        var productos: List<ProductoEntity>? = null
        var catalogo: List<CatalogoProducto>? = null
        var membresias: List<MembresiaEntity>? = null
    }

    fun generarNombreDefectoRespaldo(): String {
        return "DespensaCX_Backup.json"
    }

    fun exportarJSONToUri(context: Context, targetUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    val db = AppDatabase.getInstance(context)
                    val container = BackupContainer().apply {
                        listas = db.listaDao().getAllSync()
                        tiendas = db.tiendaDao().getAllTiendasSync()
                        productos = db.productoDao().getAllSync()
                        catalogo = db.catalogoDao().getAllSync()
                    }
                    val json = Gson().toJson(container)
                    writer.write(json)
                    true
                }
            } ?: false
        } catch (e: Exception) {
            Log.e("BackupHelper", "Error al exportar JSON", e)
            false
        }
    }

    fun importarJSONFromUri(context: Context, sourceUri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { `is` ->
                importarJSONFromInputStream(context, `is`)
            } ?: false
        } catch (e: Exception) {
            Log.e("BackupHelper", "Error al importar JSON", e)
            false
        }
    }

    fun importarJSONFromInputStream(context: Context, inputStream: InputStream): Boolean {
        return try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }

            val container = Gson().fromJson(sb.toString(), BackupContainer::class.java) ?: return false
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

                container.membresias?.forEach { m ->
                    val newTiendaId = mapaTiendasOldToNew[m.tiendaId]
                    if (newTiendaId != null) {
                        db.membresiaDao().insert(m.copy(id = 0, tiendaId = newTiendaId))
                    }
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
                }
            }
            true
        } catch (e: Exception) {
            Log.e("BackupHelper", "Error procesando JSON", e)
            false
        }
    }
}
