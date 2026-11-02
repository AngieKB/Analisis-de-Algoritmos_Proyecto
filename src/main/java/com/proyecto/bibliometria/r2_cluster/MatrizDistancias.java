package com.proyecto.bibliometria.r2_cluster;

import com.proyecto.bibliometria.corpus.CorpusAbstracts;
import com.proyecto.bibliometria.r1_similitud.algo_clasicos.CosenoTFIDF;

public class MatrizDistancias {

    // Construye la matriz de distancias entre todos los abstracts
    // distancia = 1 - similitud (similitud 1 = identicos, entonces distancia 0)
    // Es simetrica (d[i][j] = d[j][i]) y la diagonal vale 0
    // Complejidad: O(a^2 * n)
    // a = abstracts
    // n = tokens por abstract

    // Decidimos usar el algoritmo de CosenoTFIDF porque mejor separaba los
    // abstratcs

    public static double[][] desdeCosenoTFIDF(CorpusAbstracts corpus) {
        int n = corpus.tamano();
        CosenoTFIDF modelo = new CosenoTFIDF(corpus.tokens);

        double[][] d = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) { // solo la mitad, la otra es simetrica
                double distancia = 1.0 - modelo.similitud(corpus.tokens.get(i), corpus.tokens.get(j));
                distancia = Math.max(0.0, distancia); // evita numeros como -0.0000001 por decimales
                d[i][j] = distancia;
                d[j][i] = distancia;
            }
        }
        return d;
    }
}
