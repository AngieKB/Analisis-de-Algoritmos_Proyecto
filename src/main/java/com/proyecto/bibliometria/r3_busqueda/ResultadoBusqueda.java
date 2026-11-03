package com.proyecto.bibliometria.r3_busqueda;

import java.util.List;

// Encapsula el resultado de buscar un patron en un archivo del corpus.
public class ResultadoBusqueda {

    public final String archivo; // nombre del archivo .txt del abstract
    public final String algoritmo; // nombre del algoritmo usado
    public final String patron; // patron buscado
    public final List<Integer> posiciones; // indices donde empieza cada ocurrencia
    public final long tiempoNs; // tiempo de busqueda en nanosegundos

    public ResultadoBusqueda(String archivo, String algoritmo, String patron,
            List<Integer> posiciones, long tiempoNs) {
        this.archivo = archivo;
        this.algoritmo = algoritmo;
        this.patron = patron;
        this.posiciones = posiciones;
        this.tiempoNs = tiempoNs;
    }

    // Ocurrencias encontradas.
    public int ocurrencias() {
        return posiciones.size();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s → %d ocurrencia(s) en %s  (%.3f ms)",
                algoritmo, patron, posiciones.size(), archivo, tiempoNs / 1_000_000.0);
    }
}
