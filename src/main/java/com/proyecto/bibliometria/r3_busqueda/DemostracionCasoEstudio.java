package com.proyecto.bibliometria.r3_busqueda;

// Demostracion paso a paso de los tres algoritmos de busqueda exacta de cadenas.
public class DemostracionCasoEstudio {

    private static final String SEP = "================================================================================";

    public static void main(String[] args) {
        String texto = "emergence generative artificial intelligence ai chatgpt";
        String patron = "ence";

        System.out.println(SEP);
        System.out.println("CASO DE ESTUDIO (Requerimiento 3)");
        System.out.println(SEP);
        System.out.printf("Patron (m=%d): \"%s\"%n", patron.length(), patron);
        System.out.printf("Texto (n=%d): \"%s\"%n", texto.length(), texto);
        System.out.println();
        System.out.println("Ocurrencias esperadas: posicion 5 (en \"emergence\") y posicion 40 (en \"intelligence\").");
        System.out.println();

        demostrarKMP(texto, patron);
        demostrarBoyerMoore(texto, patron);
        demostrarRabinKarp(texto, patron);
    }

    // 1. KNUTH-MORRIS-PRATT
    private static void demostrarKMP(String texto, String patron) {
        System.out.println(SEP);
        System.out.println("1. ALGORITMO KNUTH-MORRIS-PRATT (KMP)");
        System.out.println(SEP);

        int m = patron.length();
        int n = texto.length();
        int[] lps = KMP.tablaPrefijos(patron);

        // Tabla LPS
        System.out.println("TABLA DE PREFIJOS (LPS / Failure Function):");
        System.out.println();

        // cabecera: caracter
        System.out.print("caracter patron |");
        for (int k = 0; k < m; k++)
            System.out.printf("%c ", patron.charAt(k));
        System.out.println();

        // separador
        System.out.print("indice          |");
        for (int k = 0; k < m; k++)
            System.out.printf("%d ", k);
        System.out.println();

        System.out.print("LPS[i]          |");
        for (int v : lps)
            System.out.printf("%d ", v);
        System.out.println();
        System.out.println();

        System.out.println("Construccion paso a paso:");
        System.out.println("LPS[0] = 0  (un solo caracter no tiene prefijo propio)");
        {
            // generar explicacion para los demas indices
            int len = 0;
            for (int k = 1; k < m; k++) {
                String sub = patron.substring(0, k + 1);
                System.out.printf("LPS[%d] = %d  (subcadena \"%s\": prefijo propio mas largo que es sufijo)%n",
                        k, lps[k], sub);
            }
        }
        System.out.println();

        // Traza de busqueda
        System.out.println("TRAZA DE BUSQUEDA (i = indice en texto, j = indice en patron):");
        System.out.println();

        int i = 0, j = 0, paso = 1, cmpKMP = 0;
        while (i < n) {
            cmpKMP++;
            char ct = texto.charAt(i);
            char cp = patron.charAt(j);

            if (ct == cp) {
                System.out.printf("[%2d] i=%2d('%c') j=%d('%c')  COINCIDE -> i++, j++%n",
                        paso++, i, ct, j, cp);
                i++;
                j++;
                if (j == m) {
                    int pos = i - m;
                    System.out.printf("*** OCURRENCIA en posicion %d (\"%s\") ***%n", pos,
                            texto.substring(pos, pos + m));
                    System.out.printf("j vuelve a lps[%d] = %d (no retrocede i)%n",
                            j - 1, lps[j - 1]);
                    j = lps[j - 1];
                }
            } else {
                if (j != 0) {
                    System.out.printf(
                            "[%2d] i=%2d('%c') j=%d('%c')  FALLO -> j = lps[%d]=%d (i se mantiene)%n",
                            paso++, i, ct, j, cp, j - 1, lps[j - 1]);
                    j = lps[j - 1];
                } else {
                    System.out.printf(
                            "[%2d] i=%2d('%c') j=0('%c')  FALLO -> i++%n",
                            paso++, i, ct, cp);
                    i++;
                }
            }
        }
        System.out.printf("%n   Total comparaciones KMP: %d  (limite teorico 2n = %d)%n%n",
                cmpKMP, 2 * n);
    }

