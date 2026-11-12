package org.example.clases.ventas.medios;

import org.example.clases.ventas.ReglasNegocioVentas;

public class Efectivo extends Medio {

    private long montoRecibido;

    public Efectivo(long montoRecibido) {
        setMontoRecibido(montoRecibido);
    }

    public long getMontoRecibido() {
        return montoRecibido;
    }

    public void setMontoRecibido(long montoRecibido) {
        if (montoRecibido < ReglasNegocioVentas.minMontoRecibir)
            throw new IllegalArgumentException("El monto recibido debe ser mayor a 0");
        this.montoRecibido = montoRecibido;
    }

    @Override
    public void procesarMedio(long monto) {
        long cambio = montoRecibido > monto ? montoRecibido - monto : 0;
    }

    public long calcularCambio(long monto) {
        return montoRecibido - monto;
    }
}
