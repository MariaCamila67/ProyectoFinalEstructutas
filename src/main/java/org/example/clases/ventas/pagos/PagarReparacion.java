package org.example.clases.ventas.pagos;

import org.example.clases.reparaciones.Reparacion;

public class PagarReparacion extends Pago {

    private Reparacion reparacion;

    public PagarReparacion(Reparacion reparacion) {
        this.reparacion = reparacion;
    }

    @Override
    public void procesarConcepto(long monto) {
        if (monto != reparacion.getValorReparacion())
            throw new IllegalArgumentException("El monto no corresponde al valor de la reparacion");
    }
}
