package org.example.clases.facturas;

import org.example.clases.personas.Cliente;
import org.example.clases.vehiculo.Automovil;
import org.example.clases.ventas.Venta;

import java.util.Date;
import java.util.UUID;

public class Factura {

    private final String id = UUID.randomUUID().toString();

    private Venta venta;
    private Automovil automovil;
    private Cliente cliente;
    private Date fecha;
    private String estado;

    public Factura(Venta venta, Automovil automovil, Cliente cliente) {
        this.venta = venta;
        this.automovil = automovil;
        this.cliente = cliente;
        this.fecha = new Date();
        this.estado = "Pagada";
    }

    public String getEstado() {
        return estado;
    }

    public String getId() {
        return id;
    }

    public String getClienteId() {
        return cliente.getId();
    }

    public String getClienteNombre() {
        return cliente.getNombre();
    }

    public Date getFecha() {
        return fecha;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public long getMonto() {
        return venta.getMonto();
    }

    public String getPlaca() {
        return automovil.getPlaca();
    }

    public String generarFactura() {
        return "Factura\nPlaca: " + automovil.getPlaca()
                + "\nCliente: " + cliente.getNombre()
                + "\nMonto: " + venta.getMonto()
                + "\nFecha: " + fecha
                + "\nEstado: " + estado;
    }

}
