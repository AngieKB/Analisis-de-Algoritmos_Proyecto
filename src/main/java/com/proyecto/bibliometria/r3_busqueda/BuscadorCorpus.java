package com.proyecto.bibliometria.r3_busqueda;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

// Busca un patron de texto en todos los abstracts del corpus.

// Benchmark integrado:
// 1. Calentamiento (RONDAS_CALENTAMIENTO): 
// se ejecutan todos los algoritmos sobre todos los archivos sin registrar resultados,
// para que el JIT de la JVM compile el codigo antes de la medicion real.
// Sin esto, KMP paga el costo de compilacion y queda en desventaja.

// 2. Medicion (RONDAS_MEDICION):
// se ejecutan N veces y se toma la mediana de los tiempos, que es mas estable que el minimo 
// o el promedio ante picos esporadicos del sistema operativo.

// Separacion preprocesamiento / busqueda:
// Se reportan por separado el tiempo de construir las estructuras auxiliares
// (tablas LPS, caracter malo, sufijo bueno, hashes iniciales) y el tiempo de recorrer el texto, tal como lo pide el enunciado.
public class BuscadorCorpus {

    private static final int RONDAS_CALENTAMIENTO = 3;
    private static final int RONDAS_MEDICION = 7;

    private final String carpetaAbstracts;

    public BuscadorCorpus(String carpetaAbstracts) {
        this.carpetaAbstracts = carpetaAbstracts;
    }

