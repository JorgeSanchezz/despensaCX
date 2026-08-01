package com.example.despensacx;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tiendas")
public class TiendaEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String nombre;
    private String color; // Código de color hexadecimal ej "#FF5722"
    private String fechaRegistro;
    private int orden;

    public TiendaEntity(String nombre, String color, String fechaRegistro, int orden) {
        this.nombre = nombre;
        this.color = color;
        this.fechaRegistro = fechaRegistro;
        this.orden = orden;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}