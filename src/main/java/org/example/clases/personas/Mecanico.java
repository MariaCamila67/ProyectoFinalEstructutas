package org.example.clases.personas;

public class Mecanico extends Persona {

    private ReglasNegocioPersona.Especialidades especialidad;
    private String contrasena;

    public Mecanico(String id, String nombre, String telefono, ReglasNegocioPersona.Especialidades especialidad, String contrasena) {
        super(id, nombre, telefono);
        this.especialidad = especialidad;
        setContrasena(contrasena);
    }

    public ReglasNegocioPersona.Especialidades getEspecialidad() {
        return especialidad;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        if (contrasena == null || contrasena.isBlank() || contrasena.length() < ReglasNegocioPersona.longMinContrasena)
            throw new IllegalArgumentException("La contraseña debe tener minimo 6 caracteres");
        this.contrasena = contrasena;
    }
}
