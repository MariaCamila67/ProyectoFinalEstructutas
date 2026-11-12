package org.example.clases.vehiculo;

import org.example.clases.personas.Cliente;

public class Gasolina extends Automovil {

    private ReglasNegocioAutomovil.Cilindraje cilindraje;

    public Gasolina(String placa, String marca, String modelo, int anio, Cliente cliente, ReglasNegocioAutomovil.Cilindraje cilindraje) {
        super(placa, marca, modelo, anio, cliente);
        this.cilindraje = cilindraje;
    }

    public ReglasNegocioAutomovil.Cilindraje getCilindraje() {
        return cilindraje;
    }
}
