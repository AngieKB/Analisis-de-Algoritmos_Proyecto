package com.proyecto.bibliometria.r2_cluster;

import java.util.ArrayList;
import java.util.List;

public class CompleteLinkage {

    // Agrupamiento jerarquico aglomerativo con enlace completo (Complete linkage)
    // Distancia entre dos grupos = distancia entre sus dos miembros mas lejanos
    // Complejidad: n-1 fusiones, y en cada una se busca el par mas lejano O(n^2)
    // y se actualiza una fila O(n), entonces el total es O(n^3)

    // Recibe la matriz de distancias CosenoTFIDF entre todos los abstracts
    // Devuelve la lista de fusiones
    public static List<Fusion> agrupar(double[][] distancias) {
        int n = distancias.length;

        // Copia de la matriz para no modificar la original
        double[][] distanciasCopia = new double[n][n];
        for (int i = 0; i < n; i++) {
            distanciasCopia[i] = distancias[i].clone();
        }

        // Cada fila de la matriz representa un grupo
        // idGrupo[i] = id del grupo que representa la fila i
        // al inicio, cada abstract es su propio grupo porque no se han fusionado
        // tamano[i] = cuantos abstracts tiene ese grupo
        // al inicio, cada grupo tiene 1 abstract
        // activo[i] = false si la fila ya fue absorbida por otra
        // al inicio, todas estan activas, porque no se han fusionado
        int[] idGrupo = new int[n];
        int[] tamano = new int[n];
        boolean[] activo = new boolean[n];
        for (int i = 0; i < n; i++) {
            idGrupo[i] = i;
            tamano[i] = 1;
            activo[i] = true;
        }

        List<Fusion> fusiones = new ArrayList<>();

        // n abstracts necesitan n-1 fusiones para quedar en un solo grupo
        for (int paso = 0; paso < n - 1; paso++) {

            // 1) Buscar el par de grupos vivos mas cercano (i < j)
            // Si hay empate, gana el primero que se encuentra
            int mejorI = -1; // identifica primer grupo
            int mejorJ = -1; // identifica segundo grupo
            double minimo = Double.MAX_VALUE; // guarda la menor distancia encontrada
            for (int i = 0; i < n; i++) {
                if (!activo[i]) // Si no está activo el grupo, pasa a la siguiente iteración
                    continue;

                // Inicia en i + 1 para no repetir comparaciones, ni con el mismo
                for (int j = i + 1; j < n; j++) {
                    if (activo[j] && distanciasCopia[i][j] < minimo) {
                        minimo = distanciasCopia[i][j];
                        mejorI = i;
                        mejorJ = j;
                    }
                }
            }

            // 2) Guardar la fusion con los ids antes de actualizarlos
            fusiones.add(new Fusion(idGrupo[mejorI], idGrupo[mejorJ], minimo, tamano[mejorI] + tamano[mejorJ]));

            // 3) Actualizar distancias: el grupo nuevo vive en la fila mejorI
            // Complete linkage = distancia al grupo k = la mayor de las dos que tenia
            for (int k = 0; k < n; k++) {
                // Si no está activo el grupo, o si el grupo es mejorI o mejorJ, salta a la
                // siguiente Iteracion
                if (!activo[k] || k == mejorI || k == mejorJ)
                    continue;

                // Esta linea define el Complete Linkage
                // selecciona la mayor distancia entre (mejorI y k) o (mejorJ y k)
                double nueva = Math.max(distanciasCopia[mejorI][k], distanciasCopia[mejorJ][k]);

                distanciasCopia[mejorI][k] = nueva;
                distanciasCopia[k][mejorI] = nueva;
            }

            // 4) La fila mejorJ queda absorbida y la fila mejorI pasa a ser el grupo nuevo
            activo[mejorJ] = false; // mejorJ ya fue absorbido, entonces ahora no es un grupo activo
            idGrupo[mejorI] = n + paso; // id nuevo: n, n+1, n+2 y asi
            tamano[mejorI] += tamano[mejorJ]; // el tamaño del nuevo grupo es la suma de los dos grupos fusionados
        }
        return fusiones;
    }
}