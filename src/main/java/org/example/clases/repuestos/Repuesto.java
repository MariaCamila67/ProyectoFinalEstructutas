package org.example.clases.repuestos;

import java.time.LocalDateTime;

public class Repuesto {

    private String nombre;
    private String proveedor;
    private LocalDateTime fechaCompra;

    public Repuesto(String nombre, String proveedor, LocalDateTime fechaCompra) {
        setNombre(nombre);
        setProveedor(proveedor);
        this.fechaCompra = fechaCompra;
    }

    public LocalDateTime getFechaCompra() {
        return fechaCompra;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        if (proveedor == null || proveedor.length() < ReglasNegocioRepuesto.longMinProveedor)
            throw new IllegalArgumentException("El nombre del proveedor no es valido");
        this.proveedor = proveedor;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.length() < ReglasNegocioRepuesto.longMinNombre)
            throw new IllegalArgumentException("El nombre del repuesto no es valido");
        this.nombre = nombre;
    }
}
