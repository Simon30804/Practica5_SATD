package palasDePlaya.agentes;

import jade.core.Agent;

public class PlayerAgent extends Agent {
    private String nombreJugador;

    @Override
    protected void setup() {
        nombreJugador = getLocalName();
        System.out.println("Jugador " + nombreJugador + " listo para jugar!");
    }

    @Override
    protected void takeDown() {
        System.out.println("Jugador " + nombreJugador + " se retira del juego.");
    }
}