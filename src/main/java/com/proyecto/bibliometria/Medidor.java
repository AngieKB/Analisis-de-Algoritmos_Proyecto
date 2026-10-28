package com.proyecto.bibliometria;

import java.util.Arrays;

/**
 * Mide el tiempo de ejecucion de un algoritmo en milisegundos.
 * Hace calentamiento previo (la JVM compila el codigo en caliente) y
 * reporta mediana, minimo y promedio de varias repeticiones.
 */
public class Medidor {

    public static class Resultado {
        public final String algoritmo;
        public final double medianaMs;
        public final double minimoMs;
        public final double promedioMs;
        public final Termino[] ordenado; // Resultado de la ultima ejecucion

        Resultado(String algoritmo, double medianaMs, double minimoMs, double promedioMs, Termino[] ordenado) {
            this.algoritmo = algoritmo;
            this.medianaMs = medianaMs;
            this.minimoMs = minimoMs;
            this.promedioMs = promedioMs;
            this.ordenado = ordenado;
        }
    }

    public static Resultado medir(Ordenador ordenador, Termino[] datos, int calentamiento, int repeticiones) {
        Termino[] salida = null;

        // Calentamiento: no se mide, deja que el JIT compile el codigo
        for (int i = 0; i < calentamiento; i++) {
            salida = ordenador.ordenar(datos);
        }

        // Medicion
        double[] tiempos = new double[repeticiones];
        for (int i = 0; i < repeticiones; i++) {
            long inicio = System.nanoTime();
            salida = ordenador.ordenar(datos);
            long fin = System.nanoTime();
            tiempos[i] = (fin - inicio) / 1_000_000.0; // ns -> ms
        }

        // Ordena los tiempos medidos (no los terminos del ranking) para sacar la
        // mediana
        Arrays.sort(tiempos);
        double suma = 0;
        for (double t : tiempos) {
            suma += t;
        }
        double mediana = tiempos[repeticiones / 2];
        return new Resultado(ordenador.nombre(), mediana, tiempos[0], suma / repeticiones, salida);
    }
}