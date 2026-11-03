package com.proyecto.bibliometria.r3_busqueda;

import java.util.ArrayList;
import java.util.List;

// Busqueda ingenua (Fuerza Bruta) de patrones.

// Algoritmo: dos bucles anidados.
// Para cada posicion i del texto, compara el patron desde esa posicion.
// Si alguna comparacion falla, avanza i en 1 y reinicia j a 0.

// Rol en el proyecto:
// 1. Linea base O(n*m) frente a la que se evalua la mejora de KMP, Boyer-Moore y Rabin-Karp.
// 2. Oraculo de correctitud: los tres algoritmos deben devolver exactamente las mismas posiciones que este.

// Complejidad:
// Peor caso: O(n * m) -> (ej. texto "aaa...a" y patron "aaa...ab")
// Mejor caso: O(n) -> (fallo siempre en el primer caracter)

public class BusquedaIngenua implements Buscador {

    @Override
    public String nombre() {
        return "Ingenua";
    }

    @Override
    public List<Integer> buscar(String texto, String patron, MetricasBusqueda m) {
        List<Integer> posiciones = new ArrayList<>();
        int n = texto.length();
        int p = patron.length();
        if (p == 0 || p > n)
            return posiciones;

        long t0 = System.nanoTime();

        for (int i = 0; i <= n - p; i++) {
            int j = 0;
            // Comparar caracter a caracter desde la posicion i
            while (j < p) {
                m.comparaciones++;
                if (texto.charAt(i + j) != patron.charAt(j))
                    break;
                j++;
            }
            if (j == p)
                posiciones.add(i); // coincidencia completa
        }

        m.busquedaNs += System.nanoTime() - t0;
        return posiciones;
    }
}
