package com.proyecto.bibliometria.r3_busqueda;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

// Busca un patron de texto en todos los abstracts del corpus.
// Soporta busqueda sensible e insensible a mayusculas.
// Mide el tiempo de cada busqueda individual con System.nanoTime().

public class BuscadorCorpus {

    private final String carpetaAbstracts;

    public BuscadorCorpus(String carpetaAbstracts) {
        this.carpetaAbstracts = carpetaAbstracts;
    }

    // Busqueda en el corpus completo

    // Busca el patron en todos los archivos .txt de la carpeta de abstracts.

    // @param buscador -> algoritmo a usar
    // @param patron -> texto a localizar
    // @param ignorarMayusculas -> si true, convierte texto y patron a minusculas
    // antes de buscar
    // @return -> lista de resultados, uno por archivo

    public List<ResultadoBusqueda> buscarEnCorpus(Buscador buscador,
            String patron,
            boolean ignorarMayusculas) throws IOException {
        File dir = new File(carpetaAbstracts);
        File[] archivos = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".txt"));

        if (archivos == null || archivos.length == 0) {
            System.out.println("No se encontraron abstracts en: " + carpetaAbstracts);
            return new ArrayList<>();
        }

        // Ordenar por nombre para resultados reproducibles
        Arrays.sort(archivos, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        String patronBusqueda = ignorarMayusculas ? patron.toLowerCase() : patron;
        List<ResultadoBusqueda> resultados = new ArrayList<>();

        for (File archivo : archivos) {
            String texto = Files.readString(archivo.toPath(), StandardCharsets.UTF_8);
            String textoBusqueda = ignorarMayusculas ? texto.toLowerCase() : texto;

            long inicio = System.nanoTime();
            List<Integer> posiciones = buscador.buscar(textoBusqueda, patronBusqueda);
            long tiempoNs = System.nanoTime() - inicio;

            resultados.add(new ResultadoBusqueda(
                    archivo.getName(), buscador.nombre(), patron, posiciones, tiempoNs));
        }
        return resultados;
    }

    // Imprimir resumen en consola

    // @brief -> Imprime los resultados de una busqueda agrupados por algoritmo.
    // @brief -> Solo muestra los archivos donde hay al menos una ocurrencia.
    // @param -> resultados -> lista de resultados
    public static void imprimirResultados(List<ResultadoBusqueda> resultados) {
        if (resultados.isEmpty()) {
            System.out.println("Sin resultados.");
            return;
        }

        int totalOcurrencias = 0;
        for (ResultadoBusqueda r : resultados) {
            if (!r.posiciones.isEmpty()) {
                System.out.printf("  %-50s  %2d ocurrencia(s) en posiciones %s%n",
                        r.archivo, r.ocurrencias(), r.posiciones);
                totalOcurrencias += r.ocurrencias();
            }
        }
        long totalNs = resultados.stream().mapToLong(r -> r.tiempoNs).sum();
        System.out.printf("  → Total: %d ocurrencia(s) en %d archivo(s)  |  %.3f ms%n",
                totalOcurrencias,
                (int) resultados.stream().filter(r -> !r.posiciones.isEmpty()).count(),
                totalNs / 1_000_000.0);
    }

    // Punto de entrada interactivo: permite al usuario ingresar el patron por consola.
    // Si se pasa un argumento por linea de comandos, se usa ese directamente.

    public static void main(String[] args) throws IOException {
        String carpeta = "abstracts";
        String patron;

        if (args.length > 0) {
            // Patron recibido como argumento de linea de comandos
            patron = args[0];
        } else {
            // Entrada interactiva por consola
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
        List<Buscador> algoritmos = List.of(new KMP(), new BoyerMoore(), new RabinKarp());

        System.out.println("\n=== Busqueda de patron: \"" + patron + "\" en " + carpeta + " ===\n");

        for (Buscador b : algoritmos) {
            System.out.println("--- " + b.nombre() + " ---");
            List<ResultadoBusqueda> resultados = corpus.buscarEnCorpus(b, patron, true);
            imprimirResultados(resultados);
            System.out.println();
        }
    }
}
