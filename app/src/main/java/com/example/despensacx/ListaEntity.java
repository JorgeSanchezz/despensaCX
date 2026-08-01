package com.example.despensacx;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "listas")
public class ListaEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String nombre;
    private String fechaCreacion;
    private String fechaModificacion;
    private boolean archivada;
    private double presupuestoMaximo;

    public ListaEntity(String nombre, String fechaCreacion, String fechaModificacion, boolean archivada, double presupuestoMaximo) {
        this.nombre = nombre;
        this.fechaCreacion = fechaCreacion;
        this.fechaModificacion = fechaModificacion;
        this.archivada = archivada;
        this.presupuestoMaximo = presupuestoMaximo;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(String fechaModificacion) { this.fechaModificacion = fechaModificacion; }

    public boolean isArchivada() { return archivada; }
    public void setArchivada(boolean archivada) { this.archivada = archivada; }

    public double getPresupuestoMaximo() { return presupuestoMaximo; }
    public void setPresupuestoMaximo(double presupuestoMaximo) { this.presupuestoMaximo = presupuestoMaximo; }
}