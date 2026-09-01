package com.example.despensacx.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.despensacx.BuildConfig
import java.util.concurrent.Executors

/**
 * AppDatabase con configuración optimizada para producción.
 *
 * Se ha activado 'exportSchema = true' para permitir el seguimiento histórico del esquema
 * y facilitar las migraciones automáticas y manuales.
 */
@Database(
    entities = [
        ListaEntity::class,
        TiendaEntity::class,
        ProductoEntity::class,
        CatalogoProducto::class,
        TicketFotoEntity::class,
        MembresiaEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun listaDao(): ListaDao
    abstract fun tiendaDao(): TiendaDao
    abstract fun productoDao(): ProductoDao
    abstract fun catalogoDao(): CatalogoDao
    abstract fun ticketFotoDao(): TicketFotoDao
    abstract fun membresiaDao(): MembresiaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        val databaseWriteExecutor = Executors.newFixedThreadPool(4)

        /**
         * Lista centralizada de todas las migraciones manuales.
         * Cuando realices un cambio en el esquema (ej. agregar columna), crea un objeto Migration
         * y añádelo a este arreglo.
         */
        private val ALL_MIGRATIONS = arrayOf<Migration>(
            // MIGRATION_6_7 // Ejemplo de migración futura
        )

        @JvmStatic
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "despensa_database"
                )
                    // Registramos todas las migraciones manuales aquí
                    .addMigrations(*ALL_MIGRATIONS)

                /**
                 * PROTECCIÓN DE DATOS:
                 * Solo permitimos la migración destructiva en DEBUG para evitar
                 * borrar la base de datos del usuario en producción por accidente.
                 */
                if (BuildConfig.DEBUG) {
                    builder.fallbackToDestructiveMigration()
                }

                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * EJEMPLO DE MIGRACIÓN MANUAL (v6 -> v7):
         * 1. Sube la 'version' en la anotación @Database a 7.
         * 2. Descomenta este bloque y añade 'MIGRATION_6_7' a 'ALL_MIGRATIONS'.
         */
        /*
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ejemplo: Agregar columna 'lastModified' a la tabla 'listas'
                // db.execSQL("ALTER TABLE listas ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
            }
        }
        */

        /**
         * NOTA SOBRE AUTOMIGRATIONS (Room 2.4+):
         * Si el cambio es simple (añadir tabla o columna), puedes usar:
         * @Database(
         *   ...
         *   autoMigrations = [
         *     AutoMigration (from = 6, to = 7)
         *   ]
         * )
         * Esto requiere que 'exportSchema' sea true y que existan los archivos .json en /schemas.
         */
    }
}
