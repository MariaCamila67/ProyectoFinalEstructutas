package org.example.api;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.example.aspectos.ClienteValidador;
import org.example.aspectos.MecanicoValidador;
import org.example.api.ApiDtos.*;
import org.example.clases.facturas.Factura;
import org.example.clases.personas.Cliente;
import org.example.clases.personas.Mecanico;
import org.example.clases.personas.ReglasNegocioPersona;
import org.example.clases.reparaciones.IngresoTaller;
import org.example.clases.reparaciones.Reparacion;
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
import org.example.Util.PerformanceReporter;
import org.example.interfaces.IMedio;
import org.example.interfaces.IPago;
import org.example.repositorios.FacturaRepositorio;
import org.example.repositorios.MecanicoRepositorio;
import org.example.servicios.ClienteServicio;
import org.example.servicios.ServicioRepuesto;
import org.example.servicios.TallerServicio;
import org.springframework.stereotype.Service;

@Service
public class TallerApiService {
    private final ServicioRepuesto servicioRepuesto = new ServicioRepuesto(Paths.get("data", "Repuestos.txt").toString());
    private final TallerServicio taller = new TallerServicio(servicioRepuesto);
    private final MecanicoValidador mecanicoValidador = new MecanicoValidador(new MecanicoRepositorio());
    private final ClienteServicio clienteServicio = new ClienteServicio();
    private final ClienteValidador clienteValidador = new ClienteValidador();
    private final FacturaRepositorio facturaRepositorio = new FacturaRepositorio();
    private List<Repuesto> repuestosCatalogo = new ArrayList<>();

    private Mecanico mecanicoActivo;
    private Cliente clienteActivo;
    private Automovil automovilActivo;
    private Reparacion reparacionActiva;
    private IngresoTaller ingresoActivo;
    private Venta ventaActiva;
    private Factura facturaActiva;
    private long ultimoPago;
    private long ultimoCambio;
    private String resumenMantenimiento = "";

    public synchronized MechanicResponse autenticar(LoginRequest request) {
        mecanicoActivo = mecanicoValidador.autenticar(request.nombre(), request.contrasena());
        if (mecanicoActivo == null) throw new IllegalArgumentException("Credenciales incorrectas.");
        return new MechanicResponse(mecanicoActivo.getId(), mecanicoActivo.getNombre(), mecanicoActivo.getEspecialidad().name());
    }

    public synchronized ClientResponse registrarCliente(ClientRequest request) {
        requireMecanico();
        Cliente cliente = new Cliente(request.id(), request.nombre(), request.telefono(), 0, false, request.ingresos());
        if (request.pedirCredito()) cliente.asignarCredito();
        String error = clienteValidador.validar(cliente);
        if (error != null) throw new IllegalArgumentException(error);
        if (cliente.isCredito()) {
            String resultado = clienteServicio.guardar(cliente);
            if (resultado.startsWith("Error")) throw new IllegalStateException(resultado);
        }
        clienteActivo = cliente;
        automovilActivo = null;
        reparacionActiva = null;
        ingresoActivo = null;
        return clienteResponse(cliente);
    }

    public synchronized ClientResponse buscarCliente(ClientSearch request) {
        requireMecanico();
        Cliente cliente = clienteServicio.buscarPorId(request.id());
        if (cliente == null) throw new IllegalArgumentException("Cliente no encontrado.");
        clienteActivo = cliente;
        automovilActivo = null;
        reparacionActiva = null;
        ingresoActivo = null;
        return clienteResponse(cliente);
    }

    public synchronized CarResponse registrarAutomovil(CarRequest request) {
        requireMecanico();
        requireCliente();
        String tipo = request.tipo().trim().toUpperCase();
        automovilActivo = switch (tipo) {
            case "GASOLINA" -> new Gasolina(request.placa(), request.marca(), request.modelo(), request.anio(), clienteActivo,
                    ReglasNegocioAutomovil.Cilindraje.valueOf(request.cilindraje()));
            case "ELECTRICO", "ELÉCTRICO" -> new Electrico(request.placa(), request.marca(), request.modelo(), request.anio(), clienteActivo, request.autonomia());
            case "HIBRIDO", "HÍBRIDO" -> new Hibrido(request.placa(), request.marca(), request.modelo(), request.anio(), clienteActivo);
            default -> throw new IllegalArgumentException("Tipo de automóvil inválido.");
        };
        reparacionActiva = null;
        ingresoActivo = null;
        return carResponse(automovilActivo);
    }

