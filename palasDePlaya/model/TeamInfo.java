package palasDePlaya.model;

import java.util.List;

public class TeamInfo {
    public final String nombreEquipo;
    public final List<String> jugadores; // nombres agentes jugador
    public int puntos = 0;
    public int partidosJugados = 0;
    public double habilidadBase;

    public TeamInfo(String nombreEquipo, List<String> jugadores, double habilidadBase) {
        this.nombreEquipo = nombreEquipo;
        this.jugadores = jugadores;
        this.habilidadBase = habilidadBase;
    }

    @Override
    public String toString(){
        return nombreEquipo + " " + jugadores;
    }
}