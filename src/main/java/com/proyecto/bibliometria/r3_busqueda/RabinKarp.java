package com.proyecto.bibliometria.r3_busqueda;

import java.util.ArrayList;
import java.util.List;

// Algoritmo Rabin-Karp de busqueda exacta de patrones.

// Idea central: 
// En vez de comparar el patron contra cada ventana de texto caracter a caracter, 
// calcula un HASH de la ventana actual y lo compara con el hash del patron.
// Solo si los hashes coinciden se verifica caracter a caracter (para descartar falsas alarmas / colisiones).

// Rolling Hash (hash deslizante):
// Representa la ventana texto[i..i+m-1] como un polinomio evaluado en BASE:
// H = (t[i]*BASE^(m-1) + t[i+1]*BASE^(m-2) + ... + t[i+m-1]) mod MOD
// Al avanzar una posicion (ventana i -> i+1):
// H_nuevo = (BASE * (H - t[i] * h) + t[i+m]) mod MOD
// donde h = BASE^(m-1) mod MOD.
// Esta actualizacion es O(1), haciendo el recorrido total O(n).

// Colisiones de hash:
// Ocurren cuando hashVentana == hashPatron pero los caracteres no coinciden.
// Se cuentan en MetricasBusqueda.colisionesHash.  Con MOD = 1_000_000_007
// son muy infrecuentes en textos naturales (probabilidad ~ 1/MOD por ventana).

// Complejidad:
// Preprocesamiento: O(m) -> calcular h, hashPatron y ventana inicial

// Busqueda:
// Caso medio: O(n + m) -> pocas colisiones con un MOD primo grande
// Peor caso: O(n * m) -> colisiones en todas las ventanas
// donde n = longitud del texto, m = longitud del patron.
public class RabinKarp implements Buscador {

    private static final long BASE = 256L; // tamano del alfabeto (ASCII extendido)
    private static final long MOD = 1_000_000_007L; // primo grande para reducir colisiones

    @Override
    public String nombre() {
        return "Rabin-Karp";
    }

    // Busqueda

    @Override
    public List<Integer> buscar(String texto, String patron, MetricasBusqueda m) {
        List<Integer> posiciones = new ArrayList<>();
        int n = texto.length();
        int mp = patron.length();
        if (mp == 0 || mp > n)
            return posiciones;

        // Preprocesamiento: calcular h, hashPatron y ventana inicial
        long t0 = System.nanoTime();

        // h = BASE^(m-1) mod MOD (factor del caracter que sale de la ventana)
        long h = 1;
        for (int k = 0; k < mp - 1; k++) {
            h = (h * BASE) % MOD;
        }

        long hashPatron = 0;
        long hashVentana = 0;
        for (int k = 0; k < mp; k++) {
            hashPatron = (hashPatron * BASE + patron.charAt(k)) % MOD;
            hashVentana = (hashVentana * BASE + texto.charAt(k)) % MOD;
        }
        m.preprocesamientoNs += System.nanoTime() - t0;

        // Busqueda
        t0 = System.nanoTime();

        for (int i = 0; i <= n - mp; i++) {

            if (hashPatron == hashVentana) {
                // Verificacion caracter a caracter para descartar colisiones
                if (verificar(texto, patron, i, m)) {
                    posiciones.add(i);
                } else {
                    m.colisionesHash++; // hash coincidio pero los chars no: colision falsa
                }
            }

            // Rolling hash: actualizar en O(1) para la siguiente ventana
            if (i < n - mp) {
                hashVentana = (BASE * (hashVentana - (long) texto.charAt(i) * h)
                        + texto.charAt(i + mp)) % MOD;
                if (hashVentana < 0)
                    hashVentana += MOD; // Java puede dar modulo negativo
                // Cada actualizacion del rolling hash es una operacion de hash O(1)
                // (multiplicacion + resta + suma + modulo). A diferencia de comparaciones
                // caracter-a-caracter, estas no se cuentan en m.comparaciones, por eso
                // se registran separadamente para que la tabla del informe sea honesta.
                m.operacionesHash++;
            }
        }
        m.busquedaNs += System.nanoTime() - t0;
        return posiciones;
    }

    // Compara caracter a caracter texto[inicio..inicio+m-1] con patron.
    // Se invoca solo cuando hay coincidencia de hash; cuenta cada comparacion.

    // @param texto -> texto completo
    // @param patron -> patron a verificar
    // @param inicio -> posicion de inicio en el texto
    // @param m -> acumulador de metricas
    // @return -> true si hay coincidencia exacta en todos los caracteres
    private static boolean verificar(String texto, String patron, int inicio, MetricasBusqueda m) {
        for (int k = 0; k < patron.length(); k++) {
            m.comparaciones++;
            if (texto.charAt(inicio + k) != patron.charAt(k))
                return false;
        }
        return true;
    }
}
