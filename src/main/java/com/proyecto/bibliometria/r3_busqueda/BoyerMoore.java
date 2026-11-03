package com.proyecto.bibliometria.r3_busqueda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Algoritmo Boyer-Moore de busqueda exacta de patrones.

// Compara el patron contra el texto de DERECHA a IZQUIERDA.
// Cuando hay un fallo aplica la heuristica que da el mayor desplazamiento:

// 1) Regla del caracter malo (Bad Character):
// Al fallar en texto[s+j] con caracter c, desplaza el patron hasta que la ultima ocurrencia de c en el patron quede alineada con texto[s+j].
// Si c no aparece en el patron, se salta el patron entero.
// Desplazamiento = j - ultimaPos(c) (minimo 1).

// 2) Regla del sufijo bueno (Good Suffix):
// Los caracteres patron[j+1..m-1] ya coincidieron (sufijo bueno t).
// Desplaza el patron hasta la siguiente ocurrencia de t en el patron precedida de un caracter diferente, 
// o hasta que un prefijo de patron coincida con un sufijo de t.

// Complejidad:
// Preprocesamiento: O(m + |alfabeto|)

// Busqueda:
// Peor caso: O(n * m) (textos con caracteres repetidos, e.g. "aaa...a")
// Caso medio: O(n / m) (textos naturales, saltando sobre partes del texto)
// donde n = longitud del texto, m = longitud del patron.

public class BoyerMoore implements Buscador {

    @Override
    public String nombre() {
        return "Boyer-Moore";
    }

    // Tabla de caracter malo (Bad Character)

    // Para cada caracter del patron guarda su ultima posicion (0-based desde la
    // izquierda).
    // Si el caracter no aparece, se usa -1 al momento de consultar.
    // Se representa como arreglo de tamano 65536 (cubre todo Unicode BMP).

    // @param patron cadena a analizar
    // @return tabla indexada por char

    public static int[] tablaCaracterMalo(String patron) {
        int[] tabla = new int[65536];
        Arrays.fill(tabla, -1); // valor por defecto: caracter no presente
        for (int i = 0; i < patron.length(); i++) {
            tabla[patron.charAt(i)] = i; // guarda la posicion mas a la derecha
        }
        return tabla;
    }

    // Tabla de sufijo bueno (Good Suffix)

    // Calcula la tabla de desplazamiento por sufijo bueno.
    // shift[j] = cuanto desplazar el patron cuando la comparacion falla en la
    // posicion j-1 del patron (los ultimos m-j caracteres coincidieron).
    // shift[0] = desplazamiento cuando el patron completo coincidio.

    // Algoritmo (Z de Apostolico-Giancarlo / borde clasico):

    // Fase 1: construir el arreglo border[] de derecha a izquierda.
    // border[i] = posicion del borde del sufijo patron[i..m-1].

    // Fase 2: para posiciones sin shift aun asignado, usar el borde de todo
    // el patron (prefijo = sufijo del sufijo bueno).

    // @param patron cadena a analizar
    // @return arreglo shift de tamano m+1

    public static int[] tablaSufijoBueno(String patron) {
        int m = patron.length();
        int[] shift = new int[m + 1]; // inicializa a 0
        int[] border = new int[m + 1];

        // Fase 1: recorrer de derecha a izquierda
        int i = m;
        int j = m + 1;
        border[i] = j;

        while (i > 0) {
            // Retroceder j mientras los caracteres no coincidan
            while (j <= m && patron.charAt(i - 1) != patron.charAt(j - 1)) {
                // Si shift[j] no fue asignado aun, fijarlo
                if (shift[j] == 0) {
                    shift[j] = j - i;
                }
                j = border[j];
            }
            i--;
            j--;
            border[i] = j;
        }

        // Fase 2: rellenar posiciones aun en 0 con el borde del patron completo
        j = border[0];
        for (i = 0; i <= m; i++) {
            if (shift[i] == 0) {
                shift[i] = j;
            }
            if (i == j) {
                j = border[j]; // avanzar al siguiente borde
            }
        }
        return shift;
    }

    // Busqueda

    // Desliza el patron sobre el texto comparando de derecha a izquierda.
    // En cada posicion s:
    // Si j < 0: ocurrencia encontrada; desplazar shift[0].
    // Si fallo en j: calcular desplMalo y desplBueno, avanzar s por el maximo.

    @Override
    public List<Integer> buscar(String texto, String patron) {
        List<Integer> posiciones = new ArrayList<>();
        int n = texto.length();
        int m = patron.length();
        if (m == 0 || m > n)
            return posiciones;

        int[] tabMalo = tablaCaracterMalo(patron);
        int[] tabBueno = tablaSufijoBueno(patron);

        int s = 0; // desplazamiento del patron sobre el texto
        while (s <= n - m) {
            int j = m - 1; // comparar de derecha a izquierda

            while (j >= 0 && patron.charAt(j) == texto.charAt(s + j)) {
                j--;
            }

            if (j < 0) {
                // Patron encontrado en posicion s
                posiciones.add(s);
                s += tabBueno[0]; // desplazamiento cuando todo coincidio
            } else {
                // Calcular ambas heuristicas y avanzar por la mayor
                char charMalo = texto.charAt(s + j);
                int posMalo = tabMalo[charMalo]; // ultima pos del caracter malo en el patron (-1 si ausente)
                int desplMalo = j - posMalo; // bad character shift
                int desplBueno = tabBueno[j + 1]; // good suffix shift

                s += Math.max(desplMalo, desplBueno);
            }
        }
        return posiciones;
    }
}
