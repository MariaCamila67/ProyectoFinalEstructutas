package org.example.api;

import java.util.List;

public final class ApiDtos {
    private ApiDtos() { }

    public record LoginRequest(String nombre, String contrasena) { }

    public record MechanicResponse(String id, String nombre, String especialidad) { }

    public record ClientRequest(String id, String nombre, String telefono, long ingresos, boolean pedirCredito) { }

    public record ClientSearch(String id) { }

    public record ClientResponse(String id, String nombre, String telefono, boolean credito, long saldo) { }

    public record CarRequest(String placa, String marca, String modelo, int anio, String tipo, String cilindraje, int autonomia) { }

    public record CarResponse(String placa, String marca, String modelo, int anio, String tipo, String clienteId, String cilindraje, int autonomia) { }

    public record PartResponse(String nombre, String proveedor, int cantidad) { }

    public record RepairRequest(String tipo, int dano, long valor, List<String> repuestos, boolean credito) { }

    public record RepairResponse(long turno, String placa, String marca, String modelo, String cliente, String clienteId, String servicio, String tipoAuto, String cilindraje, int autonomia, int dano, long valor, String estado, boolean credito, boolean pagada, boolean puestaAPunto, String resumenMantenimiento) { }

    public record PaymentRequest(String destino, String medio, long abono, long recibido, int cuotas) { }

    public record PaymentResponse(long monto, String medio, long cambio, int cuotas) { }

    public record InvoiceResponse(String id, String placa, String cliente, long valor, String estado, String fecha) { }

    public record QueueResponse(List<RepairResponse> mantenimiento, List<RepairResponse> accidentes) { }

    public record ErrorResponse(String error) { }

    public record SessionResponse(MechanicResponse mecanico, ClientResponse cliente, CarResponse automovil, RepairResponse reparacion) { }
}
