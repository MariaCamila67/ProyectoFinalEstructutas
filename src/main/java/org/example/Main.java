package org.example;

import org.example.aspectos.ClienteValidador;
import org.example.aspectos.MecanicoValidador;
import org.example.clases.facturas.Factura;
import org.example.clases.personas.Cliente;
import org.example.clases.personas.Mecanico;
import org.example.clases.reparaciones.Reparacion;
import org.example.clases.reparaciones.mantenimientos.Mantenimiento;
import org.example.clases.reparaciones.mantenimientos.MantenimientoElectrico;
import org.example.clases.reparaciones.mantenimientos.MantenimientoGasolina;
import org.example.clases.reparaciones.mantenimientos.MantenimientoHibrido;
import org.example.clases.repuestos.Repuesto;
import org.example.clases.vehiculo.Automovil;
import org.example.clases.vehiculo.Electrico;
import org.example.clases.vehiculo.Gasolina;
import org.example.clases.vehiculo.Hibrido;
import org.example.clases.vehiculo.ReglasNegocioAutomovil;
import org.example.clases.ventas.Venta;
import org.example.clases.ventas.medios.Efectivo;
import org.example.clases.ventas.medios.Tarjeta;
import org.example.clases.ventas.pagos.PagarReparacion;
import org.example.clases.ventas.pagos.PagarSaldoPendiente;
import org.example.interfaces.IMedio;
import org.example.interfaces.IPago;
import org.example.servicios.ClienteServicio;
import org.example.repositorios.FacturaRepositorio;
import org.example.repositorios.MecanicoRepositorio;
import org.example.servicios.ServicioRepuesto;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Scanner;
import java.util.stream.Stream;

public class Main {

    // ================================================================
    // ESTRUCTURAS DE DATOS DEL MVP
    // ================================================================

    /** Niveles de daño para el servicio de accidentes (mayor numero = mas grave = mas prioridad). */
    static final int DANIO_MIN = 1;
    static final int DANIO_MAX = 5;

    /** BOLSA (bag): coleccion sin orden que admite repetidos. Guarda el inventario de repuestos. */
    static class BolsaRepuestos {

        private final Map<String, Integer> cantidades = new LinkedHashMap<>();
        private final Map<String, Repuesto> detalle = new LinkedHashMap<>();

        private String clave(String nombre) {
            return nombre.trim().toLowerCase();
        }

        void agregar(Repuesto repuesto) {
            String k = clave(repuesto.getNombre());
            cantidades.merge(k, 1, Integer::sum);
            detalle.putIfAbsent(k, repuesto);
        }

        int cantidad(String nombre) {
            return cantidades.getOrDefault(clave(nombre), 0);
        }

        Repuesto obtener(String nombre) {
            return detalle.get(clave(nombre));
        }

        void retirar(String nombre) {
            String k = clave(nombre);
            int actual = cantidades.getOrDefault(k, 0);
            if (actual == 0)
                throw new IllegalStateException("No hay existencias de: " + nombre);
            if (actual == 1) {
                cantidades.remove(k);
                detalle.remove(k);
            } else {
                cantidades.put(k, actual - 1);
            }
        }

        void vaciar() {
            cantidades.clear();
            detalle.clear();
        }

        boolean estaVacia() {
            return cantidades.isEmpty();
        }

        void mostrar() {
            for (Map.Entry<String, Integer> e : cantidades.entrySet()) {
                Repuesto r = detalle.get(e.getKey());
                System.out.println("  - " + r.getNombre() + " | " + r.getProveedor()
                        + " | " + r.getFechaCompra().toLocalDate() + " | Cantidad: " + e.getValue());
            }
        }
    }

    /** Registro de un vehiculo dentro del taller. Se guarda en la cola FIFO o en la cola de prioridad. */
    static class IngresoTaller {
        final Reparacion reparacion;
        final String servicio;      // MANTENIMIENTO o ACCIDENTE
        final int nivelDanio;       // solo aplica a ACCIDENTE
        final long turno;           // orden de llegada (desempata la prioridad)
        boolean aCredito;
        boolean pagada;

