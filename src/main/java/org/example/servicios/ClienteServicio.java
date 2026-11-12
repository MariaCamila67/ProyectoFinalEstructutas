package org.example.servicios;

import org.example.aspectos.ClienteValidador;
import org.example.clases.personas.Cliente;
import org.example.clases.personas.ReglasNegocioPersona;
import org.example.repositorios.ClienteRepositorio;

public class ClienteServicio {

    private ClienteRepositorio repositorio;
    private ClienteValidador validador;

    public ClienteServicio() {
        repositorio = new ClienteRepositorio();
        validador = new ClienteValidador();
    }

    public String guardar(Cliente cliente) {
        if (!cliente.isCredito())
            return "Solo se guardan clientes con credito";

        String error = validador.validar(cliente);
        if (error != null) return error;

        return repositorio.guardar(cliente);
    }

    public String actualizarSaldoPendiente(String id, long saldoPendiente) {
        if (saldoPendiente > ReglasNegocioPersona.maxSaldoPendiente)
            return "El saldo pendiente supera el maximo permitido";

        Cliente cliente = repositorio.buscarPorId(id);
        if (cliente == null)
            return "Cliente no encontrado";

        if (saldoPendiente > ReglasNegocioPersona.minSaldoPendiente && !cliente.isCredito())
            return "El cliente no puede tener saldo pendiente sin tener credito";

        return repositorio.actualizarSaldoPendiente(id, saldoPendiente);
    }

    public Cliente buscarPorId(String id) {
        return repositorio.buscarPorId(id);
    }
}
