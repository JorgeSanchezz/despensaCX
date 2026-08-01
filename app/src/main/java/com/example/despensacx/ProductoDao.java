package com.example.despensacx;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ProductoDao {

    @Insert
    long insert(ProductoEntity producto);

    @Update
    void update(ProductoEntity producto);

    @Delete
    void delete(ProductoEntity producto);

    @Query("SELECT * FROM productos WHERE listaId = :listaId")
    LiveData<List<ProductoEntity>> getProductosByLista(long listaId);

    @Query("SELECT * FROM productos WHERE listaId = :listaId")
    List<ProductoEntity> getProductosByListaSync(long listaId);

    @Query("UPDATE productos SET seleccionado = :seleccionado WHERE listaId = :listaId")
    void setAllSeleccionadoSync(long listaId, boolean seleccionado);

    @Query("SELECT * FROM productos")
    List<ProductoEntity> getAllSync();
}