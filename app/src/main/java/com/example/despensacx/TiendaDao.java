package com.example.despensacx;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TiendaDao {
    @Insert
    long insert(TiendaEntity tienda);

    @Update
    void update(TiendaEntity tienda);

    @Delete
    void delete(TiendaEntity tienda);

    @Query("SELECT * FROM tiendas ORDER BY orden ASC, nombre ASC")
    LiveData<List<TiendaEntity>> getAllTiendas();

    @Query("SELECT * FROM tiendas ORDER BY orden ASC, nombre ASC")
    List<TiendaEntity> getAllTiendasSync();

    @Query("SELECT COUNT(*) FROM tiendas")
    int getCountSync();

    @Query("SELECT COUNT(*) FROM productos WHERE tiendaId = :tiendaId")
    int countProductosByTiendaSync(long tiendaId);
}