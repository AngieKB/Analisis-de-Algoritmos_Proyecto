package com.proyecto.bibliometria.r1_similitud.algo_clasicos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Jaccard {

    public static Traza trazar(List<String> a, List<String> b) {
        Set<String> conjuntoA = new TreeSet<>(a);
        Set<String> conjuntoB = new TreeSet<>(b);
        Set<String> interseccion = new TreeSet<>(conjuntoA);
        interseccion.retainAll(conjuntoB);
        Set<String> union = new TreeSet<>(conjuntoA);
        union.addAll(conjuntoB);
        double similitud = union.isEmpty() ? 1.0 : (double) interseccion.size() / union.size();
        return new Traza(
                List.copyOf(conjuntoA),
                List.copyOf(conjuntoB),
                List.copyOf(interseccion),
                List.copyOf(union),
                interseccion.size(),
                union.size(),
                similitud);
    }

    // Similitud de Jaccard entre dos listas de tokens
    // Solo importan los tokens que hay
    // Jaccard = |A interseccion B| / |A union B|
    // Resultado: 0 = nada en comun, 1 = mismo conjunto de palabras
    // Complejidad: O(m + n)
    // m = cantidad de tokens de a
    // n = cantidad de tokens de b
    public static double similitud(List<String> a, List<String> b) {

        // Convertir cada lista en conjunto, elimina las palabras repetidas
        Set<String> conjuntoA = new HashSet<>(a);
        Set<String> conjuntoB = new HashSet<>(b);

        // Interseccion: palabras que estan en A y tambien en B
        // retainAll deja solo las que tambien estan en conjuntoB
        Set<String> interseccion = new HashSet<>(conjuntoA);
        interseccion.retainAll(conjuntoB); // La complejidad es O(conjuntoA), depende de los elementos unicos de a

        // Union: palabras que estan en A o en B
        Set<String> union = new HashSet<>(conjuntoA);
        union.addAll(conjuntoB);

        // Dos conjuntos vacios se consideran identicos.
        // Con esto se evita dividir entre 0
        if (union.isEmpty())
            return 1.0;

        return (double) interseccion.size() / union.size();
    }

    public record Traza(
            List<String> setA,
            List<String> setB,
            List<String> intersection,
            List<String> union,
            int intersectionSize,
            int unionSize,
            double similarity) {
    }
}