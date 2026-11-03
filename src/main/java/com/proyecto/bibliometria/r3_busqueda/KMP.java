package com.proyecto.bibliometria.r3_busqueda;

import java.util.ArrayList;
import java.util.List;

// Algoritmo Knuth-Morris-Pratt (KMP) de busqueda exacta de patrones.

// Idea central: 
// antes de buscar, se analiza el patron para construir la "tabla de prefijos" (failure function / LPS array).
// lps[i] = longitud del prefijo propio mas largo de patron[0..i] que tambien es sufijo de ese mismo prefijo.
// Esto permite que, al producirse un fallo durante la comparacion, el indice j en el patron retroceda solo hasta lps[j-1] en vez de 0,
// evitando repetir comparaciones ya realizadas sobre el texto.

// Complejidad:
// Preprocesamiento: O(m) -> construir la tabla LPS
// Busqueda: O(n) -> cada caracter del texto se examina como maximo una vez
// Total: O(n + m)
// donde n = longitud del texto, m = longitud del patron.
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
    // Si hay fallo y len > 0, se retrocede: len = lps[len-1] (sin mover i).
    // Si hay fallo y len == 0, lps[i] = 0 y se avanza i.

    // @param patron -> cadena a analizar
    // @return -> arreglo lps de longitud m
    public static int[] tablaPrefijos(String patron) {
        int m = patron.length();
        int[] lps = new int[m];
        lps[0] = 0;

        int len = 0;
        int i = 1;
        while (i < m) {
            if (patron.charAt(i) == patron.charAt(len)) {
                lps[i++] = ++len;
            } else if (len != 0) {
                len = lps[len - 1]; // retroceder sin mover i
            } else {
                lps[i++] = 0;
            }
        }
        return lps;
    }

    // Busqueda

    // Busca el patron en el texto usando la tabla LPS.
    // Mide por separado el tiempo de preprocesamiento y el de busqueda,
    // y cuenta cada comparacion caracter-a-caracter sobre el texto.

    // i recorre el texto; j recorre el patron.
    // Coincidencia: i++, j++.
    // Patron completo (j == m): se registra la posicion y j = lps[j-1].
    // Fallo con j > 0: j = lps[j-1] (sin mover i, sin comparacion extra).
    // Fallo con j == 0: i++ (avanzar el texto).
    @Override
    public List<Integer> buscar(String texto, String patron, MetricasBusqueda m) {
        List<Integer> posiciones = new ArrayList<>();
        int n = texto.length();
        int mp = patron.length();
        if (mp == 0 || mp > n) return posiciones;

        // Preprocesamiento: construir tabla LPS
        long t0 = System.nanoTime();
        int[] lps = tablaPrefijos(patron);
        m.preprocesamientoNs += System.nanoTime() - t0;

        // Busqueda
        t0 = System.nanoTime();
        int i = 0; // indice en el texto
        int j = 0; // indice en el patron

        while (i < n) {
            m.comparaciones++; // comparacion texto[i] vs patron[j]
            if (texto.charAt(i) == patron.charAt(j)) {
                i++;
                j++;
                if (j == mp) {
                    posiciones.add(i - mp);
                    j = lps[j - 1]; // buscar proxima (puede solapar)
                }
            } else {
                if (j != 0) {
                    // i nunca retrocede; j retrocede usando la tabla LPS.
                    // El mismo texto[i] se comparara de nuevo en la proxima iteracion
                    // con un j mas pequeno. Esto garantiza que el total de comparaciones
                    // es a lo sumo 2n (i avanza n veces y j no puede retroceder mas
                    // de lo que avanzo).
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }
        m.busquedaNs += System.nanoTime() - t0;
        return posiciones;
    }
}
