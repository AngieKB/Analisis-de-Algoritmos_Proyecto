package com.proyecto.bibliometria.r1_similitud.algo_clasicos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class CosenoTFIDF {

    // CosenoTDIDF: modelo de espacio vectorial, cada abstract es un vector de pesos
    // TF-IDF

    // TF = Term Frequency (Cuanto aparece una palabra)
    // IDF = Inverse Document Frequency (qué tan poco común es una palabra dentro de
    // todos los abstracts.)

    // Similitud = coseno del angulo entre los dos vectores
    // (0 = nada en comun, 1 = misma direccion)
    // Complejidad: Construir el modelo: O(T), T = total de tokens del corpus
    // Comparar dos abstracts: O(m + n), m y n = tokens de cada abstract
    // Los vectores son mapas, solo se guardan las palabras que si aparecen

    // idf.get(palabra) = que tan rara es la palabra en el corpus
    // Entre más bajo el valor, más común es la palabra
    private final Map<String, Double> idf = new HashMap<>();

    // IDF para palabras que no estan en el corpus (df = 0)
    private final double idfDesconocida;

    // Recibe el corpus completo, una lista de tokens por cada abstract
    // Lista interior: Tokens por abstract
    // Lista exterior: Los abstracts
    public CosenoTFIDF(List<List<String>> corpus) {
        int n = corpus.size(); // cantidad de abstracts (20 para los nuestros)

        // df = en cuantos abstracts aparece cada palabra
        Map<String, Integer> df = new HashMap<>();
        for (List<String> documento : corpus) {
            // new HashSet<>(documento) deja cada palabra una sola vez por abstract
            // asi df cuenta abstracts, no apariciones
            for (String palabra : new HashSet<>(documento)) {
                // Si la palabra NO existe, la añade con valor 1
                // Si la palabra existe: le suma 1
                df.merge(palabra, 1, Integer::sum);
            }
        }

        // IDF: ln((1 + Numero de abstracts) / (1 + df)) + 1
        // El +1 de afuera evita que una palabra presente en todos los abstracts quede
        // con peso 0
        // Esto hace que las palabras que aparecen en pocos abstracts tengan un valor
        // idf mayor
        for (Map.Entry<String, Integer> e : df.entrySet()) {
            idf.put(e.getKey(), Math.log((1.0 + n) / (1.0 + e.getValue())) + 1.0);
        }
        idfDesconocida = Math.log(1.0 + n) + 1.0; // caso df = 0
    }

    // Convierte una lista de tokens en un vector: palabra -> TF * IDF
    private Map<String, Double> vectorizar(List<String> tokens) {

        // TF: cuantas veces aparece cada palabra en este abstract
        Map<String, Integer> tf = new HashMap<>();
        for (String t : tokens) {
            // Si la palabra NO existe, la añade con valor 1
            // Si la palabra existe: le suma 1
            tf.merge(t, 1, Integer::sum);
        }

        // Peso de la palabra = TF * IDF
        Map<String, Double> vector = new HashMap<>();
        for (Map.Entry<String, Integer> e : tf.entrySet()) {
            // Si la palabra existe, obtiene su idf
            // Si no existe (es nueva), queda con valor idfDesconocida
            double pesoIdf = idf.getOrDefault(e.getKey(), idfDesconocida);
            vector.put(e.getKey(), e.getValue() * pesoIdf);
        }
        return vector;
    }

    // Similitud coseno entre dos listas de tokens
    public double similitud(List<String> a, List<String> b) {
        Map<String, Double> va = vectorizar(a);
        Map<String, Double> vb = vectorizar(b);

        // Si alguno esta vacio no hay direccion que comparar
        // dos vacios se consideran identicos
        if (va.isEmpty() && vb.isEmpty()) {
            return 1.0;
        }

        // Uno solo vacio = nada en comun
        if (va.isEmpty() || vb.isEmpty()) {
            return 0.0;
        }

        // Producto punto, solo suman las palabras que estan en los DOS vectores
        // (si falta en uno, su peso es 0 y el producto es 0)
        double productoPunto = 0.0;
        for (String palabra : va.keySet()) {
            if (vb.containsKey(palabra)) {
                double pesoA = va.get(palabra);
                double pesoB = vb.get(palabra);

                productoPunto += pesoA * pesoB;
            }
        }
        // Norma de cada vector: raiz de la suma de los pesos al cuadrado
        double normaA = norma(va);
        double normaB = norma(vb);

        // Similitud de coseno: (va · vb) / (||va|| × ||vb||)
        // Math.min es por si al calcularlo, da >1 por decimales
        return Math.min(1.0, productoPunto / (normaA * normaB));
    }

    // Norma (largo) de un vector
    private static double norma(Map<String, Double> vector) {
        double suma = 0.0;
        for (double peso : vector.values()) {
            suma += peso * peso;
        }
        return Math.sqrt(suma);
    }
}