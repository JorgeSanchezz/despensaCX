package com.example.despensacx;

public class ProductoConTienda {
    private ProductoEntity producto;
    private TiendaEntity tienda;

    public ProductoConTienda(ProductoEntity producto, TiendaEntity tienda) {
        this.producto = producto;
        this.tienda = tienda;
    }

    public ProductoEntity getProducto() { return producto; }
    public TiendaEntity getTienda() { return tienda; }
}