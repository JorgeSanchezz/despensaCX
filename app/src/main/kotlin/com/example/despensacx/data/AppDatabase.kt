package com.example.despensacx.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@Database(entities = [ListaEntity::class, TiendaEntity::class, ProductoEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun listaDao(): ListaDao
    abstract fun tiendaDao(): TiendaDao
    abstract fun productoDao(): ProductoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        val databaseWriteExecutor = Executors.newFixedThreadPool(4)

        @JvmStatic
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "despensa_database"
                )
                    .addCallback(sRoomDatabaseCallback)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val sRoomDatabaseCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                databaseWriteExecutor.execute {
                    val dao = INSTANCE?.tiendaDao()
                    if (dao?.getCountSync() == 0) {
                        val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                        dao.insert(TiendaEntity(nombre = "Bodega Aurrerá", color = "#4CAF50", fechaRegistro = fecha, orden = 1))
                        dao.insert(TiendaEntity(nombre = "Walmart", color = "#0071CE", fechaRegistro = fecha, orden = 2))
                        dao.insert(TiendaEntity(nombre = "Sam's Club", color = "#003087", fechaRegistro = fecha, orden = 3))
                        dao.insert(TiendaEntity(nombre = "Otra", color = "#9E9E9E", fechaRegistro = fecha, orden = 4))
                    }
                }
            }
        }
    }
}
