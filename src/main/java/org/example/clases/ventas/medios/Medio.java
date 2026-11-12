package org.example.clases.ventas.medios;

import org.example.interfaces.IMedio;

public abstract class Medio implements IMedio {

    @Override
    public abstract void procesarMedio(long monto);
}