    public synchronized List<PartResponse> cargarRepuestos() {
        taller.cargarInventario();
        repuestosCatalogo = servicioRepuesto.cargarRepuestos();
        return inventarioActual();
    }

    public synchronized List<PartResponse> consultarInventario() {
        return inventarioActual();
    }

    private List<PartResponse> inventarioActual() {
        List<PartResponse> resultado = new ArrayList<>();
        List<String> vistos = new ArrayList<>();
        for (Repuesto repuesto : repuestosCatalogo) {
            String nombre = repuesto.getNombre().trim();
            if (!vistos.contains(nombre.toLowerCase())) {
                vistos.add(nombre.toLowerCase());
                resultado.add(new PartResponse(nombre, repuesto.getProveedor(), taller.cantidadRepuesto(nombre)));
            }
        }
        return resultado;
    }

    public synchronized RepairResponse crearReparacion(RepairRequest request) {
        requireMecanico();
        requireCliente();
        if (automovilActivo == null) throw new IllegalStateException("Registra un automóvil primero.");
        if (taller.inventarioVacio()) throw new IllegalStateException("Carga los repuestos primero.");
        if (ingresoActivo != null) throw new IllegalStateException("Finaliza la reparación activa antes de crear otra.");
        IngresoTaller.Tipo tipo = IngresoTaller.Tipo.valueOf(request.tipo().toUpperCase());
        if (tipo == IngresoTaller.Tipo.ACCIDENTE
                && (request.dano() < TallerServicio.DANIO_MIN || request.dano() > TallerServicio.DANIO_MAX))
            throw new IllegalArgumentException("El nivel de daño debe estar entre 1 y 5.");

        List<Repuesto> repuestos = taller.seleccionarRepuestos(request.repuestos());
        reparacionActiva = new Reparacion(automovilActivo, LocalDateTime.now(), repuestos, request.valor(), List.of(mecanicoActivo));
        clienteActivo.asignarReparacion(reparacionActiva);
        if (request.credito()) {
            if (!clienteActivo.isCredito()) throw new IllegalArgumentException("El cliente no tiene crédito aprobado.");
            long saldoAnterior = clienteActivo.getSaldoPendiente();
            clienteActivo.gestionarCredito("SI");
            String error = clienteValidador.validar(clienteActivo);
            if (error != null) { clienteActivo.setSaldoPendiente(saldoAnterior); throw new IllegalArgumentException(error); }
            String guardado = clienteServicio.actualizarSaldoPendiente(clienteActivo.getId(), clienteActivo.getSaldoPendiente());
            if (guardado.startsWith("Error")) {
                clienteActivo.setSaldoPendiente(saldoAnterior);
                throw new IllegalStateException(guardado);
            }
        } else clienteActivo.gestionarCredito("NO");

        ingresoActivo = taller.ingresar(reparacionActiva, tipo, request.dano());
        taller.consumirRepuestos(repuestos);
        ingresoActivo.setCredito(request.credito());
        resumenMantenimiento = "";
        ventaActiva = null;
        facturaActiva = null;
        return repairResponse(ingresoActivo);
    }

    public synchronized String ejecutarPuestaAPunto() {
        requireRepair();
        String resultado;
        if (automovilActivo instanceof Gasolina) resultado = reparacionActiva.ejecutarPuestaAPunto(new MantenimientoGasolina());
        else if (automovilActivo instanceof Electrico) resultado = reparacionActiva.ejecutarPuestaAPunto(new MantenimientoElectrico());
        else if (automovilActivo instanceof Hibrido) resultado = reparacionActiva.ejecutarPuestaAPunto(new MantenimientoHibrido());
        else throw new IllegalStateException("Tipo de vehículo no reconocido.");
        resumenMantenimiento = resultado;
        return resultado;
    }

