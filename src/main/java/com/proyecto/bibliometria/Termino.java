package com.proyecto.bibliometria;

public class Termino {
    public final String palabra;
    public int frecuencia;

    public Termino(String palabra, int frecuencia) {
        this.palabra = palabra;
        this.frecuencia = frecuencia;
    }

    @Override
    public String toString() {
        return palabra + ": " + frecuencia;
    }
}
