package r2_cluster_test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.proyecto.bibliometria.corpus.CorpusAbstracts;
import com.proyecto.bibliometria.r2_cluster.CompleteLinkage;
import com.proyecto.bibliometria.r2_cluster.Fusion;
import com.proyecto.bibliometria.r2_cluster.MatrizDistancias;

public class TestCompleteLinkage {

    // Caso pequeno con respuesta conocida: puntos en una recta en 0, 1, 3 y 8
    // Fusion 1: {0,1} a distancia 1
    // Fusion 2: {0,1} con 3 a distancia max(3, 2) = 3
    // Fusion 3: {0,1,3} con 8 a distancia max(8, 7, 5) = 8
    @Test
    void casoPequenoConocido() {
        double[][] d = {
                { 0, 1, 3, 8 },
                { 1, 0, 2, 7 },
                { 3, 2, 0, 5 },
                { 8, 7, 5, 0 }
        };
        List<Fusion> f = CompleteLinkage.agrupar(d);

        assertEquals(3, f.size()); // n-1 fusiones

        assertEquals(1.0, f.get(0).distancia, 1e-9);
        assertEquals(2, f.get(0).tamano);

        assertEquals(3.0, f.get(1).distancia, 1e-9);
        assertEquals(3, f.get(1).tamano);

        assertEquals(8.0, f.get(2).distancia, 1e-9);
        assertEquals(4, f.get(2).tamano);
    }

    // Con los 20 abstracts reales: 19 fusiones, alturas que nunca bajan
    @Test
    void abstractsReales() throws Exception {
        CorpusAbstracts corpus = new CorpusAbstracts("abstracts");
        double[][] d = MatrizDistancias.desdeCosenoTFIDF(corpus);

        List<Fusion> fusiones = CompleteLinkage.agrupar(d);
        assertEquals(corpus.tamano() - 1, fusiones.size());

        // En complete linkage las distancias de fusion son no decrecientes
        for (int i = 1; i < fusiones.size(); i++) {
            assertTrue(fusiones.get(i).distancia >= fusiones.get(i - 1).distancia - 1e-9);
        }

        // La ultima fusion contiene a los 20 abstracts
        Fusion ultima = fusiones.get(fusiones.size() - 1);
        assertEquals(corpus.tamano(), ultima.tamano);

        // Propiedad de complete linkage: la ultima fusion ocurre a la distancia
        // del par de abstracts mas lejanos de todo el corpus
        double maximo = 0;
        for (int i = 0; i < d.length; i++) {
            for (int j = i + 1; j < d.length; j++) {
                maximo = Math.max(maximo, d[i][j]);
            }
        }
        assertEquals(maximo, ultima.distancia, 1e-9);

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