    public synchronized PaymentResponse procesarPago(PaymentRequest request) {
        if (clienteActivo == null) throw new IllegalStateException("Selecciona un cliente primero.");
        if (automovilActivo == null) throw new IllegalStateException("Registra un automóvil primero.");
        IPago pago;
        long monto;
        if (request.destino().equalsIgnoreCase("actual")) {
            requireRepair();
            if (ingresoActivo != null && ingresoActivo.isCredito()) throw new IllegalStateException("La reparación está a crédito; genera la factura en F8.");
            pago = new PagarReparacion(reparacionActiva);
            monto = reparacionActiva.getValorReparacion();
        } else if (request.destino().equalsIgnoreCase("saldo")) {
            monto = request.abono();
            if (monto <= 0 || monto > clienteActivo.getSaldoPendiente()) throw new IllegalArgumentException("El abono no es válido.");
            pago = new PagarSaldoPendiente(clienteActivo);
        } else throw new IllegalArgumentException("El tipo de pago no es válido.");

        IMedio medio;
        ultimoCambio = 0;
        if (request.medio().equalsIgnoreCase("efectivo")) {
            if (request.recibido() < monto) throw new IllegalArgumentException("El monto recibido es insuficiente.");
            Efectivo efectivo = new Efectivo(request.recibido());
            ultimoCambio = efectivo.calcularCambio(monto);
            medio = efectivo;
        } else if (request.medio().equalsIgnoreCase("tarjeta")) medio = new Tarjeta(request.cuotas());
        else throw new IllegalArgumentException("Medio de pago inválido.");

        long saldoAnterior = clienteActivo.getSaldoPendiente();
        ventaActiva = new Venta(pago, medio, monto);
        ventaActiva.ejecutarVenta();
        if (request.destino().equalsIgnoreCase("saldo")) {
            String guardado = clienteServicio.actualizarSaldoPendiente(clienteActivo.getId(), clienteActivo.getSaldoPendiente());
            if (guardado.startsWith("Error")) {
                clienteActivo.setSaldoPendiente(saldoAnterior);
                throw new IllegalStateException(guardado);
            }
        }
        ultimoPago = monto;
        if (ingresoActivo != null && request.destino().equalsIgnoreCase("actual")) ingresoActivo.setPagada(true);
        facturaActiva = new Factura(ventaActiva, automovilActivo, clienteActivo);
        return new PaymentResponse(monto, request.medio(), ultimoCambio, request.cuotas());
    }

    public synchronized InvoiceResponse generarFactura() {
        if (automovilActivo == null || clienteActivo == null)
            throw new IllegalStateException("No hay un automóvil y cliente activos para facturar.");
        if (ingresoActivo != null && ingresoActivo.isCredito() && facturaActiva == null) {
            if (reparacionActiva == null) throw new IllegalStateException("No hay reparación activa para facturar.");
            ventaActiva = new Venta(new PagarReparacion(reparacionActiva), monto -> { }, reparacionActiva.getValorReparacion());
            facturaActiva = new Factura(ventaActiva, automovilActivo, clienteActivo);
            facturaActiva.setEstado("Credito");
        }
        if (facturaActiva == null) throw new IllegalStateException("Procesa el pago antes de generar la factura.");
        facturaRepositorio.guardarFactura(facturaActiva);
        if (ingresoActivo != null) taller.registrarSalida(ingresoActivo);
        InvoiceResponse respuesta = invoiceResponse(facturaActiva);
        clienteActivo = null;
        automovilActivo = null;
        reparacionActiva = null;
        ingresoActivo = null;
        ventaActiva = null;
        facturaActiva = null;
        PerformanceReporter.reportarMemoriaSistema();
        return respuesta;
    }

