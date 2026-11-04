package com.proyecto.bibliometria.r2_cluster;

import java.util.ArrayList;
import java.util.List;

public class CoeficienteCofenetico {

    // Metrica de evaluacion Interna: coeficiente de correlacion cofenetico
    // Mide que tan fielmente el arbol conserva las distancias originales
    // Distancia cofenetica c(i,j) = altura de la fusion donde i y j quedan juntos
    // por primera vez
    // Resultado = correlacion de Pearson entre d(i,j) y c(i,j) sobre todos los
    // pares i<j
    // Rango: -1 a 1, y mientras mas cerca de 1, mas fiel es el arbol a los datos
    // Complejidad: O(n^2) (cada par se asigna una vez y luego se recorre una vez)

    // Construye la matriz de distancias cofeneticas a partir de la lista de
    // fusiones
    public static double[][] matrizCofenetica(List<Fusion> fusiones, int n) {
        double[][] c = new double[n][n];

        // miembros.get(id) = abstracts que contiene el grupo con ese id
        // Ids 0..n-1 son abstracts sueltos, y el grupo de la fusion k tiene id n+k
        // Como se agregan en orden, el indice de la lista coincide con el id
        List<List<Integer>> miembros = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<Integer> solo = new ArrayList<>();
            solo.add(i);
            miembros.add(solo);
        }

        for (Fusion f : fusiones) {
            List<Integer> a = miembros.get(f.clusterA);
            List<Integer> b = miembros.get(f.clusterB);

            // Todo par con un abstract de A y uno de B se junta por primera vez aqui,
            // asi que su distancia cofenetica es la altura de esta fusion
            for (int x : a) {
                for (int y : b) {
                    c[x][y] = f.distancia;
                    c[y][x] = f.distancia;
                }
            }

            // El grupo nuevo contiene a los miembros de A y de B
            List<Integer> union = new ArrayList<>(a);
            union.addAll(b);
            miembros.add(union);
        }
        return c;
    }

    // Correlacion de Pearson entre las distancias originales y las cofeneticas
    // Pearson = covarianza(D, C) / (desviacion(D) * desviacion(C))
    public static double calcular(double[][] d, List<Fusion> fusiones) {
        return trazar(d, fusiones).coefficient();
    }

    public static Traza trazar(double[][] d, List<Fusion> fusiones) {
        int n = d.length;
        double[][] c = matrizCofenetica(fusiones, n);

        // Solo los pares i<j (la matriz es simetrica y la diagonal vale 0)
        int pares = 0;
        double sumaD = 0.0;
        double sumaC = 0.0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                sumaD += d[i][j];
                sumaC += c[i][j];
                pares++;
            }
        }
        if (pares == 0) {
            return new Traza(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, c, List.of());
        }
        double mediaD = sumaD / pares;
        double mediaC = sumaC / pares;

        // covarianza y varianzas (sin dividir entre pares: se cancela en el cociente)
        double covarianza = 0.0;
        double varianzaD = 0.0;
        double varianzaC = 0.0;
        List<ParCofenetico> detalles = new ArrayList<>(pares);
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double desvD = d[i][j] - mediaD;
                double desvC = c[i][j] - mediaC;
                double contribucion = desvD * desvC;
                covarianza += contribucion;
                varianzaD += desvD * desvD;
                varianzaC += desvC * desvC;
                detalles.add(new ParCofenetico(i, j, d[i][j], c[i][j], desvD, desvC, contribucion));
            }
        }

        // Si todas las distancias son iguales no hay variacion que correlacionar
        if (varianzaD == 0.0 || varianzaC == 0.0) {
            return new Traza(pares, mediaD, mediaC, covarianza, varianzaD, varianzaC, 0.0, c,
                    List.copyOf(detalles));
        }

        double coeficiente = covarianza / Math.sqrt(varianzaD * varianzaC);
        return new Traza(pares, mediaD, mediaC, covarianza, varianzaD, varianzaC, coeficiente, c,
                List.copyOf(detalles));
    }

    public record Traza(
            int pairCount,
            double meanOriginalDistance,
            double meanCopheneticDistance,
            double covarianceSum,
            double originalVarianceSum,
            double copheneticVarianceSum,
            double coefficient,
            double[][] copheneticMatrix,
            List<ParCofenetico> pairs) {
    }

    public record ParCofenetico(
            int articleAIndex,
            int articleBIndex,
            double originalDistance,
            double copheneticDistance,
            double centeredOriginalDistance,
            double centeredCopheneticDistance,
            double covarianceContribution) {
    }
}