    // 2. BOYER-MOORE
    private static void demostrarBoyerMoore(String texto, String patron) {
        System.out.println(SEP);
        System.out.println("2. ALGORITMO BOYER-MOORE");
        System.out.println(SEP);

        int m = patron.length();
        int n = texto.length();
        int[] tabMalo = BoyerMoore.tablaCaracterMalo(patron);
        int[] tabBueno = BoyerMoore.tablaSufijoBueno(patron);

        // Tabla de caracter malo (solo chars presentes en el patron)
        System.out.println("ESTRUCTURAS AUXILIARES:");
        System.out.println();
        System.out.println("1. Tabla de Caracter Malo (ultima posicion en el patron):");
        System.out.println("(solo se muestran los chars presentes; el resto vale -1)");
        System.out.println();
        // Recorrer el patron y mostrar la ultima posicion de cada caracter distinto
        java.util.LinkedHashMap<Character, Integer> vistos = new java.util.LinkedHashMap<>();
        for (int k = 0; k < m; k++) {
            char c = patron.charAt(k);
            if (c < 256)
                vistos.put(c, tabMalo[c]);
        }
        for (var entry : vistos.entrySet()) {
            System.out.printf("tabMalo['%c'] = %d%n", entry.getKey(), entry.getValue());
        }
        System.out.println("tabMalo[cualquier otro] = -1");
        System.out.println();

        System.out.println("Ejemplo de desplazamiento por Caracter Malo:");
        System.out.println("Si el texto tiene 'c' en la posicion de comparacion j=3:");
        int ejemploJ = 3;
        char ejemploC = 'c';
        int ejemploPos = tabMalo[ejemploC];
        System.out.printf("despl = j - tabMalo['%c'] = %d - (%d) = %d%n",
                ejemploC, ejemploJ, ejemploPos, ejemploJ - ejemploPos);
        System.out.println();

        // Tabla de sufijo bueno
        System.out.println("2. Tabla de Sufijo Bueno (shift[j+1] = desplazamiento cuando fallo en posicion j):");
        System.out.println();
        System.out.printf("shift[0] = %d  (desplazamiento tras coincidencia completa del patron)%n",
                tabBueno[0]);
        for (int k = 1; k <= m; k++) {
            System.out.printf("shift[%d] = %d  (fallo en j=%d, sufijo bueno = \"%s\")%n",
                    k, tabBueno[k], k - 1, patron.substring(k));
        }
        System.out.println();

        // Traza de busqueda (comparacion derecha → izquierda)
        System.out.println("TRAZA DE ITERACIONES (comparacion de DERECHA a IZQUIERDA):");
        System.out.println();

        int s = 0, iter = 1, cmpBM = 0;
        while (s <= n - m) {
            // Mostrar alineacion actual
            String ventana = texto.substring(s, s + m);
            System.out.printf("[Iter %2d] s=%2d  ventana=\"%s\"%n", iter++, s, ventana);

            int j = m - 1;
            while (j >= 0) {
                cmpBM++;
                if (patron.charAt(j) != texto.charAt(s + j))
                    break;
                j--;
            }

            if (j < 0) {
                System.out.printf("*** COINCIDENCIA en s=%d (\"%s\") ***%n",
                        s, texto.substring(s, s + m));
                System.out.printf("Avance = shift[0] = %d%n", tabBueno[0]);
                s += tabBueno[0];
            } else {
                char cMalo = texto.charAt(s + j);
                int posMalo = (cMalo < 256) ? tabMalo[cMalo] : -1;
                int dMalo = j - posMalo;
                int dBueno = tabBueno[j + 1];
                int avance = Math.max(dMalo, dBueno);

                System.out.printf(
                        "Fallo j=%d: patron='%c' vs texto='%c'%n",
                        j, patron.charAt(j), cMalo);
                System.out.printf(
                        "Bad Char: j - tabMalo['%c'] = %d - (%d) = %d%n",
                        cMalo, j, posMalo, dMalo);
                System.out.printf(
                        "Good Suffix: shift[%d] = %d%n",
                        j + 1, dBueno);
                System.out.printf(
                        "Avance = max(%d, %d) = %d%n", dMalo, dBueno, avance);
                s += avance;
            }
            System.out.println();
        }
        System.out.printf("Total comparaciones Boyer-Moore: %d%n%n", cmpBM);
    }

