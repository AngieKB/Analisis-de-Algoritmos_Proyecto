package com.proyecto.bibliometria.r2_cluster;

import java.io.File;
import java.util.List;

import com.proyecto.bibliometria.corpus.CorpusAbstracts;

public class GenerarDendrogramas {

    // Programa que se corre para generar los dendrogramas de los 3 algoritmos
    // Los 3 usan la misma matriz de distancias (coseno TF-IDF), asi la comparacion
    // es justa

    public static void main(String[] args) throws Exception {
        CorpusAbstracts corpus = new CorpusAbstracts("abstracts");
        double[][] distancias = MatrizDistancias.desdeCosenoTFIDF(corpus);
        new File("dendrogramas").mkdirs();

        List<Fusion> single = SingleLinkage.agrupar(distancias);
        Dendrograma.guardarSVG(single, corpus.nombres, "Single Linkage", 1.0,
                new File("dendrogramas/single_linkage.svg"));

        List<Fusion> complete = CompleteLinkage.agrupar(distancias);
        Dendrograma.guardarSVG(complete, corpus.nombres, "Complete Linkage", 1.0,
                new File("dendrogramas/complete_linkage.svg"));

        List<Fusion> average = AverageLinkage.agrupar(distancias);
        Dendrograma.guardarSVG(average, corpus.nombres, "Average Linkage", 1.0,
                new File("dendrogramas/average_linkage.svg"));

        System.out.println("Dendrogramas generados en la carpeta dendrogramas/");
    }
}