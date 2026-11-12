package org.example.clases.reparaciones;

import org.example.clases.personas.Mecanico;
import org.example.clases.repuestos.Repuesto;
import org.example.clases.vehiculo.Automovil;
import org.example.interfaces.IMantenimiento;

import java.time.LocalDateTime;
import java.util.List;

public class Reparacion {

    private Automovil automovil;
    private LocalDateTime fecha;
    private List<Repuesto> repuestos;
    private long valorReparacion;
    private List<Mecanico> mecanicos;

    public Reparacion(Automovil automovil, LocalDateTime fecha, List<Repuesto> repuestos, long valorReparacion, List<Mecanico> mecanicos) {
        this.automovil = automovil;
        this.fecha = fecha;
        this.repuestos = repuestos;
        setValorReparacion(valorReparacion);
        this.mecanicos = mecanicos;
    }

    public long getValorReparacion() {
        return valorReparacion;
    }

    public void setValorReparacion(long valorReparacion) {
        if (valorReparacion < ReglasNegocioReparacion.minValorReparacion)
            throw new IllegalArgumentException("El valor de reparacion no debe ser menor a 0");
        this.valorReparacion = valorReparacion;
    }

    public List<Repuesto> getRepuestos() {
        return repuestos;
    }

    public List<Mecanico> getMecanicos() {
        return mecanicos;
    }

    public Automovil getAutomovil() {
        return automovil;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }


    public String ejecutarPuestaAPunto(IMantenimiento eleccionMantenimiento) {
        try {
            if (eleccionMantenimiento == null)
                return "Error: No se ha provisto una estrategia de mantenimiento válida.";

            if (this.automovil == null)
                return "Error: No se puede realizar la puesta a punto. No hay un automóvil asignado en la reparación.";

            return eleccionMantenimiento.puestaAPunto(this);
        } catch (Exception ex) {
            return "Error durante la ejecución de la puesta a punto: " + ex.getMessage();
        }
    }
}
