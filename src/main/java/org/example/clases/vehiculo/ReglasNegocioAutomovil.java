package org.example.clases.vehiculo;

import java.time.Year;

public final class ReglasNegocioAutomovil {

    private ReglasNegocioAutomovil() {
    }

    // validaciones del automovil
    public enum Cilindraje { C4, C6, C12, C16 }

    public static final int numeroBaterias = 2;

    public static final int longMinMarca = 2;

    public static final int longMaxMarca = 12;

    public static final int autonomiaMax = 100;

    public static final int autonomiaMin = 0;

    public static int anioActual() {
        return Year.now().getValue();
    }

    public static final String xrPlaca = "^[A-Z]{3}\\d{3}$";

    public static final String xrMarca = "^[a-zA-Z\\p{L}]{3,}$";

    // validaciones gasolina
    public static final int psiC4 = 45;

    public static final int psiC6 = 50;

    public static final int psiC12 = 55;

    public static final int psiC16 = 60;

    public static final int capacidadRadiador = 100;

    // validaciones hibrido y electrico
    public static final int desgasteMaxLlantas = 30;

    public static final int nivelMinAmortiguacion = 70;

    public static final int capacidadBateria = 100;

    // validaciones electrico
    public static final int autonomiaExcelente = 80;

    public static final int autonomiaBuena = 50;

    public static final int autonomiaModerada = 20;
}
