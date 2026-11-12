package org.example.clases.vehiculo;

import org.example.clases.personas.Cliente;

import java.util.regex.Pattern;

public class Automovil {

    private String placa;
    private String marca;
    private String modelo;
    private int anio;
    private Cliente cliente;

    public Automovil(String placa, String marca, String modelo, int anio, Cliente cliente) {
        setPlaca(placa);
        setMarca(marca);
        setModelo(modelo);
        setAnio(anio);
        this.cliente = cliente;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        if (placa == null || !Pattern.matches(ReglasNegocioAutomovil.xrPlaca, placa.trim()))
            throw new IllegalArgumentException("La placa no es valida");
        this.placa = placa;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        if (marca == null || marca.isBlank() || marca.trim().length() < ReglasNegocioAutomovil.longMinMarca || !Pattern.matches(ReglasNegocioAutomovil.xrMarca, marca))
            throw new IllegalArgumentException("La marca no es valido");
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        if (modelo == null || modelo.isBlank())
            throw new IllegalArgumentException("La longitud del nombre del modelo no es valido");
        this.modelo = modelo;
    }

    public int getAnio() {
        return anio;
    }

    public void setAnio(int anio) {
        if (anio > ReglasNegocioAutomovil.anioActual())
            throw new IllegalArgumentException("El año no es valido");
        this.anio = anio;
    }

    public Cliente getCliente() {
        return cliente;
    }
}
