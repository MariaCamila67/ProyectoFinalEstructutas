package org.example.clases.reparaciones.mantenimientos;

import org.example.clases.reparaciones.Reparacion;
import org.example.clases.vehiculo.Gasolina;
import org.example.clases.vehiculo.ReglasNegocioAutomovil;

public class MantenimientoGasolina extends Mantenimiento {

    @Override
    public String puestaAPunto(Reparacion reparacion) {
        try {
            if (!(reparacion.getAutomovil() instanceof Gasolina))
                return "El vehículo no es de tipo gasolina, verifique si solicito el mantenimiento adecuado";

            Gasolina auto = (Gasolina) reparacion.getAutomovil();

            int psi;
            switch (auto.getCilindraje()) {
                case C4:
                    psi = ReglasNegocioAutomovil.psiC4;
                    break;
                case C6:
                    psi = ReglasNegocioAutomovil.psiC6;
                    break;
                case C12:
                    psi = ReglasNegocioAutomovil.psiC12;
                    break;
                case C16:
                    psi = ReglasNegocioAutomovil.psiC16;
                    break;
                default:
                    return "Error: cilindraje no reconocido";
            }

            return "PUESTA A PUNTO GASOLINA"
                    + "\nVehículo: " + auto.getMarca() + " " + auto.getModelo() + " | Placa: " + auto.getPlaca()
                    + "\nCilindraje: " + auto.getCilindraje()
                    + "\nCorreas: cambiadas"
                    + "\nBujías: ajustadas a " + psi + " PSI según cilindraje " + auto.getCilindraje()
                    + "\nRadiador: drenado (capacidad " + ReglasNegocioAutomovil.capacidadRadiador + "L)"
                    + "\nInyectores: ajustados";
        } catch (Exception ex) {
            return "Error durante la puesta a punto de vehiculo gasolina: " + ex.getMessage();
        }
    }
}
