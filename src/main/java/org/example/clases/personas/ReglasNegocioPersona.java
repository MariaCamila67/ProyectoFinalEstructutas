package org.example.clases.personas;

public final class ReglasNegocioPersona {

    private ReglasNegocioPersona() {
    }

    public enum Especialidades { MECANICO, ELECTRICISTA, PINTURA, INYECCION }

    public static final int longMinNombre = 1;

    public static final int longMinContrasena = 6;

    public static final long maxSaldoPendiente = 20000000;

    public static final long minSaldoPendiente = 0;

    public static final long valorAprobacion = 4500000;

    public static final String xrTelefono = "^3\\d{9}$";

    public static final String xrId = "^\\d{1,10}$";
}
