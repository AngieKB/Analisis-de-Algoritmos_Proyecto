package com.proyecto.bibliometria.r2_cluster;

public class Fusion {

    // Es el paso de agrupamiento
    // Ids: los abstracts originales son 0 .. n-1
    // el grupo que nace en la fusion numero k empezando en 0 tiene id n + k

    public final int clusterA; // id del primer grupo
    public final int clusterB; // id del segundo grupo
    public final double distancia; // distancia a la que se unieron, osea, la altura en el dendrograma
    public final int tamano; // cantidad de abstracts del grupo nuevo

    public Fusion(int clusterA, int clusterB, double distancia, int tamano) {
        this.clusterA = clusterA;
        this.clusterB = clusterB;
        this.distancia = distancia;
        this.tamano = tamano;
    }

    @Override
    public String toString() {
        return clusterA + " + " + clusterB + " a distancia " + String.format("%.4f", distancia)
                + " (tamano " + tamano + ")";
    }
}