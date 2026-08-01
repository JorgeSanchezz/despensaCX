package com.example.despensacx;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ListaDao {
    @Insert
    long insert(ListaEntity lista);

    @Update
    void update(ListaEntity lista);

    @Delete
    void delete(ListaEntity lista);

    @Query("SELECT * FROM listas WHERE id = :id LIMIT 1")
    ListaEntity getByIdSync(long id);

    @Query("SELECT * FROM listas WHERE id = :id LIMIT 1")
    LiveData<ListaEntity> getById(long id);

    @Query("SELECT * FROM listas WHERE archivada = 0 ORDER BY id DESC")
    LiveData<List<ListaEntity>> getListasActivas();

    @Query("SELECT * FROM listas WHERE archivada = 1 ORDER BY id DESC")
    LiveData<List<ListaEntity>> getListasArchivadas();

    @Query("SELECT * FROM listas")
    List<ListaEntity> getAllSync();
}