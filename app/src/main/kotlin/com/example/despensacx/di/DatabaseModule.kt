package com.example.despensacx.di

import android.content.Context
import com.example.despensacx.data.AppDatabase
import com.example.despensacx.data.CatalogoDao
import com.example.despensacx.data.ListaDao
import com.example.despensacx.data.ProductoDao
import com.example.despensacx.data.TiendaDao
import com.example.despensacx.data.TicketFotoDao
import com.example.despensacx.data.MembresiaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideListaDao(database: AppDatabase): ListaDao {
        return database.listaDao()
    }

    @Provides
    fun provideProductoDao(database: AppDatabase): ProductoDao {
        return database.productoDao()
    }

    @Provides
    fun provideTiendaDao(database: AppDatabase): TiendaDao {
        return database.tiendaDao()
    }

    @Provides
    fun provideCatalogoDao(database: AppDatabase): CatalogoDao {
        return database.catalogoDao()
    }

    @Provides
    fun provideTicketFotoDao(database: AppDatabase): TicketFotoDao {
        return database.ticketFotoDao()
    }

    @Provides
    fun provideMembresiaDao(database: AppDatabase): MembresiaDao {
        return database.membresiaDao()
    }
}