    // Carga de archivos
    private File[] cargarArchivos() {
        File dir = new File(carpetaAbstracts);
        File[] archivos = dir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".txt"));
        if (archivos == null)
            return new File[0];
        Arrays.sort(archivos, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return archivos;
    }

    // Busqueda simple (una sola corrida, sin warmup ni repeticiones)

    // Busca el patron en todos los archivos .txt con una sola corrida.
    // Devuelve un ResultadoBusqueda por archivo con metricas individuales.

    // @param buscador -> algoritmo a usar
    // @param patron -> texto a localizar
    // @param ignorarMayusculas -> si true convierte texto y patron a minusculas
    // (Locale.ROOT)
    public List<ResultadoBusqueda> buscarEnCorpus(Buscador buscador, String patron,
            boolean ignorarMayusculas) throws IOException {
        File[] archivos = cargarArchivos();
        String patronBusqueda = ignorarMayusculas
                ? patron.toLowerCase(Locale.ROOT)
                : patron;

        List<ResultadoBusqueda> resultados = new ArrayList<>();
        for (File archivo : archivos) {
            String texto = Files.readString(archivo.toPath(), StandardCharsets.UTF_8);
            String textoBusqueda = ignorarMayusculas ? texto.toLowerCase(Locale.ROOT) : texto;

            MetricasBusqueda m = new MetricasBusqueda();
            List<Integer> posiciones = buscador.buscar(textoBusqueda, patronBusqueda, m);

            resultados.add(new ResultadoBusqueda(
                    archivo.getName(), buscador.nombre(), patron, posiciones, m));
        }
        return resultados;
    }

    // Benchmark con calentamiento y mediana

    // Ejecuta el algoritmo sobre el corpus con calentamiento JIT y multiples
    // repeticiones. Devuelve los resultados de la ultima corrida con las
    // metricas medianas de tiempo y el total de comparaciones acumulado.

    // @param buscador -> algoritmo a usar
    // @param patron -> texto a localizar
    // @param ignorarMayusculas -> si true convierte a minusculas (Locale.ROOT)
    public List<ResultadoBusqueda> benchmarkEnCorpus(Buscador buscador, String patron,
            boolean ignorarMayusculas) throws IOException {
        File[] archivos = cargarArchivos();
        String patronBusqueda = ignorarMayusculas
                ? patron.toLowerCase(Locale.ROOT)
                : patron;

        // Pre-cargar textos en memoria para no medir I/O
        String[] textos = new String[archivos.length];
        for (int i = 0; i < archivos.length; i++) {
            String raw = Files.readString(archivos[i].toPath(), StandardCharsets.UTF_8);
            textos[i] = ignorarMayusculas ? raw.toLowerCase(Locale.ROOT) : raw;
        }

        // Calentamiento: no se registran resultados
        for (int r = 0; r < RONDAS_CALENTAMIENTO; r++) {
            for (String texto : textos) {
                buscador.buscar(texto, patronBusqueda, new MetricasBusqueda());
            }
        }

        // Medicion: N rondas por archivo
        // preNs[archivo][ronda], busqNs[archivo][ronda]
        long[][] preNs = new long[archivos.length][RONDAS_MEDICION];
        long[][] busqNs = new long[archivos.length][RONDAS_MEDICION];
        long[] cmpTotal = new long[archivos.length];
        long[] colTotal = new long[archivos.length];
        @SuppressWarnings("unchecked")
        List<Integer>[] ultimasPosiciones = new ArrayList[archivos.length];

        for (int r = 0; r < RONDAS_MEDICION; r++) {
            for (int a = 0; a < archivos.length; a++) {
                MetricasBusqueda m = new MetricasBusqueda();
                List<Integer> pos = buscador.buscar(textos[a], patronBusqueda, m);
                preNs[a][r] = m.preprocesamientoNs;
                busqNs[a][r] = m.busquedaNs;
                if (r == RONDAS_MEDICION - 1) {
                    cmpTotal[a] = m.comparaciones;
                    colTotal[a] = m.colisionesHash;
                    ultimasPosiciones[a] = pos;
                }
            }
        }

        // Construir resultados con medianas de tiempo
        List<ResultadoBusqueda> resultados = new ArrayList<>();
        for (int a = 0; a < archivos.length; a++) {
            Arrays.sort(preNs[a]);
            Arrays.sort(busqNs[a]);
            long medPre = preNs[a][RONDAS_MEDICION / 2];
            long medBusq = busqNs[a][RONDAS_MEDICION / 2];

            MetricasBusqueda mMediana = new MetricasBusqueda();
            mMediana.preprocesamientoNs = medPre;
            mMediana.busquedaNs = medBusq;
            mMediana.comparaciones = cmpTotal[a];
            mMediana.colisionesHash = colTotal[a];

            resultados.add(new ResultadoBusqueda(
                    archivos[a].getName(), buscador.nombre(), patron,
                    ultimasPosiciones[a], mMediana));
        }
        return resultados;
    }

    // Impresion de resultados
    public static void imprimirResultados(List<ResultadoBusqueda> resultados) {
        if (resultados.isEmpty()) {
            System.out.println("  Sin resultados.");
            return;
        }

        long totalOcurrencias = 0;
        long totalPre = 0, totalBusq = 0, totalCmp = 0, totalCol = 0;

        for (ResultadoBusqueda r : resultados) {
            if (!r.posiciones.isEmpty()) {
                System.out.printf("  %-50s %2d ocurrencia(s) pos=%s%n",
                        r.archivo, r.ocurrencias(), r.posiciones);
            }
            totalOcurrencias += r.ocurrencias();
            totalPre += r.metricas.preprocesamientoNs;
            totalBusq += r.metricas.busquedaNs;
            totalCmp += r.metricas.comparaciones;
            totalCol += r.metricas.colisionesHash;
        }

        long archivosConOcurrencias = resultados.stream()
                .filter(r -> !r.posiciones.isEmpty()).count();
        System.out.printf(
                "  → %d ocurrencia(s) en %d archivo(s)%n" +
                        "    pre=%.4f ms  busq=%.4f ms  cmp=%d  col=%d%n",
                totalOcurrencias, archivosConOcurrencias,
                totalPre / 1_000_000.0,
                totalBusq / 1_000_000.0,
                totalCmp, totalCol);
    }

    // Punto de entrada interactivo
    public static void main(String[] args) throws IOException {
        String carpeta = "abstracts";
        String patron;

        if (args.length > 0) {
            patron = args[0];
        } else {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Ingrese el patron de busqueda: ");
            patron = scanner.nextLine().trim();
            scanner.close();
        }

        if (patron.isEmpty()) {
            System.out.println("El patron no puede estar vacio.");
            return;
        }

        BuscadorCorpus corpus = new BuscadorCorpus(carpeta);
        List<Buscador> algoritmos = List.of(
                new BusquedaIngenua(), new KMP(), new BoyerMoore(), new RabinKarp());

        System.out.printf("%n=== Busqueda: \"%s\" | calentamiento=%d rondas medicion=%d ===%n%n",
                patron, RONDAS_CALENTAMIENTO, RONDAS_MEDICION);

        for (Buscador b : algoritmos) {
            System.out.println("--- " + b.nombre() + " ---");
            List<ResultadoBusqueda> res = corpus.benchmarkEnCorpus(b, patron, true);
            imprimirResultados(res);
            System.out.println();
        }
    }
}
