package r2_cluster_test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.proyecto.bibliometria.corpus.CorpusAbstracts;
import com.proyecto.bibliometria.r2_cluster.AverageLinkage;
import com.proyecto.bibliometria.r2_cluster.CoeficienteCofenetico;
import com.proyecto.bibliometria.r2_cluster.CompleteLinkage;
import com.proyecto.bibliometria.r2_cluster.MatrizDistancias;
import com.proyecto.bibliometria.r2_cluster.SingleLinkage;

public class TestCoeficienteCofenetico {

    // Con los 20 abstracts reales: compara los tres criterios
    @Test
    void comparaLosTresAlgoritmos() throws Exception {
        CorpusAbstracts corpus = new CorpusAbstracts("abstracts");
        double[][] d = MatrizDistancias.desdeCosenoTFIDF(corpus);

        double single = CoeficienteCofenetico.calcular(d, SingleLinkage.agrupar(d));
        double complete = CoeficienteCofenetico.calcular(d, CompleteLinkage.agrupar(d));
        double average = CoeficienteCofenetico.calcular(d, AverageLinkage.agrupar(d));

        System.out.println("Coeficiente cofenetico Single:   " + single);
        System.out.println("Coeficiente cofenetico Complete: " + complete);
        System.out.println("Coeficiente cofenetico Average:  " + average);

        // Un coeficiente de correlacion siempre esta entre -1 y 1
        for (double v : new double[] { single, complete, average }) {
            assertTrue(v >= -1.0 - 1e-9 && v <= 1.0 + 1e-9);
        }
    }
}