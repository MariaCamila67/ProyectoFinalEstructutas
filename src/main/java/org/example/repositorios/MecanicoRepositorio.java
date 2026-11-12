package org.example.repositorios;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.example.clases.personas.Mecanico;
import org.example.clases.personas.ReglasNegocioPersona;

import java.util.Locale;

public class MecanicoRepositorio {

    private static final String NOMBRE_BD = "ProyectoTaller";
    private static final String NOMBRE_COLECCION = "Mecanicos";

    private final MongoCollection<Document> coleccion;

    public MecanicoRepositorio() {
        this(ConexionMongo.uri());
    }

    public MecanicoRepositorio(String uri) {

        MongoClient client = MongoClients.create(uri);
        coleccion = client.getDatabase(NOMBRE_BD).getCollection(NOMBRE_COLECCION);
    }

    public Mecanico buscarPorCredenciales(String nombre, String contrasena) {
        if (nombre == null || nombre.isBlank() || contrasena == null || contrasena.isBlank())
            return null;

        try {
            Document documento = coleccion.find(Filters.and(
                    Filters.eq("nombre", nombre.trim()),
                    Filters.eq("contrasena", contrasena.trim())
            )).first();

            if (documento == null) return null;

            // Admite el id del dominio en "id" o como "_id" de tipo String.
            String id = documento.getString("id");
            if (id == null && documento.get("_id") instanceof String)
                id = documento.getString("_id");
            String especialidad = documento.getString("especialidad");
            if (especialidad == null || especialidad.isBlank())
                throw new IllegalArgumentException("El mecanico no tiene especialidad registrada");

            return new Mecanico(
                    id,
                    documento.getString("nombre"),
                    documento.getString("telefono"),
                    ReglasNegocioPersona.Especialidades.valueOf(especialidad.trim().toUpperCase(Locale.ROOT)),
                    documento.getString("contrasena"));
        } catch (Exception ex) {
            throw new IllegalStateException("Error al consultar el mecanico en MongoDB: " + ex.getMessage(), ex);
        }
    }




}