    // 3. RABIN-KARP
    private static void demostrarRabinKarp(String texto, String patron) {
        System.out.println(SEP);
        System.out.println("3. ALGORITMO RABIN-KARP (Rolling Hash)");
        System.out.println(SEP);

        final long BASE = 256L;
        final long MOD = 1_000_000_007L;
        int m = patron.length();
        int n = texto.length();

        // Parametros
        System.out.println("PARAMETROS:");
        System.out.printf("BASE = %d  (tamano del alfabeto, ASCII extendido)%n", BASE);
        System.out.printf("MOD  = %,d  (primo grande para reducir colisiones)%n", MOD);
        System.out.printf("m    = %d%n%n", m);

        // h = BASE^(m-1) mod MOD
        long h = 1;
        for (int k = 0; k < m - 1; k++)
            h = (h * BASE) % MOD;
        System.out.printf("Factor h = 256^(m-1) mod MOD = 256^%d mod %,d = %,d%n%n",
                m - 1, MOD, h);

        // Hash del patron paso a paso
        System.out.printf("CALCULO DEL HASH DEL PATRON \"%s\":%n%n", patron);
        System.out.println(
                "Representacion polinomial:  H = t[0]*256^3 + t[1]*256^2 + t[2]*256 + t[3]");
        System.out.println();

        long hashP = 0;
        long valorSinMod = 0;
        for (int k = 0; k < m; k++) {
            char c = patron.charAt(k);
            long hPrev = hashP;
            valorSinMod = valorSinMod * BASE + c;
            hashP = (hashP * BASE + c) % MOD;
            System.out.printf("Paso %d  '%c'(=%3d):  (%,d * 256 + %d) = %,d  -->  mod %,d = %,d%n",
                    k + 1, c, (int) c, hPrev, (int) c, hPrev * BASE + c, MOD, hashP);
        }
        System.out.println();
        System.out.printf("Valor polinomial SIN reducir : %,d%n", valorSinMod);
        System.out.printf("%,d > %,d  =>  se aplica reduccion modular%n", valorSinMod, MOD);
        System.out.printf("H(patron) = %,d mod %,d = %,d%n%n", valorSinMod, MOD, hashP);

        // Rolling Hash sobre el texto
        System.out.println("TRAZA DE ROLLING HASH:");
        System.out.println();
        System.out.printf("H(patron) = %,d%n%n", hashP);

        // Calcular hash de la primera ventana
        long hashV = 0;
        for (int k = 0; k < m; k++)
            hashV = (hashV * BASE + texto.charAt(k)) % MOD;

        int colisiones = 0;
        int verificaciones = 0;
        // Indices de ventanas a imprimir (inicio, fin y alrededor de ocurrencias)
        // Mostrar ventanas 0-8, luego "...", luego 38-43
        int UMBRAL_INICIO = 8;
        int UMBRAL_FIN_A = 38;
        int UMBRAL_FIN_B = 44;
        boolean puntosImpresos = false;

        for (int i = 0; i <= n - m; i++) {
            String ventana = texto.substring(i, i + m);
            boolean imprimir = (i <= UMBRAL_INICIO)
                    || (i >= UMBRAL_FIN_A && i <= UMBRAL_FIN_B)
                    || (hashV == hashP); // siempre mostrar coincidencias y colisiones

            if (!imprimir && !puntosImpresos && i > UMBRAL_INICIO) {
                System.out.println(
                        "...  [ventanas intermedias: rolling hash actualizado en O(1)] ...");
                puntosImpresos = true;
            }

            if (imprimir) {
                if (hashV == hashP) {
                    verificaciones++;
                    boolean exacto = ventana.equals(patron);
                    if (exacto) {
                        System.out.printf(
                                "[i=%2d] \"%s\"  H=%,d  == H(P)  =>  verificacion char a char:  COINCIDENCIA ***%n",
                                i, ventana, hashV);
                    } else {
                        colisiones++;
                        System.out.printf(
                                "[i=%2d] \"%s\"  H=%,d  == H(P)  =>  verificacion char a char:  COLISION (falsa alarma)%n",
                                i, ventana, hashV);
                    }
                } else {
                    System.out.printf("[i=%2d] \"%s\"  H=%,d  != H(P)%n",
                            i, ventana, hashV);
                }
            }

            // Actualizar rolling hash
            if (i < n - m) {
                char cSale = texto.charAt(i);
                char cEntra = texto.charAt(i + m);
                hashV = (BASE * (hashV - (long) cSale * h) + cEntra) % MOD;
                if (hashV < 0)
                    hashV += MOD;
            }
        }

        System.out.println();
        System.out.printf("Colisiones de hash (falsas alarmas) : %d%n", colisiones);
        System.out.printf("Verificaciones char a char          : %d%n", verificaciones);
        System.out.printf("Probabilidad teorica por ventana    : 1 / %,d  ~  %.2e%n%n",
                MOD, 1.0 / MOD);
        System.out.println(SEP);
    }
}
