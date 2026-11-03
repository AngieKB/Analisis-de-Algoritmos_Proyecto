package com.proyecto.bibliometria.r3_busqueda;

import java.util.List;

//Contrato comun de los algoritmos de busqueda de patrones.
//Recibe el texto y el patron, y devuelve las posiciones (indices 0-based)
//donde el patron comienza dentro del texto.
//No se permite el uso de indexOf(), contains(), find() ni metodos similares del lenguaje.

public interface Buscador {

    // Nombre legible del algoritmo (para reportes y benchmarks).
    String nombre();

    // @brief -> Busca todas las ocurrencias exactas de {@code patron} dentro de
    // {@code texto}.
    // @param texto -> texto donde se busca (puede ser el abstract completo)
    // @param patron -> cadena a localizar
    // @return -> lista ordenada de indices de inicio de cada ocurrencia; vacia si
    // no hay ninguna
    List<Integer> buscar(String texto, String patron);
}
