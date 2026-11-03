package r3_busqueda_test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.proyecto.bibliometria.r3_busqueda.BoyerMoore;
import com.proyecto.bibliometria.r3_busqueda.BusquedaIngenua;
import com.proyecto.bibliometria.r3_busqueda.Buscador;
import com.proyecto.bibliometria.r3_busqueda.KMP;
import com.proyecto.bibliometria.r3_busqueda.MetricasBusqueda;
import com.proyecto.bibliometria.r3_busqueda.RabinKarp;

// Benchmark de escalabilidad para el informe.
//
// Ejecuta los cuatro algoritmos sobre textos de n = 10^4, 10^5 y 10^6
// caracteres, mas dos casos adversos que destacan las diferencias asintoticas:
//
//   Caso A: texto a^n, patron a^(m-1)b
//     Castiga a la Busqueda Ingenua: en cada posicion coinciden m-1 caracteres
//     antes de fallar, dando O(n*m) comparaciones.
//     KMP y BM lo resuelven en O(n).
//
//   Caso B: texto a^n, patron a^m
//     Castiga a Boyer-Moore con muchas ocurrencias: cada posicion es una
//     ocurrencia, BM no puede saltar y degenera a O(n*m).
//     KMP lo resuelve en O(n).
//
// Nota sobre el ruido de nanoTime en preprocesamiento:
//   Para patrones de ~8 chars, construir la tabla LPS toma decenas de ns,
//   cerca del costo de la propia llamada a nanoTime. Para el informe, repite
//   el preprocesamiento N_PRE veces y divide para obtener un promedio estable.
public class BenchmarkEscalabilidad {

    private static final int CALENTAMIENTO = 5;
    private static final int REPETICIONES = 9;

    // Para medir preprocesamiento con menos ruido de nanoTime: repite N_PRE veces y
    // promedia.
    private static final int N_PRE = 10_000;

    private static final List<Buscador> ALGORITMOS = List.of(
            new BusquedaIngenua(), new KMP(), new BoyerMoore(), new RabinKarp());

    // -----------------------------------------------------------------------
    // Escalabilidad con texto aleatorio (patron corto y frecuente)
    // -----------------------------------------------------------------------

    @Test
    void escalabilidadTextoAleatorio() {
        int[] tamanios = { 10_000, 100_000, 1_000_000 };
        String patron = "learning";

        System.out.println("\n=== Escalabilidad texto aleatorio | patron=\"" + patron + "\" ===");
        System.out.printf("%-14s  %10s  %10s  %10s  %12s  %12s%n",
                "Algoritmo", "10^4 ms", "10^5 ms", "10^6 ms", "cmp@10^6", "pre_avg_ns");
        System.out.println("-".repeat(80));

        for (Buscador b : ALGORITMOS) {
            double[] ms = new double[tamanios.length];
            long[] cmps = new long[tamanios.length];
            long[] preNs = new long[tamanios.length];

            for (int idx = 0; idx < tamanios.length; idx++) {
                String texto = generarAleatorio(tamanios[idx], 42);
                long[] ns = new long[REPETICIONES];
                MetricasBusqueda mLast = new MetricasBusqueda();

                // Calentamiento
                for (int r = 0; r < CALENTAMIENTO; r++)
                    b.buscar(texto, patron, new MetricasBusqueda());

                // Medicion (mediana de tiempos totales)
                for (int r = 0; r < REPETICIONES; r++) {
                    MetricasBusqueda m = new MetricasBusqueda();
                    long t0 = System.nanoTime();
                    b.buscar(texto, patron, m);
                    ns[r] = System.nanoTime() - t0;
                    mLast = m;
                }
                Arrays.sort(ns);
                ms[idx] = ns[REPETICIONES / 2] / 1_000_000.0;
                cmps[idx] = mLast.comparaciones;

                // Preprocesamiento promediado sobre N_PRE repeticiones
                long preAcum = 0;
                for (int r = 0; r < N_PRE; r++) {
                    MetricasBusqueda m = new MetricasBusqueda();
                    b.buscar(texto.substring(0, Math.min(600, texto.length())), patron, m);
                    preAcum += m.preprocesamientoNs;
                }
                preNs[idx] = preAcum / N_PRE;
            }

            System.out.printf("%-14s  %10.3f  %10.3f  %10.3f  %12d  %12d%n",
                    b.nombre(), ms[0], ms[1], ms[2], cmps[2], preNs[2]);
        }
    }

