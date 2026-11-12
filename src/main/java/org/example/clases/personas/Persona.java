package org.example.clases.personas;

import java.util.regex.Pattern;

public class Persona {

    private String id;
    private String nombre;
    private String telefono;

    public Persona(String id, String nombre, String telefono) {
        setId(id);
        setNombre(nombre);
        setTelefono(telefono);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || !Pattern.matches(ReglasNegocioPersona.xrId, id.trim()))
            throw new IllegalArgumentException("El id no es valido");
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() < ReglasNegocioPersona.longMinNombre)
            throw new IllegalArgumentException("El nombre no es valido");
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        if (telefono == null || !Pattern.matches(ReglasNegocioPersona.xrTelefono, telefono.trim()))
            throw new IllegalArgumentException("El telefono no es valido");
        this.telefono = telefono;
    }
}
