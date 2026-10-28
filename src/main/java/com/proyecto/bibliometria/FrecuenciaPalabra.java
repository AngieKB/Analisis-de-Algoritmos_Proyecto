package com.proyecto.bibliometria;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrecuenciaPalabra {

    // Recibe los tokens (con repeticiones) y devuelve un arreglo de terminos con su
    // frecuencia
    public static Termino[] contar(List<String> tokens) {
        Map<String, Integer> ids = new HashMap<>(); // Con esto guardamos el id de cada palabra
        List<String> palabras = new ArrayList<>(); // Con esto guardamos la palabra en cada id
        int[] frecuenciaPorId = new int[tokens.size()]; // Con esto guardamos la frecuencia de cada id

        for (String t : tokens) { // Recorremos todos los tokens
            Integer id = ids.get(t); // Obtenemos el id de la palabra
            if (id == null) { // Si no tiene id
                id = palabras.size(); // Le asignamos el id actual
                ids.put(t, id); // Guardamos el id de la palabra
                palabras.add(t); // Guardamos la palabra
            }
            frecuenciaPorId[id]++; // Sumamos la frecuencia del id en una unidad
        }

        // Creamos el arreglo de terminos con la cantidad de palabras unicas
        int tamanio = palabras.size();
        Termino[] entrada = new Termino[tamanio];
        for (int i = 0; i < tamanio; i++) {
            entrada[i] = new Termino(palabras.get(i), frecuenciaPorId[i]);
        }

        return entrada;
    }
}