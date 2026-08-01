package com.example.despensacx;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "productos",
        foreignKeys = {
                @ForeignKey(
                        entity = ListaEntity.class,
                        parentColumns = "id",
                        childColumns = "listaId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = TiendaEntity.class,
                        parentColumns = "id",
                        childColumns = "tiendaId",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {@Index("listaId"), @Index("tiendaId")}
)
public class ProductoEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long listaId;
    private long tiendaId;
    private String descripcion;
    private double precio;
    private int cantidad;
    private boolean seleccionado;

    public ProductoEntity(long listaId, long tiendaId, String descripcion, double precio, int cantidad, boolean seleccionado) {
        this.listaId = listaId;
        this.tiendaId = tiendaId;
        this.descripcion = descripcion;
        this.precio = precio;
        this.cantidad = cantidad;
        this.seleccionado = seleccionado;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getListaId() { return listaId; }
    public void setListaId(long listaId) { this.listaId = listaId; }

    public long getTiendaId() { return tiendaId; }
    public void setTiendaId(long tiendaId) { this.tiendaId = tiendaId; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public boolean isSeleccionado() { return seleccionado; }
    public void setSeleccionado(boolean seleccionado) { this.seleccionado = seleccionado; }
}