    public synchronized QueueResponse consultarColas() {
        List<RepairResponse> mantenimiento = new ArrayList<>();
        for (IngresoTaller ingreso : taller.ordenMantenimiento()) mantenimiento.add(repairResponse(ingreso));
        List<RepairResponse> accidentes = new ArrayList<>();
        for (IngresoTaller ingreso : taller.ordenAccidentes()) accidentes.add(repairResponse(ingreso));
        return new QueueResponse(mantenimiento, accidentes);
    }

    public synchronized SessionResponse consultarSesion() {
        return new SessionResponse(
                mecanicoActivo == null ? null : new MechanicResponse(mecanicoActivo.getId(), mecanicoActivo.getNombre(), mecanicoActivo.getEspecialidad().name()),
                clienteActivo == null ? null : clienteResponse(clienteActivo),
                automovilActivo == null ? null : carResponse(automovilActivo),
                ingresoActivo == null ? null : repairResponse(ingresoActivo));
    }

    public synchronized RepairResponse atenderSiguiente(String tipo) {
        IngresoTaller.Tipo servicio = IngresoTaller.Tipo.valueOf(tipo.toUpperCase());
        ingresoActivo = taller.atenderSiguiente(servicio);
        reparacionActiva = ingresoActivo.getReparacion();
        automovilActivo = reparacionActiva.getAutomovil();
        clienteActivo = automovilActivo.getCliente();
        clienteActivo.asignarReparacion(reparacionActiva);
        facturaActiva = null;
        ventaActiva = null;
        return repairResponse(ingresoActivo);
    }

    public synchronized String consultarEstadoFactura(String placa) { return facturaRepositorio.consultarEstadoFactura(placa); }
    public synchronized List<String> buscarFacturas(String placa) { return facturaRepositorio.buscarPorPlaca(placa); }
    public synchronized List<String> facturasPagadas() { return facturaRepositorio.consultarFacturasPagadas(); }

    private void requireMecanico() { if (mecanicoActivo == null) throw new IllegalStateException("Autentica al mecánico primero."); }
    private void requireCliente() { if (clienteActivo == null) throw new IllegalStateException("Registra o busca un cliente primero."); }
    private void requireRepair() { if (reparacionActiva == null) throw new IllegalStateException("Crea o atiende una reparación primero."); }
    private ClientResponse clienteResponse(Cliente c) { return new ClientResponse(c.getId(), c.getNombre(), c.getTelefono(), c.isCredito(), c.getSaldoPendiente()); }
    private CarResponse carResponse(Automovil a) {
        String cilindraje = a instanceof Gasolina gasolina ? gasolina.getCilindraje().name() : "";
        int autonomia = a instanceof Electrico electrico ? electrico.getAutonomia() : 0;
        return new CarResponse(a.getPlaca(), a.getMarca(), a.getModelo(), a.getAnio(), a.getClass().getSimpleName(), a.getCliente().getId(), cilindraje, autonomia);
    }
    private RepairResponse repairResponse(IngresoTaller i) {
        Reparacion r = i.getReparacion();
        Automovil a = r.getAutomovil();
        Cliente c = a.getCliente();
        String tipoAuto = a.getClass().getSimpleName();
        String cilindraje = a instanceof Gasolina gasolina ? gasolina.getCilindraje().name() : "";
        int autonomia = a instanceof Electrico electrico ? electrico.getAutonomia() : 0;
        String resumen = reparacionActiva == r ? resumenMantenimiento : "";
        return new RepairResponse(i.getTurno(), a.getPlaca(), a.getMarca(), a.getModelo(), c.getNombre(), c.getId(),
                i.getTipo().name(), tipoAuto, cilindraje, autonomia, i.getNivelDanio(), r.getValorReparacion(),
                i.isAtendido() ? "atendiendo" : "espera", i.isCredito(), i.isPagada(), !resumen.isBlank(), resumen);
    }
    private InvoiceResponse invoiceResponse(Factura f) { return new InvoiceResponse(f.getId(), f.getPlaca(), f.getClienteNombre(), f.getMonto(), f.getEstado(), new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(f.getFecha())); }
}
