package com.proyecto.bibliometria.r1_similitud.algo_clasicos;

import java.util.List;

public class LCS {

    // Subsecuencia Comun Mas Larga (LCS) entre dos listas de tokens
    // Una subsecuencia mantiene el orden pero no tiene que ser contigua
    // Usa programacion dinamica con TABULACION (bottom-up)
    // m = cantidad de tokens de a
    // n = cantidad de tokens de b
    // Complejidad: tiempo O(m*n) (cada celda se calcula una vez)
    // Es sensible al orden
    public static int longitud(List<String> a, List<String> b) {
        int m = a.size(); // filas
        int n = b.size(); // columnas

        // tabla[i][j] = longitud del LCS entre los primeros i tokens de a
        // y los primeros j tokens de b
        // Caso base: fila 0 y columna 0 valen 0
        int[][] tabla = new int[m + 1][n + 1];

        // Llenar la tabla fila por fila, de izquierda a derecha
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a.get(i - 1).equals(b.get(j - 1))) {
                    // Tokens iguales = extiende el LCS de la diagonal en 1
                    tabla[i][j] = tabla[i - 1][j - 1] + 1;
                } else {
                    // Tokens distintos = se queda con el mejor entre ignorar el token de a (arriba)
                    // o ignorar el token de b (izquierda)
                    tabla[i][j] = Math.max(tabla[i - 1][j], tabla[i][j - 1]);
                }
            }
        }
        // La respuesta esta en la esquina inferior derecha
        // Este numero indica la longitud de la LCS
        return tabla[m][n];
    }

    // Convierte la longitud en una similitud entre 0 y 1
    // 1 = una lista esta contenida en orden dentro de la otra, 0 = nada en comun
    // Se divide entre el largo de la lista mas larga
    public static double similitud(List<String> a, List<String> b) {
        int max = Math.max(a.size(), b.size());
        if (max == 0) // dos listas vacias son identicas (evita dividir entre 0)
            return 1.0;
        return (double) longitud(a, b) / max;
    }
}