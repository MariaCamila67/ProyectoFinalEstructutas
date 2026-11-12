package org.example.clases.ventas.pagos;

import org.example.clases.personas.Cliente;

public class PagarSaldoPendiente extends Pago {

    private Cliente cliente;

    public PagarSaldoPendiente(Cliente cliente) {
        this.cliente = cliente;
    }

    @Override
    public void procesarConcepto(long monto) {
        if (monto > cliente.getSaldoPendiente())
            throw new IllegalArgumentException("El monto supera el saldo pendiente");

        cliente.setSaldoPendiente(cliente.getSaldoPendiente() - monto);
    }
}
