package org.example.clases.ventas.pagos;

import org.example.interfaces.IPago;

public abstract class Pago implements IPago {

    @Override
    public abstract void procesarConcepto(long monto);
}
