package com.proyecto.bibliometria;

import java.util.List;

import com.proyecto.bibliometria.corpus.ExtractorPDF;
import com.proyecto.bibliometria.corpus.Preprocesador;

public class Main {

    public static void main(String[] args) throws Exception {
        // Fase 1: extraccion de abstracts (omite los que ya existen)
        ExtractorPDF.procesarCarpeta("Articulos", "abstracts", "crudos");

        // Fase 2: preprocesamiento
        Preprocesador prep = new Preprocesador();
        List<String> tokens = prep.procesarCorpus("abstracts");
    }
}