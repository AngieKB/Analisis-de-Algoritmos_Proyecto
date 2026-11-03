package com.proyecto.bibliometria.r1_similitud.algo_clasicos;

import java.util.List;

public class Levenshtein {

    // Distancia de Levenshtein entre dos listas de tokens
    // Este algoritmo cuenta la distancia minima de edicion entre dos secuencias de
    // tokens, por medio de las operaciones: insercion, eliminacion y sustitucion.
    // Cada operacion cuesta 1.

    // Usa programacion dinamica con tabulacion (bottom-up)
    // Se llena una tabla con ciclos, empezando por los casos base
    // m = cantidad de tokens de a
    // n = cantidad de tokens de b
    // Complejidad: O(m*n)

    public static int distancia(List<String> a, List<String> b) {
        int m = a.size(); // cantidad de tokens de a (filas)
        int n = b.size(); // cantidad de tokens de b (columnas)

        // tabla[i][j] = distancia entre los primeros i tokens de a
        // y los primeros j tokens de b
        // El +1 es porque hace falta la fila 0 y la columna 0 (lista vacia)
        int[][] tabla = new int[m + 1][n + 1];

        // Caso base 1: convertir i tokens de a en "nada" cuesta i eliminaciones
        for (int i = 0; i <= m; i++) {
            tabla[i][0] = i;
        }

        // Caso base 2: convertir "nada" en j tokens de b cuesta j inserciones
        for (int j = 0; j <= n; j++) {
            tabla[0][j] = j;
        }

        // Llena el resto de la tabla, fila por fila, de izquierda a derecha
        // En este orden, las 3 celdas vecinas que se necesitan ya estan llenas
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {

                // El token numero i de a esta en la posicion i-1 de la lista
                // El token numero j de b esta en la posicion j-1 de la lista
                String tokenA = a.get(i - 1);
                String tokenB = b.get(j - 1);

                if (tokenA.equals(tokenB)) {
                    // Tokens iguales: no cuesta nada, se copia la diagonal
                    tabla[i][j] = tabla[i - 1][j - 1];
                } else {
                    // Tokens distintos: se prueban las 3 operaciones
                    int eliminacion = tabla[i - 1][j]; // arriba: borrar token de a
                    int insercion = tabla[i][j - 1]; // izquierda: insertar token en a
                    int sustitucion = tabla[i - 1][j - 1]; // diagonal: cambiar un token por otro

                    // Se elige la mas barata y se suma 1 por la operacion hecha
                    tabla[i][j] = 1 + Math.min(Math.min(eliminacion, insercion), sustitucion);
                }
            }
        }

        // La respuesta esta en la esquina inferior derecha:
        // todos los tokens de a contra todos los tokens de b
        return tabla[m][n];
    }

    // Convierte la distancia en una similitud entre 0 y 1
    // 1 = listas identicas, 0 = nada en comun
    // Se divide entre el largo de la lista mas larga porque esa es la
    // distancia maxima posible (borrar todo y escribir todo de nuevo)
    public static double similitud(List<String> a, List<String> b) {
        int max = Math.max(a.size(), b.size());
        if (max == 0) // dos listas vacias son identicas (evita dividir entre 0)
            return 1.0;
        return 1.0 - (double) distancia(a, b) / max;
    }
}