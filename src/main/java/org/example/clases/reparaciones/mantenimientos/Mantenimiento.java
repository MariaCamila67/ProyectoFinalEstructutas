package org.example.clases.reparaciones.mantenimientos;

import org.example.clases.reparaciones.Reparacion;
import org.example.interfaces.IMantenimiento;

public abstract class Mantenimiento implements IMantenimiento {

    @Override
    public abstract String puestaAPunto(Reparacion reparacion);
}
