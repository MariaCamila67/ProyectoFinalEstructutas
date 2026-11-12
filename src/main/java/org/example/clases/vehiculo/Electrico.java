package org.example.clases.vehiculo;

import org.example.clases.personas.Cliente;

public class Electrico extends Automovil {

    private int autonomia;

    public Electrico(String placa, String marca, String modelo, int anio, Cliente cliente, int autonomia) {
        super(placa, marca, modelo, anio, cliente);
        setAutonomia(autonomia);
    }

    public int getAutonomia() {
        return autonomia;
    }

    public void setAutonomia(int autonomia) {
        if (autonomia < ReglasNegocioAutomovil.autonomiaMin || autonomia > ReglasNegocioAutomovil.autonomiaMax)
            throw new IllegalArgumentException("La cantidad de la autonomia no es valida");
        this.autonomia = autonomia;
    }
}