    // -----------------------------------------------------------------------
    // Caso adverso A: a^n + patron a^(m-1)b → castiga Busqueda Ingenua
    // -----------------------------------------------------------------------

    @Test
    void casoAdversoIngenua() {
        int n = 100_000;
        int m = 8;
        String texto = "a".repeat(n);
        String patron = "a".repeat(m - 1) + "b"; // aaaaaab

        System.out.println("\n=== Caso adverso Ingenua: a^" + n + "  patron=a^" + (m - 1) + "b ===");
        System.out.printf("%-14s  %10s  %12s  %8s%n", "Algoritmo", "mediana ms", "comparaciones", "ocurr.");
        System.out.println("-".repeat(52));

        // El oraculo nos da 0 ocurrencias (el patron nunca aparece)
        List<Integer> oracle = new BusquedaIngenua().buscar(texto, patron);

        for (Buscador b : ALGORITMOS) {
            long[] ns = new long[REPETICIONES];
            MetricasBusqueda mLast = new MetricasBusqueda();

            for (int r = 0; r < CALENTAMIENTO; r++)
                b.buscar(texto, patron, new MetricasBusqueda());

            for (int r = 0; r < REPETICIONES; r++) {
                MetricasBusqueda mRun = new MetricasBusqueda();
                long t0 = System.nanoTime();
                List<Integer> pos = b.buscar(texto, patron, mRun);
                ns[r] = System.nanoTime() - t0;
                if (r == REPETICIONES - 1) {
                    mLast = mRun;
                    assertEquals(oracle, pos, b.nombre() + " difiere del oraculo");
                }
            }
            Arrays.sort(ns);
            System.out.printf("%-14s  %10.3f  %12d  %8d%n",
                    b.nombre(), ns[REPETICIONES / 2] / 1_000_000.0,
                    mLast.comparaciones, oracle.size());
        }
    }

    // -----------------------------------------------------------------------
    // Caso adverso B: a^n + patron a^m → muchas ocurrencias, castiga BM
    // -----------------------------------------------------------------------

    @Test
    void casoAdversoBoyerMoore() {
        int n = 100_000;
        int m = 4;
        String texto = "a".repeat(n);
        String patron = "a".repeat(m); // aaaa

        System.out.println("\n=== Caso adverso BM: a^" + n + "  patron=a^" + m + " ===");
        System.out.printf("%-14s  %10s  %12s  %8s%n", "Algoritmo", "mediana ms", "comparaciones", "ocurr.");
        System.out.println("-".repeat(52));

        List<Integer> oracle = new BusquedaIngenua().buscar(texto, patron);

        for (Buscador b : ALGORITMOS) {
            long[] ns = new long[REPETICIONES];
            MetricasBusqueda mLast = new MetricasBusqueda();

            for (int r = 0; r < CALENTAMIENTO; r++)
                b.buscar(texto, patron, new MetricasBusqueda());

            for (int r = 0; r < REPETICIONES; r++) {
                MetricasBusqueda mRun = new MetricasBusqueda();
                long t0 = System.nanoTime();
                List<Integer> pos = b.buscar(texto, patron, mRun);
                ns[r] = System.nanoTime() - t0;
                if (r == REPETICIONES - 1) {
                    mLast = mRun;
                    assertEquals(oracle, pos, b.nombre() + " difiere del oraculo");
                }
            }
            Arrays.sort(ns);
            System.out.printf("%-14s  %10.3f  %12d  %8d%n",
                    b.nombre(), ns[REPETICIONES / 2] / 1_000_000.0,
                    mLast.comparaciones, oracle.size());
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String generarAleatorio(int n, long seed) {
        java.util.Random rnd = new java.util.Random(seed);
        char[] chars = new char[n];
        for (int i = 0; i < n; i++) {
            chars[i] = (char) ('a' + rnd.nextInt(26));
        }
        return new String(chars);
    }
}