package org.example.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class FiltroTiempoApi extends OncePerRequestFilter {

    private static final Logger log = LogManager.getLogger("tiempos");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long inicio = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long tiempoNs = System.nanoTime() - inicio;
            int estado = response.getStatus();
            String resultado = estado < 400 ? "exito" : "error";
            log.info("Tiempo de ejecucion {}ns | operacion=solicitudApi | metodo={} | ruta={} | resultado={} | estado={}",
                    tiempoNs, request.getMethod(), rutaSegura(request.getRequestURI()), resultado, estado);
        }
    }

    // Evita guardar documentos de clientes o placas en el log.
    private String rutaSegura(String ruta) {
        return ruta.replaceAll("(?<=/clients/)[^/]+", "{id}")
                .replaceAll("(?<=/invoices/status/)[^/]+", "{placa}")
                .replaceAll("(?<=/invoices/plate/)[^/]+", "{placa}");
    }
}
