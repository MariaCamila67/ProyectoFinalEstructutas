package org.example.clases.reparaciones.mantenimientos;

import org.example.clases.reparaciones.Reparacion;
import org.example.clases.vehiculo.Electrico;
import org.example.clases.vehiculo.ReglasNegocioAutomovil;

public class MantenimientoElectrico extends Mantenimiento {

    @Override
    public String puestaAPunto(Reparacion reparacion) {
        try {
            if (!(reparacion.getAutomovil() instanceof Electrico))
                return "Error: el vehículo no es de tipo eléctrico";

            Electrico auto = (Electrico) reparacion.getAutomovil();

            String estadoBateria;
            String accion;

            if (auto.getAutonomia() >= ReglasNegocioAutomovil.autonomiaExcelente) {
                estadoBateria = "Excelente";
                accion = "sin necesidad de recarga inmediata";
            } else if (auto.getAutonomia() >= ReglasNegocioAutomovil.autonomiaBuena) {
                estadoBateria = "Buena";
                accion = "se recomienda recarga preventiva";
            } else if (auto.getAutonomia() >= ReglasNegocioAutomovil.autonomiaModerada) {
                estadoBateria = "Moderada";
                accion = "recarga necesaria próximamente";
            } else {
                estadoBateria = "Crítica";
                accion = "recarga inmediata requerida";
            }

            return "PUESTA A PUNTO ELÉCTRICO"
                    + "\nVehículo: " + auto.getMarca() + " " + auto.getModelo() + " | Placa: " + auto.getPlaca()
                    + "\nAutonomía actual: " + auto.getAutonomia() + "%"
                    + "\nBatería: estado " + estadoBateria + " (" + auto.getAutonomia() + "%) — " + accion
                    + "\nLubricantes: cambiados"
                    + "\nLlantas: desgaste máximo permitido " + ReglasNegocioAutomovil.desgasteMaxLlantas + "% — revisadas"
                    + "\nAmortiguación: nivel mínimo requerido " + ReglasNegocioAutomovil.nivelMinAmortiguacion + "% — calibrada"
                    + "\nLíquido de frenos: cambiado";
        } catch (Exception ex) {
            return "Error durante la puesta a punto de vehiculo electrico: " + ex.getMessage();
        }
    }
}
