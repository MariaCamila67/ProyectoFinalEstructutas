package org.example.aspectos;

import org.example.clases.personas.Cliente;
import org.example.clases.personas.ReglasNegocioPersona;

public class ClienteValidador {

    public String validar(Cliente cliente) {
        if (cliente.getSaldoPendiente() > ReglasNegocioPersona.maxSaldoPendiente)
            return "El saldo pendiente supera el maximo permitido";

        if (cliente.getSaldoPendiente() > ReglasNegocioPersona.minSaldoPendiente && !cliente.isCredito())
            return "El cliente no puede tener saldo pendiente sin tener credito";

        return null;
    }
}
