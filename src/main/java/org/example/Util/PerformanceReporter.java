package org.example.Util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;
import oshi.SystemInfo;

import java.util.Locale;

public class PerformanceReporter {

    private static final Logger log = LogManager.getLogger("JOL");
    private static final Logger logOshi = LogManager.getLogger("OSHI");
    private static final SystemInfo si = new SystemInfo();
    private static final long BYTES_PER_MB = 1024L * 1024L;

    public static void medirPesoObjeto(Object objeto, String nombreObjeto) {
        try {
        long pesoBytes = ClassLayout.parseInstance(objeto).instanceSize();
        long pesoTotal = GraphLayout.parseInstance(objeto).totalSize();

        log.info("Objeto '{}' | tamaño superficial: {} bytes | con referencias: {} bytes",
                nombreObjeto, pesoBytes, pesoTotal);

        } catch (RuntimeException ex) {
            log.error("No se pudo medir '{}' con JOL: {}", nombreObjeto, ex.getMessage());
        }
    }

    public static void reportarMemoriaSistema() {
        try {
        Runtime runtime = Runtime.getRuntime();

        long totalHeap = runtime.totalMemory();
        long usedHeap = totalHeap - runtime.freeMemory();
        long maxHeap = runtime.maxMemory();
        double porcentajeUso = totalHeap == 0 ? 0 : (100.0 * usedHeap) / totalHeap;

        long totalFisica = si.getHardware().getMemory().getTotal();
        long disponibleFisica = si.getHardware().getMemory().getAvailable();

        logOshi.info("Memoria JVM | usada: {} | asignada: {} | máxima: {} | uso: {}%",
                formatearMegabytes(usedHeap), formatearMegabytes(totalHeap),
                formatearMegabytes(maxHeap), formatearDecimal(porcentajeUso));

        logOshi.info("Memoria física | usada: {} | total: {} | disponible: {}",
                formatearMegabytes(totalFisica - disponibleFisica),
                formatearMegabytes(totalFisica), formatearMegabytes(disponibleFisica));

        } catch (RuntimeException ex) {
            logOshi.error("No se pudo consultar memoria con OSHI: {}", ex.getMessage());
        }
    }

    private static String formatearMegabytes(long bytes) {
        return formatearDecimal((double) bytes / BYTES_PER_MB) + " MB";
    }

    private static String formatearDecimal(double valor) {
        return String.format(Locale.ROOT, "%.2f", valor);
    }

}
