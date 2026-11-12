package org.example.servicios;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.example.clases.reparaciones.IngresoTaller;
import org.example.clases.reparaciones.Reparacion;
import org.example.clases.repuestos.Repuesto;
import org.example.estructuras.Cola;
import org.example.Util.PerformanceReporter;

public class TallerServicio {

    public static final int DANIO_MIN = 1;
    public static final int DANIO_MAX = 5;

    private final ServicioRepuesto servicioRepuesto;

    // 1. BOLSA: inventario de repuestos -> F4
    private final Bag<Repuesto> inventario = new HashBag<>();

    // 2. COLA (FIFO): vehiculos que ingresan por mantenimiento, en orden de llegada  -> F5
    private final Cola<IngresoTaller> colaMantenimiento = new Cola<>();

    // 3. COLA DE PRIORIDAD: vehiculos accidentados, primero el de mayor nivel de dano  -> F10
    private final PriorityQueue<IngresoTaller> colaAccidentes = new PriorityQueue<>(
            (a, b) -> {
                if (a.getNivelDanio() != b.getNivelDanio())
                    return b.getNivelDanio() - a.getNivelDanio();   // mayor dano primero
                return Long.compare(a.getTurno(), b.getTurno());    // si empatan, el que llego antes
            });

    private long contadorTurnos = 0;

    // Cola auxiliar de vehículos atendidos que aún no registran su salida.
    private final Cola<IngresoTaller> colaAtendidos = new Cola<>();

    public TallerServicio(ServicioRepuesto servicioRepuesto) {
        this.servicioRepuesto = servicioRepuesto;
    }
    //Bolsa

    private String clave(String nombre) {
        return nombre.trim().toLowerCase();
    }

    public int cargarInventario() {
        List<Repuesto> cargados = servicioRepuesto.cargarRepuestos();

        inventario.clear();
        for (Repuesto r : cargados) inventario.add(r);
        PerformanceReporter.medirPesoObjeto(inventario, "Bolsa de repuestos (HashBag)");
        return cargados.size();
    }

    public boolean inventarioVacio() {
        return inventario.isEmpty();
    }

    public int cantidadRepuesto(String nombre) {
        return contarRepuesto(nombre);
    }

    private int contarRepuesto(String nombre) {
        int cantidad = 0;
        String buscado = clave(nombre);
        for (Repuesto repuesto : inventario)
            if (clave(repuesto.getNombre()).equals(buscado)) cantidad++;
        return cantidad;
    }

    public List<String> inventario() {
        List<String> resultado = new ArrayList<>();
        List<String> vistos = new ArrayList<>();
        for (Repuesto repuesto : inventario) {
            String nombre = clave(repuesto.getNombre());
            if (!vistos.contains(nombre)) {
                vistos.add(nombre);
                resultado.add(repuesto.getNombre() + ": " + contarRepuesto(nombre));
            }
        }
        return resultado;
    }

    public List<Repuesto> seleccionarRepuestos(List<String> nombres) {
        List<String> pedidos = new ArrayList<>();
        for (String nombre : nombres) {
            if (nombre.isBlank()) continue;
            pedidos.add(clave(nombre));
        }

        List<String> faltantes = new ArrayList<>();
        List<String> revisados = new ArrayList<>();
        for (String pedido : pedidos) {
            if (revisados.contains(pedido)) continue;
            revisados.add(pedido);
            int solicitados = 0;
            for (String otro : pedidos) if (otro.equals(pedido)) solicitados++;
            int disponibles = contarRepuesto(pedido);
            if (disponibles < solicitados)
                faltantes.add(pedido + " (faltan " + (solicitados - disponibles) + ")");
        }
        if (!faltantes.isEmpty())
            throw new IllegalArgumentException("Repuestos que deben solicitarse: " + String.join(", ", faltantes));

        List<Repuesto> seleccionados = new ArrayList<>();
        for (String pedido : pedidos)
            for (Repuesto repuesto : inventario)
                if (clave(repuesto.getNombre()).equals(pedido)) {
                    seleccionados.add(repuesto);
                    break;
                }
        return seleccionados;
    }


