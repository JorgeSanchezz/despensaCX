package com.example.despensacx;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {ListaEntity.class, TiendaEntity.class, ProductoEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ListaDao listaDao();
    public abstract TiendaDao tiendaDao();
    public abstract ProductoDao productoDao();

    private static volatile AppDatabase INSTANCE;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(4);

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "despensa_database")
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                TiendaDao dao = INSTANCE.tiendaDao();
                if (dao.getCountSync() == 0) {
                    String fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
                    dao.insert(new TiendaEntity("Bodega Aurrerá", "#4CAF50", fecha, 1));
                    dao.insert(new TiendaEntity("Walmart", "#0071CE", fecha, 2));
                    dao.insert(new TiendaEntity("Sam's Club", "#003087", fecha, 3));
                    dao.insert(new TiendaEntity("Otra", "#9E9E9E", fecha, 4));
                }
            });
        }
    };
}