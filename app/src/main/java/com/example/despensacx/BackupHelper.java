package com.example.despensacx;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class BackupHelper {

    public static class BackupContainer {
        public List<ListaEntity> listas;
        public List<TiendaEntity> tiendas;
        public List<ProductoEntity> productos;
    }

    public static String generarNombreDefectoRespaldo() {
        String fecha = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        return "respaldo_" + fecha + ".json";
    }

    public static boolean exportarJSONToUri(Context context, Uri targetUri) {
        try (OutputStream os = context.getContentResolver().openOutputStream(targetUri);
             OutputStreamWriter writer = new OutputStreamWriter(os)) {

            AppDatabase db = AppDatabase.getInstance(context);
            BackupContainer container = new BackupContainer();
            container.listas = db.listaDao().getAllSync();
            container.tiendas = db.tiendaDao().getAllTiendasSync();
            container.productos = db.productoDao().getAllSync();

            String json = new Gson().toJson(container);
            writer.write(json);
            return true;
        } catch (Exception e) {
            Log.e("BackupHelper", "Error al exportar JSON vía SAF", e);
            return false;
        }
    }

    public static boolean importarJSONFromUri(Context context, Uri sourceUri) {
        try (InputStream is = context.getContentResolver().openInputStream(sourceUri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            BackupContainer container = new Gson().fromJson(sb.toString(), BackupContainer.class);
            if (container == null) return false;

            AppDatabase db = AppDatabase.getInstance(context);
            Map<Long, Long> mapaTiendasOldToNew = new HashMap<>();

            for (TiendaEntity t : container.tiendas) {
                long oldId = t.getId();
                t.setId(0);
                long newId = db.tiendaDao().insert(t);
                mapaTiendasOldToNew.put(oldId, newId);
            }

            for (ListaEntity l : container.listas) {
                long oldListaId = l.getId();
                l.setId(0);
                long newListaId = db.listaDao().insert(l);

                if (container.productos != null) {
                    for (ProductoEntity p : container.productos) {
                        if (p.getListaId() == oldListaId) {
                            p.setId(0);
                            p.setListaId(newListaId);
                            Long newTiendaId = mapaTiendasOldToNew.get(p.getTiendaId());
                            if (newTiendaId != null) {
                                p.setTiendaId(newTiendaId);
                                db.productoDao().insert(p);
                            }
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            Log.e("BackupHelper", "Error al importar JSON vía SAF", e);
            return false;
        }
    }
}