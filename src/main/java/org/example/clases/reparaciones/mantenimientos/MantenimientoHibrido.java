package org.example.clases.reparaciones.mantenimientos;

import org.example.clases.reparaciones.Reparacion;
import org.example.clases.vehiculo.Hibrido;
import org.example.clases.vehiculo.ReglasNegocioAutomovil;

public class MantenimientoHibrido extends Mantenimiento {

    @Override
    public String puestaAPunto(Reparacion reparacion) {
        try {
            if (!(reparacion.getAutomovil() instanceof Hibrido))
                return "Error: el vehículo no es de tipo híbrido";

            Hibrido auto = (Hibrido) reparacion.getAutomovil();

            int cargaPorBateria = ReglasNegocioAutomovil.capacidadBateria / auto.getNumeroBaterias();

            return "PUESTA A PUNTO - HÍBRIDO"
                    + "\nVehículo: " + auto.getMarca() + " " + auto.getModelo() + " | Placa: " + auto.getPlaca()
                    + "\nNúmero de baterías: " + auto.getNumeroBaterias()
                    + "\nMotor generador eléctrico: lubricado"
                    + "\nFrenos: cambiados"
                    + "\nBaterías: " + auto.getNumeroBaterias() + " baterías recargadas a " + cargaPorBateria + "% cada una"
                    + "\nLlantas: desgaste máximo permitido " + ReglasNegocioAutomovil.desgasteMaxLlantas + "% — revisadas"
                    + "\nAmortiguación: nivel mínimo requerido " + ReglasNegocioAutomovil.nivelMinAmortiguacion + "% — calibrada"
                    + "\nLíquido de frenos: cambiado";
        } catch (Exception ex) {
            return "Error durante la puesta a punto de vehiculo hibrido: " + ex.getMessage();
        }
    }
}
