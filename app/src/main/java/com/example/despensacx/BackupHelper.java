package com.example.despensacx;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackupHelper {

    public static class BackupContainer {
        public List<ListaEntity> listas;
        public List<TiendaEntity> tiendas;
        public List<ProductoEntity> productos;
    }

    public static boolean exportarJSON(Context context, File targetFile) {
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            BackupContainer container = new BackupContainer();
            container.listas = db.listaDao().getAllSync();
            container.tiendas = db.tiendaDao().getAllTiendasSync();
            container.productos = db.productoDao().getAllSync();

            String json = new Gson().toJson(container);

            FileOutputStream fos = new FileOutputStream(targetFile);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write(json);
            writer.close();
            fos.close();
            return true;
        } catch (Exception e) {
            Log.e("BackupHelper", "Error al exportar JSON", e);
            return false;
        }
    }

    public static boolean importarJSON(Context context, File sourceFile) {
        try {
            FileInputStream fis = new FileInputStream(sourceFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            fis.close();

            BackupContainer container = new Gson().fromJson(sb.toString(), BackupContainer.class);
            if (container == null) return false;

            AppDatabase db = AppDatabase.getInstance(context);

            // Reconstruir datos e IDs
            Map<Long, Long> mapaTiendasOldToNew = new HashMap<>();

            for (TiendaEntity t : container.tiendas) {
                long oldId = t.getId();
                t.setId(0); // Forzar auto-generate
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
            Log.e("BackupHelper", "Error al importar JSON", e);
            return false;
        }
    }
}