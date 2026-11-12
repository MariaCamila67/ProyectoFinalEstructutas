package org.example.servicios;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.example.clases.repuestos.Repuesto;

public class ServicioRepuesto {

    private static final DateTimeFormatter FECHA_DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Path rutaArchivo;

    public ServicioRepuesto(String rutaArchivo) {
        this.rutaArchivo = Paths.get(Objects.requireNonNull(rutaArchivo, "rutaArchivo"));
    }

    public List<Repuesto> cargarRepuestos() {
        try {
            validarArchivo();

            List<Repuesto> repuestos = new ArrayList<>();

            for (String linea : Files.readAllLines(rutaArchivo)) {
                if (linea.isBlank()) continue;

                String[] campos = dividir(linea);
                LocalDateTime fecha = campos.length == 3 ? parsearFecha(campos[2]) : null;

                if (fecha != null)
                    repuestos.add(new Repuesto(campos[0], campos[1], fecha));
            }

            return repuestos;
        } catch (Exception ex) {
            throw new RuntimeException("Error al cargar repuestos: " + ex.getMessage(), ex);
        }
    }

    public Repuesto consultarRepuesto(String nombre) {
        if (nombre == null || nombre.isBlank()) return null;

        try {
            validarArchivo();
            String nombreBuscado = nombre.trim();

            for (String linea : Files.readAllLines(rutaArchivo)) {
                if (linea.isBlank()) continue;

                String[] campos = dividir(linea);

                if (campos.length == 3 && campos[0].equalsIgnoreCase(nombreBuscado)) {
                    LocalDateTime fecha = parsearFecha(campos[2]);
                    if (fecha != null)
                        return new Repuesto(campos[0], campos[1], fecha);
                }
            }

            return null;
        } catch (Exception ex) {
            throw new RuntimeException("Error al consultar repuesto '" + nombre + "': " + ex.getMessage(), ex);
        }
    }

    private void validarArchivo() throws FileNotFoundException {
        if (!Files.exists(rutaArchivo))
            throw new FileNotFoundException("El archivo de repuestos no existe: " + rutaArchivo);
    }

    private String[] dividir(String linea) {
        String[] campos = linea.split("\\|");
        for (int i = 0; i < campos.length; i++) campos[i] = campos[i].trim();
        return campos;
    }


    private LocalDateTime parsearFecha(String texto) {
        try {
            return LocalDateTime.parse(texto);
        } catch (DateTimeParseException e1) {
            try {
                return LocalDate.parse(texto).atStartOfDay();
            } catch (DateTimeParseException e2) {
                try {
                    return LocalDate.parse(texto, FECHA_DD_MM_YYYY).atStartOfDay();
                } catch (DateTimeParseException e3) {
                    return null;
                }
            }
        }
    }
}
