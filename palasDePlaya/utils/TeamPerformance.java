package palasDePlaya.utils;

public class TeamPerformance {
    private final double habilidadBase; //
    private final double caidaRendimiento; // Conforme vaya jugando más partidos su rendimiento bajará y

    public TeamPerformance(double habilidadBase, double caidaRendimiento) {
        this.habilidadBase = habilidadBase;
        this.caidaRendimiento = caidaRendimiento;
    }

    /**
     * Rendimiento teórico del equipo en función de los partidos jugados.
     * Función exponencial decreciente: habilidadBase * e^(-caidaRendimiento * partidosJugados^1.5)
     * El exponente 1.5 hace la disminuciónmedida que se juegan no lineal, se va acelerando a medida que juegan más partidos.
     */
    public double getRendimiento(int partidosJugados) {
        double  exponente = Math.pow(partidosJugados, 1.5);
        return habilidadBase * Math.exp(-caidaRendimiento * exponente);
    }



}