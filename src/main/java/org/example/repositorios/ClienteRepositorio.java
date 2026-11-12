package org.example.repositorios;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

import org.example.clases.personas.Cliente;


public class ClienteRepositorio {

    private static final Logger log = LogManager.getLogger("tiempos");

    private static final String NOMBRE_BD = "ProyectoTaller";
    private static final String NOMBRE_COLECCION = "ClientesConCredito";

    private final MongoCollection<Document> coleccion;

    public ClienteRepositorio() {
        this(ConexionMongo.uri());
    }

    public ClienteRepositorio(String uri) {

        MongoClient client = MongoClients.create(uri);
        coleccion = client.getDatabase(NOMBRE_BD).getCollection(NOMBRE_COLECCION);
    }

    public String guardar(Cliente cliente) {
        long inicio = System.nanoTime();
        String resultado = "error";

        try {
            coleccion.insertOne(aDocumento(cliente));
            resultado = "exito";
            return "Cliente guardado exitosamente";
        } catch (Exception e) {
            return "Error al guardar el cliente: " + e.getMessage();
        } finally {
            long tiempoNs = System.nanoTime() - inicio;
            log.info("Tiempo de ejecucion {}ns | operacion=guardarCliente | origen=MongoDB | resultado={}", tiempoNs, resultado);
        }
    }

    public String actualizarSaldoPendiente(String id, long saldoPendiente) {
        long inicio = System.nanoTime();
        String resultado = "error";
        try {
            Bson filtro = Filters.eq("_id", id);
            coleccion.updateOne(filtro, Updates.set("saldoPendiente", saldoPendiente));
            resultado = "exito";
            return "Saldo pendiente actualizado exitosamente";
        } catch (Exception e) {
            return "Error al actualizar el saldo pendiente: " + e.getMessage();
        } finally {
            long tiempoNs = System.nanoTime() - inicio;
            log.info("Tiempo de ejecucion {}ns | operacion=actualizarSaldoCliente | origen=MongoDB | resultado={}", tiempoNs, resultado);
        }
    }

    public Cliente buscarPorId(String id) {
        long inicio = System.nanoTime();
        String resultado = "error";
        try {
            Document doc = coleccion.find(Filters.eq("_id", id)).first();
            Cliente cliente = doc == null ? null : aCliente(doc);
            resultado = cliente == null ? "no encontrado" : "encontrado";
            return cliente;
        } catch (Exception e) {
            resultado = "error de base de datos";
            return null;
        } finally {
            long tiempoNs = System.nanoTime() - inicio;
            log.info("Tiempo de ejecucion {}ns | operacion=buscarClientePorId | origen=MongoDB | resultado={}", tiempoNs, resultado);
        }
    }

    private Document aDocumento(Cliente c) {
        return new Document("_id", c.getId())
                .append("nombre", c.getNombre())
                .append("telefono", c.getTelefono())
                .append("credito", c.isCredito())
                .append("saldoPendiente", c.getSaldoPendiente());
    }

    private Cliente aCliente(Document d) {
        Number saldo = (Number) d.get("saldoPendiente");
        return new Cliente(
                d.getString("_id"),
                d.getString("nombre"),
                d.getString("telefono"),
                saldo == null ? 0 : saldo.longValue(),
                Boolean.TRUE.equals(d.getBoolean("credito")),
                0);
    }
}
