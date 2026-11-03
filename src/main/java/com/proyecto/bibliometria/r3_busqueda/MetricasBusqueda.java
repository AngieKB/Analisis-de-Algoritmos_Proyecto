package com.proyecto.bibliometria.r3_busqueda;

// Acumulador de metricas para un algoritmo de busqueda de patrones.
// Se instancia por llamada a buscar() y se pasa como parametro
// (sin campos de instancia en los algoritmos) para ser seguro en entornos con peticiones concurrentes.

// Separa explicitamente:
// preprocesamientoNs -> tiempo construyendo estructuras auxiliares (tablas, hashes iniciales)
// busquedaNs -> tiempo recorriendo el texto
// comparaciones -> comparaciones caracter-a-caracter durante la busqueda
// operacionesHash -> evaluaciones y actualizaciones de hash (Rabin-Karp)
// colisionesHash -> Rabin-Karp: ventanas donde el hash coincidio pero los chars no
public class MetricasBusqueda {

    public long preprocesamientoNs = 0;
    public long busquedaNs = 0;
    public long comparaciones = 0;
    public long operacionesHash = 0; // operaciones de calculo/comparacion de hash en Rabin-Karp
    public long colisionesHash = 0;  // significativo solo en Rabin-Karp

    public void reset() {
        preprocesamientoNs = busquedaNs = comparaciones = operacionesHash = colisionesHash = 0;
    }

    @Override
    public String toString() {
        return String.format(
                "pre=%.4f ms  busq=%.4f ms  cmp=%d  hashOps=%d  col=%d",
                preprocesamientoNs / 1_000_000.0,
                busquedaNs / 1_000_000.0,
                comparaciones,
                operacionesHash,
                colisionesHash);
    }
}
