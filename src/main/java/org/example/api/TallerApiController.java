package org.example.api;

import java.util.List;

import org.example.api.ApiDtos.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class TallerApiController {
    private final TallerApiService servicio;

    public TallerApiController(TallerApiService servicio) { this.servicio = servicio; }

    @PostMapping("/auth")
    public MechanicResponse autenticar(@RequestBody LoginRequest request) { return servicio.autenticar(request); }

    @PostMapping("/clients")
    public ClientResponse registrarCliente(@RequestBody ClientRequest request) { return servicio.registrarCliente(request); }

    @GetMapping("/clients/{id}")
    public ClientResponse buscarCliente(@PathVariable String id) { return servicio.buscarCliente(new ClientSearch(id)); }

    @PostMapping("/cars")
    public CarResponse registrarAutomovil(@RequestBody CarRequest request) { return servicio.registrarAutomovil(request); }

    @PostMapping("/inventory/load")
    public List<PartResponse> cargarRepuestos() { return servicio.cargarRepuestos(); }

    @GetMapping("/inventory")
    public List<PartResponse> consultarInventario() { return servicio.consultarInventario(); }

    @PostMapping("/repairs")
    public RepairResponse crearReparacion(@RequestBody RepairRequest request) { return servicio.crearReparacion(request); }

    @PostMapping("/maintenance")
    public String ejecutarPuestaAPunto() { return servicio.ejecutarPuestaAPunto(); }

    @PostMapping("/payments")
    public PaymentResponse procesarPago(@RequestBody PaymentRequest request) { return servicio.procesarPago(request); }

    @PostMapping("/invoices")
    public InvoiceResponse generarFactura() { return servicio.generarFactura(); }

    @GetMapping("/queues")
    public QueueResponse consultarColas() { return servicio.consultarColas(); }

    @GetMapping("/session")
    public SessionResponse consultarSesion() { return servicio.consultarSesion(); }

    @PostMapping("/queues/attend/{tipo}")
    public RepairResponse atenderSiguiente(@PathVariable String tipo) { return servicio.atenderSiguiente(tipo); }

    @GetMapping("/invoices/status/{placa}")
    public String consultarEstadoFactura(@PathVariable String placa) { return servicio.consultarEstadoFactura(placa); }

    @GetMapping("/invoices/plate/{placa}")
    public List<String> buscarFacturas(@PathVariable String placa) { return servicio.buscarFacturas(placa); }

    @GetMapping("/invoices/paid")
    public List<String> facturasPagadas() { return servicio.facturasPagadas(); }
}