        IngresoTaller(Reparacion reparacion, String servicio, int nivelDanio, long turno) {
            this.reparacion = reparacion;
            this.servicio = servicio;
            this.nivelDanio = nivelDanio;
            this.turno = turno;
        }

        String descripcion() {
            Automovil a = reparacion.getAutomovil();
            String base = "Turno #" + turno + " | Placa: " + a.getPlaca() + " | " + a.getMarca() + " " + a.getModelo()
                    + " | Cliente: " + a.getCliente().getNombre();
            return servicio.equals("ACCIDENTE") ? base + " | Nivel de dano: " + nivelDanio : base;
        }
    }

    // ================================================================
    // UTILIDADES DE LECTURA (evitan que un dato mal digitado cierre el programa)
    // ================================================================

    private static final Scanner entrada = new Scanner(System.in);

    private static String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return entrada.nextLine();
    }

    private static long leerLong(String mensaje) {
        try {
            return Long.parseLong(leerTexto(mensaje).trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Debe ingresar un valor numerico valido");
        }
    }

    private static int leerEntero(String mensaje) {
        return (int) leerLong(mensaje);
    }

    private static String persistirSaldo(ClienteServicio servicio, Cliente cliente) {
        if (servicio == null)
            return "Base de datos no disponible: el saldo solo se actualizo en memoria";
        return servicio.actualizarSaldoPendiente(cliente.getId(), cliente.getSaldoPendiente());
    }

    private static void imprimirCola(String titulo, Collection<IngresoTaller> enOrden) {
        System.out.println("\n--- " + titulo + " ---");
        if (enOrden.isEmpty()) {
            System.out.println("No hay vehiculos en espera");
            return;
        }
        int posicion = 1;
        for (IngresoTaller i : enOrden)
            System.out.println(posicion++ + ". " + i.descripcion());
    }

    // ================================================================
    // PROGRAMA PRINCIPAL
    // ================================================================

    public static void main(String[] args) {
        if (args.length > 0 && "--consola".equals(args[0])) {
            ejecutarConsola();
            return;
        }
        org.example.api.TallerApiApplication.main(args);
    }

    // El menú anterior se conserva como modo alternativo: Main --consola.
    private static void ejecutarConsola() {

        //RUTAS DE ARCHIVOS
        String rutaRepuestos = Paths.get("data", "Repuestos.txt").toString();

        //SERVICIOS Y ASPECTOS
        MecanicoRepositorio mecanicoRepositorio = new MecanicoRepositorio();
        ServicioRepuesto  repuestoServicio  = new ServicioRepuesto(rutaRepuestos);
        MecanicoValidador mecanicoValidador = new MecanicoValidador(mecanicoRepositorio);
        ClienteValidador  clienteValidador  = new ClienteValidador();
        FacturaRepositorio facturaRepositorio = new FacturaRepositorio();

        // La conexion a MongoDB puede fallar: el taller sigue funcionando solo con memoria
        ClienteServicio clienteServicio = null;
        try {
            clienteServicio = new ClienteServicio();
        } catch (Exception ex) {
            System.out.println("Advertencia: sin conexion a la base de datos (" + ex.getMessage() + ")");
            System.out.println("Los clientes con credito solo se guardaran en memoria.");
        }

        //ESTADO DEL PROGRAMA
        Mecanico      mecanicoActivo   = null;
        Cliente       clienteActivo    = null;
        Automovil     autoActivo       = null;
        Reparacion    reparacionActiva = null;
        IngresoTaller ingresoActivo    = null;
        Venta         ventaActiva      = null;
        Factura       facturaActiva    = null;

        Map<String, Cliente> clientesTaller = new HashMap<>();

        //ESTRUCTURAS DE DATOS
        BolsaRepuestos bolsaRepuestos = new BolsaRepuestos();                    // F4: inventario (bag)
        Queue<IngresoTaller> colaMantenimiento = new ArrayDeque<>();             // F5: orden de llegada (FIFO)
        PriorityQueue<IngresoTaller> colaAccidentes = new PriorityQueue<>(       // F10: prioridad por nivel de dano
                Comparator.comparingInt((IngresoTaller i) -> i.nivelDanio).reversed()
                        .thenComparingLong(i -> i.turno));
        long contadorTurnos = 0;

        boolean salir = false;
        while (!salir) {
            System.out.println("\n========================================");
            System.out.println("       TALLER DE AUTOMOVILES");
            System.out.println("========================================");
            System.out.println("1.  Autenticar mecanico");
            System.out.println("2.  Registrar cliente");
            System.out.println("3.  Registrar automovil");
            System.out.println("4.  Cargar repuestos desde archivo");
            System.out.println("5.  Crear reparacion");
            System.out.println("6.  Ejecutar puesta a punto");
            System.out.println("7.  Procesar pago");
            System.out.println("8.  Generar factura y autorizar salida");
            System.out.println("9.  Consultar facturas");
            System.out.println("10. Consultar orden de accidentalidad");
            System.out.println("11. Atender siguiente vehiculo");
            System.out.println("12. Salir");

            if (!entrada.hasNextLine()) break;
            String opcion = leerTexto("\nSeleccione una opcion: ");

            switch (opcion.trim()) {

                //1. AUTENTICAR MECANICO (F1)
                case "1":
                    try {
                        System.out.println("\n=== AUTENTICACION MECANICO ===");
                        String nombreMecanico = leerTexto("Nombre: ");
                        String contrasenaMecanico = leerTexto("Contrasena: ");

                        mecanicoActivo = mecanicoValidador.autenticar(nombreMecanico, contrasenaMecanico);
                        if (mecanicoActivo == null)
                            throw new Exception("Credenciales incorrectas. Intente nuevamente.");

                        System.out.println("Mecanico autenticado: " + mecanicoActivo.getNombre()
                                + " - Especialidad: " + mecanicoActivo.getEspecialidad());
                    } catch (Exception ex) { System.out.println("Error: " + ex.getMessage()); }
                    break;

                //2. REGISTRAR CLIENTE (F2)
                case "2":
                    try {
                        if (mecanicoActivo == null) throw new Exception("Debe autenticar un mecanico primero.");

                        System.out.println("\n=== GESTIONAR CLIENTE ===");
                        System.out.println("1. Registrar cliente nuevo");
                        System.out.println("2. Buscar cliente existente");
                        String opCliente = leerTexto("Opcion: ").trim();

                        if (opCliente.equals("1")) {
                            String idCliente = leerTexto("ID: ").trim();
                            if (clientesTaller.containsKey(idCliente))
                                throw new Exception("Ya existe un cliente con ese id. Use la opcion de busqueda.");
                            String nombreCliente = leerTexto("Nombre: ");
                            String telefonoCliente = leerTexto("Telefono: ").trim();
                            long ingresos = leerLong("Ingresos mensuales: ");

                            Cliente nuevoCliente = new Cliente(idCliente, nombreCliente, telefonoCliente, 0, false, ingresos);

                            String solicitaCredito = leerTexto("Desea solicitar credito? (SI/NO): ").trim().toUpperCase();
                            if (solicitaCredito.equals("SI"))
                                System.out.println(nuevoCliente.asignarCredito());

                            String resultadoValidacion = clienteValidador.validar(nuevoCliente);
                            if (resultadoValidacion != null)
                                throw new Exception("Cliente invalido: " + resultadoValidacion);

                            if (nuevoCliente.isCredito()) {
                                if (clienteServicio != null)
                                    System.out.println(clienteServicio.guardar(nuevoCliente));
                                else
                                    System.out.println("Base de datos no disponible: cliente con credito registrado solo en memoria");
                            } else {
                                System.out.println("Cliente registrado en memoria: " + nuevoCliente.getNombre());
                            }

                            clientesTaller.put(nuevoCliente.getId(), nuevoCliente);
                            clienteActivo = nuevoCliente;
                        } else if (opCliente.equals("2")) {
                            String idBuscado = leerTexto("Id del cliente: ").trim();

                            Cliente encontrado = clientesTaller.get(idBuscado);
                            if (encontrado == null && clienteServicio != null) {
                                encontrado = clienteServicio.buscarPorId(idBuscado);
                                if (encontrado != null) clientesTaller.put(encontrado.getId(), encontrado);
                            }
                            if (encontrado == null)
                                throw new Exception("Cliente no encontrado");

                            clienteActivo = encontrado;
                            System.out.println("Cliente encontrado: " + clienteActivo.getNombre()
                                    + " | Saldo: " + clienteActivo.getSaldoPendiente()
                                    + " | Credito: " + clienteActivo.isCredito());
                        } else {
                            throw new Exception("Opcion invalida");
                        }

                        // Un cliente nuevo en pantalla inicia un flujo nuevo
                        autoActivo = null; reparacionActiva = null; ingresoActivo = null;
                        ventaActiva = null; facturaActiva = null;
                    } catch (Exception ex) { System.out.println("Error: " + ex.getMessage()); }
                    break;

                //3. REGISTRAR AUTOMOVIL (F3)
                case "3":
                    try {
                        if (clienteActivo == null) throw new Exception("Debe registrar un cliente primero.");

                        System.out.println("\n=== REGISTRO DE AUTOMOVIL ===");
                        System.out.println("Tipo: 1. Gasolina  2. Electrico  3. Hibrido");
                        String tipoAuto = leerTexto("Seleccione tipo: ").trim();

                        String placa  = leerTexto("Placa (Ej: ABC123): ").trim().toUpperCase();
                        String marca  = leerTexto("Marca: ").trim();
                        String modelo = leerTexto("Modelo: ").trim();
                        int anio      = leerEntero("Anio: ");

                        Automovil nuevoAuto;
                        switch (tipoAuto) {
                            case "1":
                                System.out.println("Cilindraje: 1.C4  2.C6  3.C12  4.C16");
                                String cilOpc = leerTexto("Seleccione: ").trim();
                                ReglasNegocioAutomovil.Cilindraje cil;
                                switch (cilOpc) {
                                    case "1": cil = ReglasNegocioAutomovil.Cilindraje.C4;  break;
                                    case "2": cil = ReglasNegocioAutomovil.Cilindraje.C6;  break;
                                    case "3": cil = ReglasNegocioAutomovil.Cilindraje.C12; break;
                                    case "4": cil = ReglasNegocioAutomovil.Cilindraje.C16; break;
                                    default: throw new Exception("Cilindraje invalido");
                                }
                                nuevoAuto = new Gasolina(placa, marca, modelo, anio, clienteActivo, cil);
                                break;
                            case "2":
                                int autonomia = leerEntero("Autonomia de la bateria (0-100%): ");
                                nuevoAuto = new Electrico(placa, marca, modelo, anio, clienteActivo, autonomia);
                                break;
                            case "3":
                                nuevoAuto = new Hibrido(placa, marca, modelo, anio, clienteActivo);
                                break;
                            default:
                                throw new Exception("Tipo de automovil invalido");
                        }

                        autoActivo = nuevoAuto;
                        reparacionActiva = null; ingresoActivo = null; ventaActiva = null; facturaActiva = null;
                        System.out.println("Automovil registrado: " + autoActivo.getMarca() + " "
                                + autoActivo.getModelo() + " - Placa: " + autoActivo.getPlaca());
                        System.out.println("Para agregarlo a una cola, cree su reparacion en la opcion 5.");
                    } catch (Exception ex) { System.out.println("Error: " + ex.getMessage()); }
                    break;

                //4. CARGAR REPUESTOS (F4) -> llena la BOLSA
                case "4":
                    try {
                        System.out.println("\n=== CARGA DE REPUESTOS DESDE ARCHIVO ===");
                        List<Repuesto> cargados = repuestoServicio.cargarRepuestos();

                        bolsaRepuestos.vaciar();
                        cargados.forEach(bolsaRepuestos::agregar);

                        System.out.println("Repuestos cargados: " + cargados.size());
                        System.out.println("Inventario disponible (bolsa):");
                        bolsaRepuestos.mostrar();
                    } catch (Exception ex) { System.out.println("Error: " + ex.getMessage()); }
                    break;

                //5. CREAR REPARACION (F5) -> ingresa a la COLA o a la COLA DE PRIORIDAD
                case "5":
                    try {
                        if (mecanicoActivo == null)         throw new Exception("Debe autenticar un mecanico primero.");
                        if (clienteActivo == null)          throw new Exception("Debe registrar un cliente primero.");
                        if (autoActivo == null)             throw new Exception("Debe registrar un automovil primero.");
                        if (bolsaRepuestos.estaVacia())     throw new Exception("Debe cargar los repuestos primero.");

                        final String placaIngreso = autoActivo.getPlaca();
                        boolean yaIngresado = Stream.concat(colaMantenimiento.stream(), colaAccidentes.stream())
                                .anyMatch(i -> i.reparacion.getAutomovil().getPlaca().equals(placaIngreso));
                        if (yaIngresado) throw new Exception("El vehiculo " + placaIngreso + " ya esta en el taller.");

                        System.out.println("\n=== CREAR REPARACION ===");
                        System.out.println("Tipo de ingreso: 1. Mantenimiento  2. Colision (accidente)");
                        String tipoIngreso = leerTexto("Seleccione: ").trim();
                        if (!tipoIngreso.equals("1") && !tipoIngreso.equals("2"))
                            throw new Exception("Tipo de ingreso invalido");

                        int nivelDanio = 0;
                        if (tipoIngreso.equals("2")) {
                            nivelDanio = leerEntero("Nivel de dano (" + DANIO_MIN + " leve - " + DANIO_MAX + " critico): ");
                            if (nivelDanio < DANIO_MIN || nivelDanio > DANIO_MAX)
                                throw new Exception("El nivel de dano debe estar entre " + DANIO_MIN + " y " + DANIO_MAX);
                        }

                        long valorReparacion = leerLong("Valor de la reparacion: ");

                        System.out.println("Repuestos disponibles:");
                        bolsaRepuestos.mostrar();
                        String lineaRepuestos = leerTexto("Ingrese nombres de repuestos separados por coma (vacio = ninguno): ");

                        // Validar existencias en la bolsa antes de consumir nada
                        Map<String, Integer> requeridos = new LinkedHashMap<>();
                        if (!lineaRepuestos.isBlank())
                            for (String nombre : lineaRepuestos.split(","))
                                if (!nombre.isBlank()) requeridos.merge(nombre.trim().toLowerCase(), 1, Integer::sum);

                        List<String> porSolicitar = new ArrayList<>();
                        List<Repuesto> repuestosSeleccionados = new ArrayList<>();
                        for (Map.Entry<String, Integer> req : requeridos.entrySet()) {
                            int disponibles = bolsaRepuestos.cantidad(req.getKey());
                            if (disponibles < req.getValue()) {
                                porSolicitar.add(req.getKey() + " (faltan " + (req.getValue() - disponibles) + ")");
                            } else {
                                for (int i = 0; i < req.getValue(); i++)
                                    repuestosSeleccionados.add(bolsaRepuestos.obtener(req.getKey()));
                            }
                        }
                        if (!porSolicitar.isEmpty())
                            throw new Exception("Repuestos que deben solicitarse: " + String.join(", ", porSolicitar));

                        Reparacion nuevaReparacion = new Reparacion(autoActivo, LocalDateTime.now(),
                                repuestosSeleccionados, valorReparacion, new ArrayList<>(List.of(mecanicoActivo)));
                        clienteActivo.asignarReparacion(nuevaReparacion);

                        // Gestionar credito
                        boolean aCredito = false;
                        if (clienteActivo.isCredito()) {
                            String respuesta = leerTexto("El cliente desea dejar el valor a credito? (SI/NO): ").trim().toUpperCase();
                            if (!respuesta.equals("SI") && !respuesta.equals("NO"))
                                throw new Exception("Respuesta invalida, debe ser SI o NO");

                            long saldoPrevio = clienteActivo.getSaldoPendiente();
                            System.out.println(clienteActivo.gestionarCredito(respuesta));

                            if (respuesta.equals("SI")) {
                                String errorCredito = clienteValidador.validar(clienteActivo);
                                if (errorCredito != null) {
                                    clienteActivo.setSaldoPendiente(saldoPrevio);
                                    throw new Exception("No se puede dejar a credito: " + errorCredito);
                                }
                                aCredito = true;
                                System.out.println(persistirSaldo(clienteServicio, clienteActivo));
                            }
                        } else {
                            System.out.println(clienteActivo.gestionarCredito("NO"));
                        }

                        // Todo validado: se consumen los repuestos y el vehiculo entra a su cola
                        for (Repuesto r : repuestosSeleccionados) bolsaRepuestos.retirar(r.getNombre());

                        contadorTurnos++;
                        if (tipoIngreso.equals("1")) {
                            ingresoActivo = new IngresoTaller(nuevaReparacion, "MANTENIMIENTO", 0, contadorTurnos);
                            colaMantenimiento.offer(ingresoActivo);
                            System.out.println("Vehiculo ingresado a la cola de mantenimiento. Posicion: " + colaMantenimiento.size());
                        } else {
                            ingresoActivo = new IngresoTaller(nuevaReparacion, "ACCIDENTE", nivelDanio, contadorTurnos);
                            colaAccidentes.offer(ingresoActivo);
                            System.out.println("Vehiculo ingresado a la cola de accidentes con nivel de dano "
                                    + nivelDanio + ". Posicion en cola: " + colaAccidentes.size());
                        }
                        ingresoActivo.aCredito = aCredito;
                        reparacionActiva = nuevaReparacion;
                        ventaActiva = null;
                        facturaActiva = null;
                        System.out.println("Reparacion creada exitosamente.");
                    } catch (Exception ex) { System.out.println("Error: " + ex.getMessage()); }
                    break;

                //6. EJECUTAR PUESTA A PUNTO (F6)
                case "6":
                    try {
                        if (reparacionActiva == null) throw new Exception("Debe crear una reparacion primero.");

                        System.out.println("\n=== PUESTA A PUNTO ===");
                        Mantenimiento mantenimiento;
                        if (autoActivo instanceof Gasolina)       mantenimiento = new MantenimientoGasolina();
                        else if (autoActivo instanceof Electrico) mantenimiento = new MantenimientoElectrico();
                        else if (autoActivo instanceof Hibrido)   mantenimiento = new MantenimientoHibrido();
                        else throw new Exception("Tipo de vehiculo no reconocido");

                        System.out.println(reparacionActiva.ejecutarPuestaAPunto(mantenimiento));
                        System.out.println("Reparacion finalizada. Puede procesar el pago.");
                    } catch (Exception ex) { System.out.println("Error: " + ex.getMessage()); }
                    break;

                //7. PROCESAR PAGO (F7)
                case "7":
                    try {
                        if (clienteActivo == null) throw new Exception("Debe registrar o buscar un cliente primero.");
                        if (autoActivo == null)    throw new Exception("Debe registrar un automovil primero.");
                        if (reparacionActiva == null && clienteActivo.getSaldoPendiente() == 0)
                            throw new Exception("Debe crear una reparacion primero o el cliente debe tener saldo pendiente.");

                        System.out.println("\n=== PROCESAR PAGO ===");
                        System.out.println("1. Pagar reparacion actual");
                        System.out.println("2. Pagar saldo pendiente");
                        String opPago = leerTexto("Opcion: ").trim();

                        IPago pago;
                        long montoPagar;

                        if (opPago.equals("1")) {
                            if (reparacionActiva == null) throw new Exception("Debe crear una reparacion primero.");
                            if (ingresoActivo != null && ingresoActivo.aCredito)
                                throw new Exception("El cliente dejo la reparacion a credito, debe pagarla desde el saldo pendiente.");
                            if (ingresoActivo != null && ingresoActivo.pagada)
                                throw new Exception("La reparacion actual ya fue pagada.");
                            pago = new PagarReparacion(reparacionActiva);
                            montoPagar = reparacionActiva.getValorReparacion();
                        } else if (opPago.equals("2")) {
                            if (clienteActivo.getSaldoPendiente() == 0) throw new Exception("El cliente no tiene saldo pendiente.");
                            long montoIngresado;
                            do {
                                montoIngresado = leerLong("Saldo pendiente: " + clienteActivo.getSaldoPendiente() + " | Cuanto desea pagar? ");
                                if (montoIngresado > clienteActivo.getSaldoPendiente())
                                    System.out.println("Error: El monto no puede ser mayor al saldo pendiente (" + clienteActivo.getSaldoPendiente() + ").");
                                else if (montoIngresado <= 0)
                                    System.out.println("Error: El monto debe ser mayor a cero.");
                            } while (montoIngresado <= 0 || montoIngresado > clienteActivo.getSaldoPendiente());
                            montoPagar = montoIngresado;
                            pago = new PagarSaldoPendiente(clienteActivo);
                        } else {
                            throw new Exception("Opcion invalida");
                        }

                        System.out.println("Medio de pago: 1. Efectivo  2. Tarjeta");
                        String opMedio = leerTexto("Seleccione: ").trim();

                        IMedio medio;
                        long cambio = 0;
                        if (opMedio.equals("1")) {
                            long montoRecibido;
                            do {
                                montoRecibido = leerLong("Monto a pagar: " + montoPagar + " | Monto recibido: ");
                                if (montoRecibido < montoPagar)
                                    System.out.println("Error: El monto recibido (" + montoRecibido + ") es insuficiente. Debe ser al menos " + montoPagar + ".");
                            } while (montoRecibido < montoPagar);
                            Efectivo efectivo = new Efectivo(montoRecibido);
                            cambio = efectivo.calcularCambio(montoPagar);
                            medio = efectivo;
                        } else if (opMedio.equals("2")) {
                            int cuotas = leerEntero("Numero de cuotas: ");
                            medio = new Tarjeta(cuotas);
                            System.out.println("Valor por cuota: " + (montoPagar / cuotas));
                        } else {
                            throw new Exception("Medio de pago invalido");
                        }

                        Venta nuevaVenta = new Venta(pago, medio, montoPagar);
                        nuevaVenta.ejecutarVenta();
                        ventaActiva = nuevaVenta;

                        facturaActiva = new Factura(ventaActiva, autoActivo, clienteActivo);

                        if (opPago.equals("1") && ingresoActivo != null) ingresoActivo.pagada = true;
                        if (opPago.equals("2")) System.out.println(persistirSaldo(clienteServicio, clienteActivo));

                        if (cambio > 0) System.out.println("Cambio a devolver: " + cambio);
                        System.out.println("Venta procesada. Ya puede generar la factura.");
                    } catch (Exception ex) { System.out.println("Error: " + ex.getMessage()); }
                    break;

                //8. GENERAR FACTURA Y AUTORIZAR SALIDA (F8)
                case "8":
                    try {
                        // Reparacion dejada a credito: la factura queda pendiente, no hay pago que procesar
                        if (facturaActiva == null && ingresoActivo != null && ingresoActivo.aCredito) {
                            Venta ventaCredito = new Venta(new PagarReparacion(reparacionActiva), monto -> { },
                                    reparacionActiva.getValorReparacion());
                            facturaActiva = new Factura(ventaCredito, autoActivo, clienteActivo);
                            facturaActiva.setEstado("Credito");
                        }
                        if (facturaActiva == null) throw new Exception("Debe procesar el pago primero.");

                        System.out.println("\n=== FACTURA Y SALIDA ===");
                        System.out.println(facturaActiva.generarFactura());
                        System.out.println(facturaRepositorio.guardarFactura(facturaActiva));

                        // El vehiculo sale del taller: se retira de la cola donde estuviera
                        if (ingresoActivo != null) {
                            colaMantenimiento.remove(ingresoActivo);
                            colaAccidentes.remove(ingresoActivo);
                        }
                        System.out.println("Salida del vehiculo autorizada.");

                        clienteActivo    = null;
                        autoActivo       = null;
                        reparacionActiva = null;
                        ingresoActivo    = null;
                        ventaActiva      = null;
                        facturaActiva    = null;
                    } catch (Exception ex) { System.out.println("Error: " + ex.getMessage()); }
                    break;

                //9. CONSULTAR FACTURAS (F9)
                case "9":
                    try {
                        System.out.println("\n1. Consultar estado por placa");
                        System.out.println("2. Buscar facturas por placa");
                        System.out.println("3. Ver todas las facturas pagadas");
                        String opFactura = leerTexto("Opcion: ").trim();

                        switch (opFactura) {
                            case "1":
                                System.out.println(facturaRepositorio.consultarEstadoFactura(leerTexto("Placa: ").trim().toUpperCase()));
                                break;

                            case "2":
                                List<String> porPlaca = facturaRepositorio.buscarPorPlaca(leerTexto("Placa: ").trim().toUpperCase());
                                if (porPlaca.isEmpty())
                                    System.out.println("No se encontraron facturas");
                                else
                                    porPlaca.forEach(System.out::println);
                                break;

                            case "3":
                                List<String> pagadas = facturaRepositorio.consultarFacturasPagadas();
                                if (pagadas.isEmpty())
                                    System.out.println("No hay facturas pagadas");
                                else
                                    pagadas.forEach(System.out::println);
                                break;

                            default:
                                System.out.println("Opcion invalida");
                                break;
                        }
                    } catch (Exception ex) { System.out.println("Error: " + ex.getMessage()); }
                    break;

                //10. CONSULTAR ORDEN DE ACCIDENTALIDAD (F10)
                case "10":
                    try {
                        System.out.println("\n1. Cola de accidentes (por nivel de dano)");
                        System.out.println("2. Cola de mantenimiento (por orden de llegada)");
                        String opCola = leerTexto("Opcion: ").trim();

                        if (opCola.equals("1")) {
                            // Se copia la cola para ordenar sin sacar vehiculos de la original
                            PriorityQueue<IngresoTaller> copia = new PriorityQueue<>(colaAccidentes);
                            List<IngresoTaller> enOrden = new ArrayList<>();
                            while (!copia.isEmpty()) enOrden.add(copia.poll());
                            imprimirCola("ORDEN DE ATENCION - ACCIDENTES", enOrden);
                        } else if (opCola.equals("2")) {
                            imprimirCola("ORDEN DE ATENCION - MANTENIMIENTO", colaMantenimiento);
                        } else {
                            throw new Exception("Opcion invalida");
                        }
                    } catch (Exception ex) { System.out.println("Error: " + ex.getMessage()); }
                    break;

                //11. ATENDER SIGUIENTE VEHICULO (toma el primero de la cola elegida)
                case "11":
                    try {
                        if (mecanicoActivo == null) throw new Exception("Debe autenticar un mecanico primero.");

                        System.out.println("\n=== ATENDER SIGUIENTE VEHICULO ===");
                        System.out.println("1. Mantenimiento (orden de llegada)");
                        System.out.println("2. Accidente (mayor nivel de dano)");
                        String opAtender = leerTexto("Opcion: ").trim();

                        IngresoTaller siguiente;
                        if (opAtender.equals("1"))      siguiente = colaMantenimiento.peek();
                        else if (opAtender.equals("2")) siguiente = colaAccidentes.peek();
                        else throw new Exception("Opcion invalida");

                        if (siguiente == null) throw new Exception("No hay vehiculos en espera en esa cola.");

                        // peek: el vehiculo permanece en la cola hasta que se autorice su salida (opcion 8)
                        ingresoActivo    = siguiente;
                        reparacionActiva = siguiente.reparacion;
                        autoActivo       = siguiente.reparacion.getAutomovil();
                        clienteActivo    = autoActivo.getCliente();
                        clienteActivo.asignarReparacion(reparacionActiva);
                        ventaActiva      = null;
                        facturaActiva    = null;

                        System.out.println("Atendiendo: " + siguiente.descripcion());
                    } catch (Exception ex) { System.out.println("Error: " + ex.getMessage()); }
                    break;

                //12. SALIR
                case "12":
                    salir = true;
                    System.out.println("Cerrando el sistema. Hasta luego.");
                    break;

                default:
                    System.out.println("Opcion invalida. Intente nuevamente.");
                    break;
            }
        }
    }
}
