package com.example.despensacx.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import java.io.*
import java.util.*

import kotlinx.coroutines.runBlocking

object BackupHelper {

    class BackupContainer {
        var listas: List<ListaEntity>? = null
        var tiendas: List<TiendaEntity>? = null
        var productos: List<ProductoEntity>? = null
        var catalogo: List<CatalogoProducto>? = null
    }

    @JvmStatic
    fun generarNombreDefectoRespaldo(): String {
        return "DespensaCX_Backup.json"
    }

    @JvmStatic
    fun exportarJSONToUri(context: Context, targetUri: Uri): Boolean {
        return try {
            context.getContentResolver().openOutputStream(targetUri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    val db = AppDatabase.getInstance(context)
                    val container = BackupContainer()
                    container.listas = db.listaDao().getAllSync()
                    container.tiendas = db.tiendaDao().getAllTiendasSync()
                    container.productos = db.productoDao().getAllSync()
                    container.catalogo = db.catalogoDao().getAllSync()

                    val json = Gson().toJson(container)
                    writer.write(json)
                    true
                }
            } ?: false
        } catch (e: Exception) {
            Log.e("BackupHelper", "Error al exportar JSON vía SAF", e)
            false
        }
    }

    @JvmStatic
    fun importarJSONFromUri(context: Context, sourceUri: Uri): Boolean {
        return try {
            context.getContentResolver().openInputStream(sourceUri)?.use { `is` ->
                BufferedReader(InputStreamReader(`is`)).use { reader ->
                    val sb = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line)
                    }

                    val container = Gson().fromJson(sb.toString(), BackupContainer::class.java) ?: return false

                    val db = AppDatabase.getInstance(context)
                    val mapaTiendasOldToNew = mutableMapOf<Long, Long>()

                    runBlocking {
                        // Limpiar datos actuales para sobrescribir
                        db.productoDao().deleteAll()
                        db.listaDao().deleteAll()
                        db.tiendaDao().deleteAll()
                        db.catalogoDao().deleteAll()

                        container.catalogo?.forEach { cp ->
                            db.catalogoDao().insert(cp)
                        }

                        container.tiendas?.forEach { t ->
                            val oldId = t.id
                            t.id = 0
                            val newId = db.tiendaDao().insert(t)
                            mapaTiendasOldToNew[oldId] = newId
                        }

                        container.listas?.forEach { l ->
                            val oldListaId = l.id
                            l.id = 0
                            val newListaId = db.listaDao().insert(l)

                            container.productos?.forEach { p ->
                                if (p.listaId == oldListaId) {
                                    p.id = 0
                                    p.listaId = newListaId
                                    val newTiendaId = mapaTiendasOldToNew[p.tiendaId]
                                    if (newTiendaId != null) {
                                        p.tiendaId = newTiendaId
                                        db.productoDao().insert(p)
                                    }
                                }
                            }
                        }
                    }
                    true
                }
            } ?: false
        } catch (e: Exception) {
            Log.e("BackupHelper", "Error al importar JSON vía SAF", e)
            false
        }
    }
}
