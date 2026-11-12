package org.example.clases.ventas;

import org.example.interfaces.IMedio;
import org.example.interfaces.IPago;

public class Venta implements IPago, IMedio {

    private IPago pago;
    private IMedio medio;
    private long monto;

    public Venta(IPago pago, IMedio medio, long monto) {
        this.pago = pago;
        this.medio = medio;
        this.monto = monto;
    }

    public long getMonto() {
        return monto;
    }

    public void setMonto(long monto) {
        this.monto = monto;
    }

    @Override
    public void procesarConcepto(long monto) {
        pago.procesarConcepto(monto);
    }

    @Override
    public void procesarMedio(long monto) {
        medio.procesarMedio(monto);
    }

    public void ejecutarVenta() {
        procesarConcepto(monto);
        procesarMedio(monto);
    }

}
