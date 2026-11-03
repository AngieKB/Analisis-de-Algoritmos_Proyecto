package com.proyecto.bibliometria.r3_busqueda;

import java.util.ArrayList;
import java.util.List;

// Algoritmo Rabin-Karp de busqueda exacta de patrones.

// Idea central: en vez de comparar el patron contra cada ventana de texto caracter a caracter, 
// calcula un HASH de la ventana actual y lo compara con el hash del patron.
// Solo si los hashes coinciden se verifica caracter a caracter (para descartar falsas alarmas / colisiones).

// Rolling Hash (hash deslizante):
// Representa la ventana texto[i..i+m-1] como un polinomio evaluado en BASE:
// H = (t[i]*BASE^(m-1) + t[i+1]*BASE^(m-2) + ... + t[i+m-1]) mod MOD
// Al avanzar una posicion (ventana i -> i+1):
// H_nuevo = (BASE * (H - t[i] * h) + t[i+m]) mod MOD donde h = BASE^(m-1) mod MOD.
// Esta actualizacion es O(1), haciendo el recorrido total O(n).

// Complejidad:
// Preprocesamiento: O(m)   (calcular hash del patron y ventana inicial)

// Busqueda:
// Caso medio:   O(n + m)  (pocas colisiones con un buen modulo primo)
// Peor caso:    O(n * m)  (colisiones en todas las ventanas)
// donde n = longitud del texto, m = longitud del patron.

public class RabinKarp implements Buscador {

    // Parametros del hash polinomial
    private static final long BASE = 256L; // tamanio del alfabeto (ASCII extendido)
    private static final long MOD = 1_000_000_007L; // primo grande para reducir colisiones

    @Override
    public String nombre() {
        return "Rabin-Karp";
    }

    // Busqueda

    @Override
    public List<Integer> buscar(String texto, String patron) {
        List<Integer> posiciones = new ArrayList<>();
        int n = texto.length();
        int m = patron.length();
        if (m == 0 || m > n)
            return posiciones;

        // h = BASE^(m-1) mod MOD
        // Se usa para restar la contribucion del caracter que sale de la ventana
        long h = 1;
        for (int k = 0; k < m - 1; k++) {
            h = (h * BASE) % MOD;
        }

        // Calcular hash del patron y de la primera ventana del texto
        long hashPatron = 0;
        long hashVentana = 0;
        for (int k = 0; k < m; k++) {
            hashPatron = (hashPatron * BASE + patron.charAt(k)) % MOD;
            hashVentana = (hashVentana * BASE + texto.charAt(k)) % MOD;
        }

        // Deslizar la ventana sobre el texto
        for (int i = 0; i <= n - m; i++) {

            // Comparar hashes
            if (hashPatron == hashVentana) {
                // Verificacion caracter a caracter para descartar colisiones
                if (verificar(texto, patron, i)) {
                    posiciones.add(i);
                }
            }

            // Calcular hash de la siguiente ventana con rolling hash (O(1))
            if (i < n - m) {
                // Restar el caracter que sale, multiplicar por BASE, sumar el que entra
                hashVentana = (BASE * (hashVentana - (long) texto.charAt(i) * h) + texto.charAt(i + m)) % MOD;
                // Garantizar valor positivo (Java puede dar modulo negativo)
                if (hashVentana < 0) {
                    hashVentana += MOD;
                }
            }
        }
        return posiciones;
    }

    // Compara caracter a caracter texto[inicio..inicio+m-1] con patron.
    // Se llama solo cuando hay coincidencia de hash (evento raro con un MOD
    // grande).

    // @param texto -> texto completo
    // @param patron -> patron a verificar
    // @param inicio -> posicion de inicio en el texto
    // @return true -> si hay coincidencia exacta
    private static boolean verificar(String texto, String patron, int inicio) {
        for (int k = 0; k < patron.length(); k++) {
            if (texto.charAt(inicio + k) != patron.charAt(k)) {
                return false;
            }
        }
        return true;
    }
}
