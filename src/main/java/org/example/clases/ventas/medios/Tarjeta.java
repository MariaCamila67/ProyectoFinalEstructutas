package org.example.clases.ventas.medios;

import org.example.clases.ventas.ReglasNegocioVentas;

public class Tarjeta extends Medio {

    private int numeroCuotas;

    public Tarjeta(int numeroCuotas) {
        setNumeroCuotas(numeroCuotas);
    }

    public int getNumeroCuotas() {
        return numeroCuotas;
    }

    public void setNumeroCuotas(int numeroCuotas) {
        if (numeroCuotas <= ReglasNegocioVentas.minCuotas)
            throw new IllegalArgumentException("El numero de cuotas debe ser mayor a 0");
        this.numeroCuotas = numeroCuotas;
    }

    @Override
    public void procesarMedio(long monto) {
        long valorCuota = monto / numeroCuotas;
    }
}