    public void consumirRepuestos(List<Repuesto> usados) {
        for (Repuesto r : usados) {
            Repuesto encontrado = null;
            for (Repuesto disponible : inventario)
                if (clave(disponible.getNombre()).equals(clave(r.getNombre()))) {
                    encontrado = disponible;
                    break;
                }
            if (encontrado == null || !inventario.remove(encontrado))
                throw new IllegalStateException("No hay existencias de: " + r.getNombre());
        }
    }

    //Colas


    public IngresoTaller ingresar(Reparacion reparacion, IngresoTaller.Tipo tipo, int nivelDanio) {
        String placa = reparacion.getAutomovil().getPlaca();
        if (placaEnCola(placa) || placaAtendida(placa))
            throw new IllegalStateException("El vehiculo " + placa + " ya esta en el taller.");

        if (tipo == IngresoTaller.Tipo.ACCIDENTE && (nivelDanio < DANIO_MIN || nivelDanio > DANIO_MAX))
            throw new IllegalArgumentException("El nivel de dano debe estar entre " + DANIO_MIN + " y " + DANIO_MAX);
        if (tipo == IngresoTaller.Tipo.MANTENIMIENTO)
            nivelDanio = 0;

        contadorTurnos++;
        IngresoTaller ingreso = new IngresoTaller(reparacion, tipo, nivelDanio, contadorTurnos);

        if (tipo == IngresoTaller.Tipo.MANTENIMIENTO) {
            colaMantenimiento.offer(ingreso);
            PerformanceReporter.medirPesoObjeto(colaMantenimiento, "Cola de mantenimiento");
        } else {
            colaAccidentes.offer(ingreso);
            PerformanceReporter.medirPesoObjeto(colaAccidentes, "Cola de prioridad de accidentes");
        }

        return ingreso;
    }

    private boolean placaEnCola(String placa) {
        for (IngresoTaller ingreso : colaMantenimiento)
            if (ingreso.getReparacion().getAutomovil().getPlaca().equals(placa)) return true;
        for (IngresoTaller ingreso : colaAccidentes)
            if (ingreso.getReparacion().getAutomovil().getPlaca().equals(placa)) return true;
        return false;
    }

    private boolean placaAtendida(String placa) {
        for (IngresoTaller ingreso : colaAtendidos)
            if (ingreso.getReparacion().getAutomovil().getPlaca().equals(placa)) return true;
        return false;
    }


    public IngresoTaller atenderSiguiente(IngresoTaller.Tipo tipo) {
        IngresoTaller siguiente = (tipo == IngresoTaller.Tipo.MANTENIMIENTO)
                ? colaMantenimiento.poll()
                : colaAccidentes.poll();

        if (siguiente == null)
            throw new IllegalStateException("No hay vehiculos en espera en esa cola.");

        siguiente.setAtendido(true);
        colaAtendidos.offer(siguiente);
        return siguiente;
    }

    public void registrarSalida(IngresoTaller ingreso) {
        if (ingreso.isAtendido()) {
            colaAtendidos.remove(ingreso);
            return;
        }
        boolean retirado = colaMantenimiento.remove(ingreso) || colaAccidentes.remove(ingreso);
        if (!retirado)
            throw new IllegalStateException("El vehiculo no se encuentra en ninguna cola del taller.");
    }

    //Consultas


    public List<IngresoTaller> ordenMantenimiento() {
        List<IngresoTaller> enOrden = new ArrayList<>();
        for (IngresoTaller ingreso : colaMantenimiento) enOrden.add(ingreso);
        return enOrden;
    }

    public List<IngresoTaller> ordenAccidentes() {
        PriorityQueue<IngresoTaller> copia = new PriorityQueue<>(colaAccidentes);
        List<IngresoTaller> enOrden = new ArrayList<>();
        while (!copia.isEmpty())
            enOrden.add(copia.poll());
        return enOrden;
    }
}
