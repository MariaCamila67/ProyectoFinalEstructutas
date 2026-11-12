package org.example.clases.personas;

import org.example.clases.reparaciones.Reparacion;
import org.example.clases.vehiculo.Automovil;

public class Cliente extends Persona {

    private long saldoPendiente;
    private boolean credito;
    private Reparacion reparacion;
    private long ingresosMensuales;
    private long valorReparacion;

    public Cliente(String id, String nombre, String telefono, long saldoPendiente, boolean credito, long ingresosMensuales) {
        super(id, nombre, telefono);
        this.credito = credito;
        this.ingresosMensuales = ingresosMensuales;
        this.saldoPendiente = saldoPendiente;
    }

    public boolean isCredito() {
        return credito;
    }

    public void setCredito(boolean credito) {
        this.credito = credito;
    }

    public long getSaldoPendiente() {
        return saldoPendiente;
    }

    public void setSaldoPendiente(long saldoPendiente) {
        this.saldoPendiente = saldoPendiente;
    }

    public long getValorReparacion() {
        return valorReparacion;
    }

    public String asignarCredito() {
        if (ingresosMensuales < ReglasNegocioPersona.valorAprobacion) {
            return "Hemos recibido su solicitud pero lamentablemente en este momento no podemos asignarle un credito, "
                    + "lo invitamos a mantenerse en contacto con la empresa e intentarlo nuevamente en otra ocasion. "
                    + "Gracias por la comprension";
        } else {
            credito = true;
            return "Hemos recibido su solicitud y ha sido aprobada, felicitaciones por su credito";
        }
    }

    public String cancelarCredito() {
        if (credito) {
            credito = false;
            return "Su credito ha sido cancelado exitosamente";
        } else {
            return "Usted no posee un credito actualmente, en caso de querer solicitarlo "
                    + "lo invitamos a seleccionar la opcion correspondiente";
        }
    }

    public void asignarReparacion(Reparacion reparacion) {
        this.reparacion = reparacion;
    }

    public Automovil getAutomovil() {
        return reparacion.getAutomovil();
    }

    public String gestionarCredito(String respuestaCredito) {
        if (credito && "SI".equals(respuestaCredito)) {
            saldoPendiente += reparacion.getValorReparacion();
            return "Saldo cargado al credito. Saldo pendiente: " + saldoPendiente;
        } else if (!credito || "NO".equals(respuestaCredito)) {
            valorReparacion = reparacion.getValorReparacion();
            return "Debe pagar: " + valorReparacion;
        } else {
            return "Respuesta invalida";
        }
    }
}
