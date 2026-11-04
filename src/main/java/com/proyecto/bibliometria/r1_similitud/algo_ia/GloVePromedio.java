package com.proyecto.bibliometria.r1_similitud.algo_ia;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GloVePromedio {

    // Modelo de embeddings Estatico: cada palabra tiene un vector fijo (GloVe
    // preentrenado)
    // El vector de un abstract es el Promedio de los vectores de sus palabras
    // Similitud = coseno entre los dos vectores promedio
    // Complejidad: cargar el archivo O(L), L = lineas del archivo (se hace una vez)
    // vectorizar O(m * d), m = tokens, d = dimension (50)
    // comparar dos vectores O(d)

    // vectores.get(palabra) = vector de d numeros de esa palabra
    private final Map<String, double[]> vectores = new HashMap<>();
    private int dimension = 0;

    // Lee un archivo en formato GloVe y guarda solo las palabras que estan en el
    // vocabulario
    // el archivo completo trae 400.000 palabras
    // Formato de cada linea: palabra n1 n2 n3 ... n50 (separados por espacio)
    public GloVePromedio(File archivo, Set<String> vocabulario) throws IOException {
        try (BufferedReader lector = Files.newBufferedReader(archivo.toPath(), StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = lector.readLine()) != null) {

                // La palabra es todo lo que hay antes del primer espacio
                int primerEspacio = linea.indexOf(' ');
                String palabra = linea.substring(0, primerEspacio);

                // Si la palabra no esta en nuestro corpus, se ignora
                if (!vocabulario.contains(palabra))
                    continue;

                // El resto de la linea son los numeros del vector
                String[] partes = linea.substring(primerEspacio + 1).split(" ");
                double[] vector = new double[partes.length];
                for (int i = 0; i < partes.length; i++) {
                    vector[i] = Double.parseDouble(partes[i]);
                }
                vectores.put(palabra, vector);
                dimension = vector.length;
            }
        }
    }

    // Cantidad de palabras del corpus que si se encontraron en GloVe
    public int palabrasConocidas() {
        return vectores.size();
    }

    // Convierte una lista de tokens en un vector: el promedio de los vectores de
    // sus palabras
    // Las palabras que GloVe no conoce se ignoran
    public double[] vectorizar(List<String> tokens) {
        double[] suma = new double[dimension];
        int conocidas = 0; // cuantas palabras tenian vector

        for (String t : tokens) {
            double[] v = vectores.get(t);
            if (v == null)
                continue; // palabra desconocida, se salta
            for (int i = 0; i < dimension; i++) {
                suma[i] += v[i];
            }
            conocidas++;
        }

        // Dividir la suma entre la cantidad de palabras = promedio
        if (conocidas > 0) {
            for (int i = 0; i < dimension; i++) {
                suma[i] /= conocidas;
            }
        }
        return suma; // si no hubo ninguna palabra conocida, queda el vector de ceros
    }

    // Similitud coseno entre los vectores promedio de dos abstracts
    // Rango: -1 a 1
    public double similitud(List<String> a, List<String> b) {
        return coseno(vectorizar(a), vectorizar(b));
    }

    public DetalleCoseno trazar(List<String> tokensA, List<String> tokensB) {
        DetalleVector vectorA = trazarVector(tokensA);
        DetalleVector vectorB = trazarVector(tokensB);
        List<ComponenteCoseno> componentes = new ArrayList<>(dimension);
        double productoPunto = 0.0;
        double normaCuadradaA = 0.0;
        double normaCuadradaB = 0.0;
        for (int i = 0; i < dimension; i++) {
            double a = vectorA.averageVector().get(i);
            double b = vectorB.averageVector().get(i);
            double producto = a * b;
            productoPunto += producto;
            normaCuadradaA += a * a;
            normaCuadradaB += b * b;
            componentes.add(new ComponenteCoseno(i, a, b, producto));
        }

        double normaA = Math.sqrt(normaCuadradaA);
        double normaB = Math.sqrt(normaCuadradaB);
        double denominador = normaA * normaB;
        double similitud = normaCuadradaA == 0.0 || normaCuadradaB == 0.0
                ? 0.0
                : Math.min(1.0, productoPunto / denominador);
        return new DetalleCoseno(
                dimension,
                "Cada palabra conocida tiene un vector GloVe; el vector del abstract es el promedio componente a componente.",
                vectorA,
                vectorB,
                componentes,
                productoPunto,
                normaCuadradaA,
                normaCuadradaB,
                normaA,
                normaB,
                denominador,
                similitud);
    }

    private DetalleVector trazarVector(List<String> tokens) {
        List<VectorPalabra> vectoresPalabra = new ArrayList<>();
        List<String> desconocidas = new ArrayList<>();
        double[] suma = new double[dimension];
        for (String token : tokens) {
            double[] vector = vectores.get(token);
            if (vector == null) {
                desconocidas.add(token);
                continue;
            }
            List<Double> componentes = new ArrayList<>(dimension);
            for (int i = 0; i < dimension; i++) {
                suma[i] += vector[i];
                componentes.add(vector[i]);
            }
            vectoresPalabra.add(new VectorPalabra(token, List.copyOf(componentes)));
        }

        List<Double> promedio = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            promedio.add(vectoresPalabra.isEmpty() ? 0.0 : suma[i] / vectoresPalabra.size());
        }
        return new DetalleVector(
                List.copyOf(vectoresPalabra),
                List.copyOf(desconocidas),
                List.copyOf(promedio),
                vectoresPalabra.size());
    }

    // coseno = (A . B) / (|A| * |B|)
    private static double coseno(double[] a, double[] b) {
        double productoPunto = 0.0;
        double normaA = 0.0;
        double normaB = 0.0;
        for (int i = 0; i < a.length; i++) {
            productoPunto += a[i] * b[i];
            normaA += a[i] * a[i];
            normaB += b[i] * b[i];
        }

        // Un vector de ceros no tiene direccion: se considera sin parecido
        if (normaA == 0.0 || normaB == 0.0)
            return 0.0;

        return Math.min(1.0, productoPunto / (Math.sqrt(normaA) * Math.sqrt(normaB)));
    }

    public record DetalleCoseno(
            int dimensions,
            String averagingFormula,
            DetalleVector vectorA,
            DetalleVector vectorB,
            List<ComponenteCoseno> components,
            double dotProduct,
            double normSquaredA,
            double normSquaredB,
            double normA,
            double normB,
            double denominator,
            double similarity) {
    }

    public record DetalleVector(
            List<VectorPalabra> knownWordVectors,
            List<String> unknownTokens,
            List<Double> averageVector,
            int knownTokenCount) {
    }

    public record VectorPalabra(String token, List<Double> vector) {
    }

    public record ComponenteCoseno(int index, double valueA, double valueB, double product) {
    }
}