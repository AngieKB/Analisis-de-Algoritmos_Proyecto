package com.proyecto.bibliometria.r3_busqueda;

import java.util.List;

// Contrato comun de los algoritmos de busqueda de patrones.

// La firma principal recibe un objeto {@link MetricasBusqueda} donde el algoritmo acumula tiempos y contadores.
// Pasarlo como parametro (en vez de guardarlo como campo de instancia) 
// hace que las implementaciones sean stateless y seguras en entornos concurrentes.

// El metodo de conveniencia sin metricas crea internamente un MetricasBusqueda descartable 
// para no romper el codigo existente.
public interface Buscador {

    // Nombre legible del algoritmo (para reportes y benchmarks).
    String nombre();

    // Busca todas las ocurrencias exactas de {@code patron} en {@code texto}
    // acumulando metricas en {@code m}.

    // @param texto -> texto donde se busca
    // @param patron -> cadena a localizar
    // @param m -> acumulador de metricas (modificado por el metodo)
    // @return -> lista ordenada de indices de inicio; vacia si no hay ninguna
    List<Integer> buscar(String texto, String patron, MetricasBusqueda m);

    // Sobrecarga de conveniencia: crea un MetricasBusqueda descartable.
    // Util en tests donde no se necesitan las metricas.
    default List<Integer> buscar(String texto, String patron) {
        return buscar(texto, patron, new MetricasBusqueda());
    }
}
