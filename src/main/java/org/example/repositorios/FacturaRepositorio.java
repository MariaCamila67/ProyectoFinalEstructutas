package org.example.repositorios;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.example.clases.facturas.Factura;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class FacturaRepositorio {

    private final MongoCollection<Document> coleccion;

    public FacturaRepositorio() {
        this(ConexionMongo.uri());
    }

    public FacturaRepositorio(String uri) {
        this(MongoClients.create(uri).getDatabase("ProyectoTaller").getCollection("Facturas"));
    }

    public FacturaRepositorio(MongoCollection<Document> coleccion) {
        this.coleccion = coleccion;
    }

    public String guardarFactura(Factura factura) {
        Document documento = new Document("_id", factura.getId())
                .append("placa", factura.getPlaca())
                .append("clienteId", factura.getClienteId())
                .append("clienteNombre", factura.getClienteNombre())
                .append("monto", factura.getMonto())
                .append("estado", factura.getEstado())
                .append("fecha", factura.getFecha());
        try {
            // Reintentar el guardado de la misma factura no crea otra factura.
            coleccion.replaceOne(Filters.eq("_id", factura.getId()), documento,
                    new ReplaceOptions().upsert(true));
            return "Factura guardada exitosamente en MongoDB";
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo guardar la factura en MongoDB: " + ex.getMessage(), ex);
        }
    }

    public List<String> consultarFacturasPagadas() {
        return consultar(Filters.eq("estado", "Pagada"));
    }

    public List<String> buscarPorPlaca(String placa) {
        return consultar(Filters.eq("placa", placa));
    }

    public String consultarEstadoFactura(String placa) {
        try {
            Document documento = coleccion.find(Filters.eq("placa", placa))
                    .sort(Sorts.descending("fecha")).first();
            return documento == null ? "Factura no encontrada"
                    : "Factura de " + placa + " - Estado: " + documento.getString("estado");
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo consultar la factura en MongoDB: " + ex.getMessage(), ex);
        }
    }

    private List<String> consultar(Bson filtro) {
        try {
            List<String> facturas = new ArrayList<>();
            try (var cursor = coleccion.find(filtro).sort(Sorts.descending("fecha")).iterator()) {
                while (cursor.hasNext()) facturas.add(formatear(cursor.next()));
            }
            return facturas;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudieron consultar las facturas en MongoDB: " + ex.getMessage(), ex);
        }
    }

    private String formatear(Document documento) {
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String fecha = formato.format(documento.getDate("fecha"));
        return "Factura\nPlaca: " + documento.getString("placa")
                + "\nCliente: " + documento.getString("clienteNombre")
                + "\nMonto: " + documento.get("monto")
                + "\nFecha: " + fecha
                + "\nEstado: " + documento.getString("estado");
    }
}
