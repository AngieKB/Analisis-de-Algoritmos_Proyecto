package com.proyecto.bibliometria;

/**
 * Contrato comun de los tres algoritmos. Recibe el arreglo SIN modificarlo y
 * devuelve uno nuevo ordenado de MAYOR a MENOR frecuencia (orden estable).
 */
public interface Ordenador {

    String nombre();

    Termino[] ordenar(Termino[] entrada);
}