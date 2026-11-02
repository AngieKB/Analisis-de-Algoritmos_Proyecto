package r2_cluster_test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.proyecto.bibliometria.corpus.CorpusAbstracts;
import com.proyecto.bibliometria.r2_cluster.Fusion;
import com.proyecto.bibliometria.r2_cluster.MatrizDistancias;
import com.proyecto.bibliometria.r2_cluster.SingleLinkage;

public class TestSingleLinkage {

    // Caso pequeno con respuesta conocida
    // Fusion 1: {0,1} a distancia 1
    // Fusion 2: {0,1} con 3 a distancia min(3, 2) = 2
    // Fusion 3: {0,1,3} con 8 a distancia min(8, 7, 5) = 5
    @Test
    void casoPequeno() {
        double[][] d = {
                { 0, 1, 3, 8 },
                { 1, 0, 2, 7 },
                { 3, 2, 0, 5 },
                { 8, 7, 5, 0 }
        };
        List<Fusion> f = SingleLinkage.agrupar(d);

        assertEquals(3, f.size()); // n-1 fusiones

        assertEquals(1.0, f.get(0).distancia);
        assertEquals(2, f.get(0).tamano);

        assertEquals(2.0, f.get(1).distancia);
        assertEquals(3, f.get(1).tamano);

        assertEquals(5.0, f.get(2).distancia);
        assertEquals(4, f.get(2).tamano);
    }

    // Con los 20 abstracts reales: 19 fusiones, alturas que nunca bajan
    @Test
    void abstractsReales() throws Exception {
        CorpusAbstracts corpus = new CorpusAbstracts("abstracts"); // Carga los abstracts
        double[][] d = MatrizDistancias.desdeCosenoTFIDF(corpus); // Usa CosenoTFIDF para la matriz de distancias

        List<Fusion> fusiones = SingleLinkage.agrupar(d); // Usa el SingleLinkage

        // Debe haber 19 fusiones
        assertEquals(corpus.tamano() - 1, fusiones.size());

        // En single linkage las distancias de fusion son ascendentes, ya que siempre se
        // van agrupando por menor distancia
        for (int i = 1; i < fusiones.size(); i++) {
            assertTrue(fusiones.get(i).distancia >= fusiones.get(i - 1).distancia);
        }
        // La ultima fusion debe contener a los 20 abstracts
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