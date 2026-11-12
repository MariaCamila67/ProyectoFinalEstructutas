package org.example.clases.vehiculo;

import org.example.clases.personas.Cliente;

public class Hibrido extends Automovil {

    private int numeroBaterias;

    public Hibrido(String placa, String marca, String modelo, int anio, Cliente cliente) {
        super(placa, marca, modelo, anio, cliente);
        this.numeroBaterias = ReglasNegocioAutomovil.numeroBaterias;
    }

    public int getNumeroBaterias() {
        return numeroBaterias;
    }
}
