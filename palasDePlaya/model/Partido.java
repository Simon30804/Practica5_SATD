package palasDePlaya.model;

public class Partido {
    public final String equipoA;
    public final String equipoB;
    public int puntosA;
    public int puntosB;
    public String ganador; // nombre del equipo ganador

    public Partido(String equipoA, String equipoB) {
        this.equipoA = equipoA;
        this.equipoB = equipoB;
        this.puntosA = 0;
        this.puntosB = 0;
        this.ganador = null;
    }

    @Override
    public String toString() {
        if (ganador == null) {
            return equipoA + " vs " + equipoB + " (pendiente)";
        } else {
            return equipoA + " " + puntosA + " - " + puntosB + " " + equipoB + " (ganador: " + ganador + ")";
        }
    }
}