package com.proyecto.bibliometria.r1_similitud.algo_ia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import com.proyecto.bibliometria.corpus.Preprocesador;

public class GenerarGloVeCorpus {

    // Programa que se corre una vez: recorta GloVe a solo las palabras del corpus
    // Entrada: modelos/glove.6B.50d.txt (400.000 palabras)
    // Salida: embeddings/glove_corpus.txt (alrededor de mil. este se sube al github
    // papu)
    // Con esto, quien clone el repo NO debe descargar todo eso de GloVe

    public static void main(String[] args) throws Exception {
        File entrada = new File("modelos/glove.6B.50d.txt");
        File salida = new File("embeddings/glove_corpus.txt");

        if (!entrada.exists()) {
            System.out.println("Falta " + entrada + " (ver paso 2)");
            return;
        }
        new File("embeddings").mkdirs();

        // Vocabulario del corpus = palabras distintas de los 20 abstracts ya
        // tokenizados
        Preprocesador prep = new Preprocesador();
        Set<String> vocabulario = new HashSet<>(prep.procesarCorpus("abstracts"));
        System.out.println("Vocabulario del corpus: " + vocabulario.size());

        int guardadas = 0;
        try (BufferedReader lector = Files.newBufferedReader(entrada.toPath(), StandardCharsets.UTF_8);
                BufferedWriter escritor = Files.newBufferedWriter(salida.toPath(), StandardCharsets.UTF_8)) {

            String linea;
            while ((linea = lector.readLine()) != null) {
                String palabra = linea.substring(0, linea.indexOf(' '));
                if (vocabulario.contains(palabra)) {
                    escritor.write(linea);
                    escritor.newLine();
                    guardadas++;
                }
            }
        }
        System.out.println("Palabras encontradas en GloVe: " + guardadas + " de " + vocabulario.size());
        System.out.println("Guardado en: " + salida);
    }
}