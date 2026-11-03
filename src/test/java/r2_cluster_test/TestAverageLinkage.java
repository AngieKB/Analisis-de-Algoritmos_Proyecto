package r2_cluster_test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.proyecto.bibliometria.corpus.CorpusAbstracts;
import com.proyecto.bibliometria.r2_cluster.AverageLinkage;
import com.proyecto.bibliometria.r2_cluster.Fusion;
import com.proyecto.bibliometria.r2_cluster.MatrizDistancias;

public class TestAverageLinkage {

    // Caso pequeno con respuesta conocida: puntos en una recta en 0, 1, 3 y 8
    // Fusion 1: {0,1} a distancia 1
    // Despues de fusionar {0,1} (cada grupo de tamano 1):
    // distancia a 3 = (1*3 + 1*2) / 2 = 2.5
    // distancia a 8 = (1*8 + 1*7) / 2 = 7.5
    // Fusion 2: {0,1} con 3 a distancia 2.5
    // Fusion 3: {0,1,3} con 8 a distancia (2*7.5 + 1*5) / 3 = 6.6667
    @Test
    void casoPequenoConocido() {
        double[][] d = {
                { 0, 1, 3, 8 },
                { 1, 0, 2, 7 },
                { 3, 2, 0, 5 },
                { 8, 7, 5, 0 }
        };
        List<Fusion> f = AverageLinkage.agrupar(d);

        assertEquals(3, f.size()); // n-1 fusiones

        assertEquals(1.0, f.get(0).distancia, 1e-9);
        assertEquals(2, f.get(0).tamano);

        assertEquals(2.5, f.get(1).distancia, 1e-9);
        assertEquals(3, f.get(1).tamano);

        assertEquals(20.0 / 3.0, f.get(2).distancia, 1e-9);
        assertEquals(4, f.get(2).tamano);
    }

    // Verifica que el promedio se pondera por el tamano de cada grupo
    // Con el promedio simple (a + b) / 2 la ultima fusion daria (7.5 + 5) / 2 =
    // 6.25
    // Con el promedio ponderado (UPGMA) da 6.6667, que es el promedio de los 3
    // pares reales:
    // (d(0,8) + d(1,8) + d(3,8)) / 3 = (8 + 7 + 5) / 3
    @Test
    void promedioPonderadoPorTamano() {
        double[][] d = {
                { 0, 1, 3, 8 },
                { 1, 0, 2, 7 },
                { 3, 2, 0, 5 },
                { 8, 7, 5, 0 }
        };
        List<Fusion> f = AverageLinkage.agrupar(d);

        double ultima = f.get(2).distancia;
        assertNotEquals(6.25, ultima, 1e-3); // no es el promedio simple
        assertEquals((8.0 + 7.0 + 5.0) / 3.0, ultima, 1e-9); // es el promedio de todos los pares
    }

    // Con los 20 abstracts reales: 19 fusiones, alturas que nunca bajan
    @Test
    void abstractsReales() throws Exception {
        CorpusAbstracts corpus = new CorpusAbstracts("abstracts");
        double[][] d = MatrizDistancias.desdeCosenoTFIDF(corpus);

        List<Fusion> fusiones = AverageLinkage.agrupar(d);
        assertEquals(corpus.tamano() - 1, fusiones.size());

        // En average linkage (UPGMA) las distancias de fusion son no decrecientes
        for (int i = 1; i < fusiones.size(); i++) {
            assertTrue(fusiones.get(i).distancia >= fusiones.get(i - 1).distancia - 1e-9);
        }

        // La ultima fusion contiene a los 20 abstracts
        assertEquals(corpus.tamano(), fusiones.get(fusiones.size() - 1).tamano);

        // Imprimir el arbol con nombres (ids >= n son grupos ya formados)
        int n = corpus.tamano();
        for (int k = 0; k < fusiones.size(); k++) {
            Fusion f = fusiones.get(k);
            System.out.println("Grupo " + (n + k) + " = " + etiqueta(f.clusterA, corpus)
                    + " + " + etiqueta(f.clusterB, corpus)
                    + "  | distancia " + String.format("%.4f", f.distancia));
        }
    }

    // Abstract original: su numero. Grupo ya formado: "G" + id
    private static String etiqueta(int id, CorpusAbstracts corpus) {
        if (id < corpus.tamano()) {
            return corpus.nombres.get(id).substring(0, corpus.nombres.get(id).indexOf('.'));
        }
        return "G" + id;
    }
}