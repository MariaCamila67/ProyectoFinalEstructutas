package org.example.repositorios;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.example.clases.facturas.Factura;
import org.example.clases.personas.Cliente;
import org.example.clases.vehiculo.Automovil;
import org.example.clases.ventas.Venta;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FacturaRepositorioTest {

    @Test
    void guardarConservaDatosYUsaElMismoIdAlReintentar() {
        List<Document> guardados = new ArrayList<>();
        MongoCollection<Document> coleccion = proxy(MongoCollection.class, (objeto, metodo, argumentos) -> {
            if (!metodo.getName().equals("replaceOne")) throw new UnsupportedOperationException();
            assertTrue(((ReplaceOptions) argumentos[2]).isUpsert());
            guardados.add((Document) argumentos[1]);
            return null;
        });
        FacturaRepositorio repositorio = new FacturaRepositorio(coleccion);
        Cliente cliente = new Cliente("123", "Juan Torres", "3056789432", 0, true, 5000000);
        Automovil auto = new Automovil("ABC123", "Toyota", "Corolla", 2020, cliente);
        Factura factura = new Factura(new Venta(monto -> {}, monto -> {}, 250000), auto, cliente);
        factura.setEstado("Credito");

        repositorio.guardarFactura(factura);
        repositorio.guardarFactura(factura);

        assertEquals(guardados.get(0).get("_id"), guardados.get(1).get("_id"));
        Document documento = guardados.get(0);
        assertEquals("ABC123", documento.getString("placa"));
        assertEquals("123", documento.getString("clienteId"));
        assertEquals("Juan Torres", documento.getString("clienteNombre"));
        assertEquals(250000L, documento.getLong("monto"));
        assertEquals("Credito", documento.getString("estado"));
        assertInstanceOf(Date.class, documento.get("fecha"));
    }

    @Test
    void falloDeGuardadoSePropagaParaInterrumpirLaSalidaDelVehiculo() {
        RuntimeException fallo = new RuntimeException("Sin conexion");
        MongoCollection<Document> coleccion = proxy(MongoCollection.class, (objeto, metodo, argumentos) -> {
            throw fallo;
        });
        Cliente cliente = new Cliente("123", "Juan", "3056789432", 0, false, 0);
        Factura factura = new Factura(new Venta(monto -> {}, monto -> {}, 50000),
                new Automovil("ABC123", "Toyota", "Corolla", 2020, cliente), cliente);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new FacturaRepositorio(coleccion).guardarFactura(factura));
        assertSame(fallo, error.getCause());
    }

    @Test
    void consultaSinResultadosInformaQueNoExisteLaFactura() {
        FindIterable<Document> resultados = proxy(FindIterable.class, (objeto, metodo, argumentos) -> {
            if (metodo.getName().equals("sort")) return objeto;
            if (metodo.getName().equals("first")) return null;
            throw new UnsupportedOperationException();
        });
        MongoCollection<Document> coleccion = proxy(MongoCollection.class, (objeto, metodo, argumentos) -> {
            if (metodo.getName().equals("find")) return resultados;
            throw new UnsupportedOperationException();
        });
        assertEquals("Factura no encontrada",
                new FacturaRepositorio(coleccion).consultarEstadoFactura("ABC123"));
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<?> tipo, java.lang.reflect.InvocationHandler manejador) {
        return (T) Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo}, manejador);
    }
}
