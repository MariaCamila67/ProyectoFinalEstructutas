package org.example.repositorios;

final class ConexionMongo {
    private ConexionMongo() {
    }

    static String uri() {
        String configurada = System.getenv("MONGODB_URI");
        return configurada == null || configurada.isBlank()
                ? "mongodb://localhost:27017"
                : configurada;
    }
}
