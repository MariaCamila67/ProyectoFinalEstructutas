package org.example.aspectos;

import org.example.clases.personas.Mecanico;
import org.example.clases.personas.ReglasNegocioPersona;
import org.example.repositorios.MecanicoRepositorio;

public class MecanicoValidador {

    private final MecanicoRepositorio repositorio;

    public MecanicoValidador(MecanicoRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public Mecanico autenticar(String nombre, String contrasena) {
        if (nombre == null || nombre.isBlank() || contrasena == null || contrasena.isBlank())
            return null;

        if (contrasena.length() < ReglasNegocioPersona.longMinContrasena)
            return null;

        return repositorio.buscarPorCredenciales(nombre, contrasena);
    }
}
