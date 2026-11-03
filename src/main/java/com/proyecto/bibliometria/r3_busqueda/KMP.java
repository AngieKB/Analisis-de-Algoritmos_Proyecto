package com.proyecto.bibliometria.r3_busqueda;

import java.util.ArrayList;
import java.util.List;

// Algoritmo Knuth-Morris-Pratt (KMP) de busqueda exacta de patrones.

// Idea central: antes de buscar, se analiza el patron para construir la
// "tabla de prefijos" (failure function / LPS array).
// lps[i] = longitud del prefijo propio mas largo de patron[0..i] que tambien es sufijo de ese mismo prefijo.
// Esto permite que, al producirse un fallo durante la comparacion, el indice j en el patron retroceda solo hasta lps[j-1] en vez de 0, 
// evitando repetir comparaciones ya realizadas.

// Complejidad:
// Preprocesamiento: O(m) -> (construir la tabla LPS)
// Busqueda:         O(n) -> (cada caracter del texto se examina como maximo una vez)
// Total:            O(n + m) -> donde n = longitud del texto, m = longitud del patron.

public class KMP implements Buscador {

    @Override
    public String nombre() {
        return "KMP";
    }

    // Tabla de prefijos (LPS = Longest Proper Prefix which is also Suffix)

    // Construye la tabla de prefijos del patron.
    // lps[0] siempre es 0 (un unico caracter no tiene prefijo propio).
    // Para i > 0:
    // Si patron[i] == patron[len], se extiende el prefijo: lps[i] = ++len.
    // Si hay fallo y len > 0, se retrocede: len = lps[len-1] (no se mueve i).
    // Si hay fallo y len == 0, lps[i] = 0 y se avanza i.

    // @param patron -> cadena a analizar
    // @return -> arreglo lps de longitud m

    public static int[] tablaPrefijos(String patron) {
        int m = patron.length();
        int[] lps = new int[m];
        lps[0] = 0;

        int len = 0; // longitud del prefijo propio mas largo actual
        int i = 1;
        while (i < m) {
            if (patron.charAt(i) == patron.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else {
                if (len != 0) {
                    // Retroceder usando la tabla ya construida (sin mover i)
                    len = lps[len - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }
        return lps;
    }

    // Busqueda

    // Busca el patron en el texto usando la tabla LPS.
    // i recorre el texto; j recorre el patron.
    // Coincidencia: i++, j++.
    // Patron completo (j == m): se registra la posicion i - m y se retrocede j a
    // lps[j-1] para buscar solapamientos.
    // Fallo con j > 0: j = lps[j-1] (sin mover i).
    // Fallo con j == 0: i++ (avanzar el texto).

    @Override
    public List<Integer> buscar(String texto, String patron) {
        List<Integer> posiciones = new ArrayList<>();
        int n = texto.length();
        int m = patron.length();
        if (m == 0 || m > n)
            return posiciones;

        int[] lps = tablaPrefijos(patron);

        int i = 0; // indice en el texto
        int j = 0; // indice en el patron

        while (i < n) {
            if (texto.charAt(i) == patron.charAt(j)) {
                i++;
                j++;
                if (j == m) {
                    posiciones.add(i - m); // ocurrencia encontrada en i-m
                    j = lps[j - 1]; // buscar proxima ocurrencia (puede solapar)
                }
            } else {
                if (j != 0) {
                    j = lps[j - 1]; // retroceder j sin mover i
                } else {
                    i++; // avanzar el texto
                }
            }
        }
        return posiciones;
    }
}
