package com.proyecto.bibliometria.r1_similitud.algo_ia;

import org.deeplearning4j.text.sentenceiterator.CollectionSentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

import java.util.List;
import java.util.stream.Collectors;

// En esta clase se usa Word2Vec de Deeplearning4j para representar palabras con vectores.
// Para comparar dos abstracts, se promedian los vectores de sus palabras y luego
// se calcula la similitud coseno entre los vectores resultantes.
// El modelo se entrena con los abstracts del proyecto para aprender el vocabulario
// específico de inteligencia artificial en educación.
public class Word2Vec {

    // Se almacena el modelo entrenado.
    private final org.deeplearning4j.models.word2vec.Word2Vec modelo;

    // Se conserva la dimensión para crear vectores de documento del mismo tamaño.
    private final int dimension;

    // Se recibe el corpus preprocesado, donde cada lista contiene los tokens de un abstract.
    // La dimensión indica cuántos valores tendrá el vector aprendido para cada palabra.
    public Word2Vec(List<List<String>> corpus, int dimension) {
        this.dimension = dimension;

        // Se unen los tokens de cada abstract porque DL4J espera recibir oraciones de texto.
        List<String> oraciones = corpus.stream()
                .map(tokens -> String.join(" ", tokens))
                .collect(Collectors.toList());

        // El iterador permite recorrer las oraciones del corpus durante el entrenamiento.
        CollectionSentenceIterator iterador = new CollectionSentenceIterator(oraciones);

        // Se configura cómo se separa el texto en palabras durante el entrenamiento.
        TokenizerFactory fabrica = new DefaultTokenizerFactory();

        // Se define el tamaño de los vectores, el contexto y las iteraciones de aprendizaje.
        // Se usa frecuencia mínima 1 para conservar palabras que aparecen pocas veces
        // en este corpus pequeño, y una semilla fija para repetir el entrenamiento.
        this.modelo = new org.deeplearning4j.models.word2vec.Word2Vec.Builder()
                .layerSize(dimension)
                .windowSize(5)
                .minWordFrequency(1)
                .iterations(15)
                .seed(42)
                .iterate(iterador)
                .tokenizerFactory(fabrica)
                .build();

        // Se entrena el modelo para que las palabras usadas en contextos similares
        // queden representadas por vectores cercanos.
        this.modelo.fit();
    }

    // El abstract se representa promediando los vectores de las palabras que conoce el modelo.
    // Si no se conoce ninguna, el vector queda en cero.
    public double[] vectorizar(List<String> tokens) {
        double[] suma = new double[dimension];
        int conocidas = 0;

        for (String token : tokens) {
            // Solo se pueden sumar palabras que estén en el vocabulario aprendido.
            if (!modelo.hasWord(token)) continue;

            // Se suma el vector de esta palabra al vector que representará el documento.
            double[] v = modelo.getWordVector(token);
            for (int i = 0; i < dimension; i++) {
                suma[i] += v[i];
            }
            conocidas++;
        }

        // Se divide por la cantidad de palabras conocidas para obtener el promedio.
        if (conocidas > 0) {
            for (int i = 0; i < dimension; i++) {
                suma[i] /= conocidas;
            }
        }
        // Si no hubo palabras conocidas, se devuelve el vector cero inicializado arriba.
        return suma;
    }

    // Se vectorizan los dos abstracts y se comparan sus vectores con la similitud coseno.
    public double similitud(List<String> a, List<String> b) {
        return coseno(vectorizar(a), vectorizar(b));
    }

    // Se comparan las direcciones de dos vectores: cuanto más se parecen, más se acerca el resultado a 1.
    // Si alguno es cero, no tiene una dirección comparable y se devuelve 0.
    private static double coseno(double[] a, double[] b) {
        double productoPunto = 0.0;
        double normaA = 0.0;
        double normaB = 0.0;

        for (int i = 0; i < a.length; i++) {
            productoPunto += a[i] * b[i];
            normaA += a[i] * a[i];
            normaB += b[i] * b[i];
        }

        // Se evita dividir por cero cuando alguno de los vectores no tiene magnitud.
        if (normaA == 0.0 || normaB == 0.0) return 0.0;

        // Se aplica la fórmula del coseno y se limita el resultado máximo a 1.
        return Math.min(1.0, productoPunto / (Math.sqrt(normaA) * Math.sqrt(normaB)));
    }
}
