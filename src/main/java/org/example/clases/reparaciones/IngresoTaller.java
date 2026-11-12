package org.example.clases.reparaciones;

import org.example.clases.vehiculo.Automovil;

public class IngresoTaller {

    public enum Tipo { MANTENIMIENTO, ACCIDENTE }

    private final Reparacion reparacion;
    private final Tipo tipo;
    private final int nivelDanio;   // solo aplica a ACCIDENTE (mayor numero = mas grave)
    private final long turno;       // orden de llegada (desempata la prioridad)
    private boolean credito;
    private boolean pagada;
    private boolean atendido;

    public IngresoTaller(Reparacion reparacion, Tipo tipo, int nivelDanio, long turno) {
        this.reparacion = reparacion;
        this.tipo = tipo;
        this.nivelDanio = nivelDanio;
        this.turno = turno;
    }

    public Reparacion getReparacion() {
        return reparacion;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public int getNivelDanio() {
        return nivelDanio;
    }

    public long getTurno() {
        return turno;
    }

    public boolean isCredito() {
        return credito;
    }

    public void setCredito(boolean credito) {
        this.credito = credito;
    }

    public boolean isPagada() {
        return pagada;
    }

    public void setPagada(boolean pagada) {
        this.pagada = pagada;
    }

    public boolean isAtendido() {
        return atendido;
    }

    public void setAtendido(boolean atendido) {
        this.atendido = atendido;
    }

    public String descripcion() {
        Automovil a = reparacion.getAutomovil();
        String base = "Turno #" + turno + " | Placa: " + a.getPlaca() + " | " + a.getMarca() + " " + a.getModelo()
                + " | Cliente: " + a.getCliente().getNombre();
        return tipo == Tipo.ACCIDENTE ? base + " | Nivel de dano: " + nivelDanio : base;
    }
}
