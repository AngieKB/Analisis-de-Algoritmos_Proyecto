package com.proyecto.bibliometria.r3_busqueda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Algoritmo Boyer-Moore de busqueda exacta de patrones.
//
// Compara el patron contra el texto de DERECHA a IZQUIERDA.
// Cuando hay un fallo aplica la heuristica que produce el mayor desplazamiento:
//
// 1) Regla del caracter malo (Bad Character):
//    Al fallar en texto[s+j] con caracter c, desplaza el patron hasta que la
//    ocurrencia de c en el patron quede alineada con texto[s+j].
//    Si c no aparece a la izquierda de j, se salta mas alla de esa posicion.
//    Desplazamiento = j - ultimaPos(c)   (al menos 1).
//
// 2) Regla del sufijo bueno (Good Suffix):
//    Los caracteres patron[j+1..m-1] ya coincidieron (sufijo bueno t).
//    Desplaza el patron hasta la siguiente ocurrencia de t precedida de un
//    caracter diferente, o hasta que un prefijo de patron coincida con un
//    sufijo de t.
//
// Complejidad:
//   Preprocesamiento: O(m + |Sigma|)   donde |Sigma| = 256 (ASCII extendido)
//   Busqueda:
//     Peor caso:  O(n * m)  (textos muy repetitivos, e.g. "aaa...a")
//     Caso medio: O(n / m)  (textos naturales; los saltos suelen superar m/2)
//   donde n = longitud del texto, m = longitud del patron.
//
// Nota sobre el tamano de la tabla de caracter malo:
//   Se usan 256 entradas (ASCII extendido) para acceso O(1) de los caracteres habituales.
//   Los caracteres fuera del rango 0-255 (ej. comillas tipograficas \u2019, guiones largos,
//   ligaduras procedentes de PDFs) se resuelven mediante busqueda hacia atras en el patron
//   solo cuando aparecen en el texto, evitando crear una tabla gigante de 65 536 entradas
//   cuyo Arrays.fill dominaria artificialmente el tiempo en textos cortos.
public class BoyerMoore implements Buscador {

    private static final int TAM_ALFABETO = 256; // ASCII extendido

    @Override
    public String nombre() {
        return "Boyer-Moore";
    }

    // Tabla de caracter malo para los primeros 256 caracteres Unicode
    public static int[] tablaCaracterMalo(String patron) {
        int[] tabla = new int[TAM_ALFABETO];
        Arrays.fill(tabla, -1);
        for (int i = 0; i < patron.length(); i++) {
            char c = patron.charAt(i);
            if (c < TAM_ALFABETO) {
                tabla[c] = i;
            }
        }
        return tabla;
    }

    // Busca el ultimo k < limite con patron.charAt(k) == c; -1 si no existe.
    // Maneja caracteres >= 256 sin requerir una tabla de 65 536 entradas.
    private static int ultimaPosicion(String patron, char c, int limite) {
        for (int k = limite - 1; k >= 0; k--) {
            if (patron.charAt(k) == c) {
                return k;
            }
        }
        return -1;
    }

    // Tabla de sufijo bueno (metodo de bordes - Strong Good Suffix rule)
    //
    // shift[j] = cuanto desplazar cuando el fallo ocurre en j-1
    // shift[0] = cuanto desplazar cuando todo el patron coincidio
    public static int[] tablaSufijoBueno(String patron) {
        int m = patron.length();
        int[] shift  = new int[m + 1]; // inicialmente 0
        int[] border = new int[m + 1];

        // Fase 1: bordes de cada sufijo
        int i = m;
        int j = m + 1;
        border[i] = j;

        while (i > 0) {
            while (j <= m && patron.charAt(i - 1) != patron.charAt(j - 1)) {
                if (shift[j] == 0) shift[j] = j - i;
                j = border[j];
            }
            i--;
            j--;
            border[i] = j;
        }

        // Fase 2: rellenar con el borde del patron completo
        j = border[0];
        for (i = 0; i <= m; i++) {
            if (shift[i] == 0) shift[i] = j;
            if (i == j) j = border[j];
        }
        return shift;
    }

    @Override
    public List<Integer> buscar(String texto, String patron, MetricasBusqueda m) {
        List<Integer> posiciones = new ArrayList<>();
        int n = texto.length();
        int mp = patron.length();
        if (mp == 0 || mp > n) return posiciones;

        // Preprocesamiento
        long t0 = System.nanoTime();
        int[] tabMalo  = tablaCaracterMalo(patron);
        int[] tabBueno = tablaSufijoBueno(patron);
        m.preprocesamientoNs += System.nanoTime() - t0;

        // Busqueda
        t0 = System.nanoTime();
        int s = 0; // desplazamiento del patron sobre el texto

        while (s <= n - mp) {
            int j = mp - 1; // comparar de derecha a izquierda

            // Contar cada comparacion, incluida la que produce el fallo
            while (j >= 0) {
                m.comparaciones++;
                if (patron.charAt(j) != texto.charAt(s + j)) break;
                j--;
            }

            if (j < 0) {
                posiciones.add(s);
                s += tabBueno[0];
            } else {
                char charMalo = texto.charAt(s + j);
                int posMalo = (charMalo < TAM_ALFABETO)
                        ? tabMalo[charMalo]
                        : ultimaPosicion(patron, charMalo, j);
                int desplMalo  = j - posMalo;
                int desplBueno = tabBueno[j + 1];
                s += Math.max(desplMalo, desplBueno);
            }
        }
        m.busquedaNs += System.nanoTime() - t0;
        return posiciones;
    }
}
