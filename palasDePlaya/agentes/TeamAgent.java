package palasDePlaya.agentes;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.CyclicBehaviour;
import palasDePlaya.utils.TeamPerformance;

public class TeamAgent extends Agent {
    private String teamName;
    private TeamPerformance performance;
    private int partidosJugados = 0;
    private double habilidadBase;

    @Override
    protected void setup() {
        teamName = getLocalName();
        Object[] args = getArguments();
        if (args != null && args.length >= 1) {
            habilidadBase = (double) args[0];
        } else {
            habilidadBase = Math.random(); // Valor aleatorio, debe estar entre 0 y 1.
        }

        performance = new TeamPerformance(habilidadBase, 0.05); // Caída de rendimiento conforme avanzan los partidos.
        System.out.println("Equipo " + teamName + " listo con habilidad base: " + habilidadBase);
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    if (msg.getPerformative() == ACLMessage.REQUEST && "PLAY".equals(msg.getContent())) {
                        // Recibimos una solicitud de jugar
                        String rival = msg.getSender().getLocalName().equals(teamName) ? "EquipoDesconocido" : msg.getSender().getLocalName();
                        partidosJugados++;
                        double rendimientoActual = performance.getRendimiento(partidosJugados) + Math.random() * 0.2;
                        double score = rendimientoActual;

                        // Respondemos al organizador con INFORM y el score
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContent("RESULT;" + score + ";" + partidosJugados);
                        send(reply);
                    }
                } else {
                    block();
                }
            }
        });

    }

    @Override
    protected void takeDown() {
        System.out.println("Equipo " + teamName + " se retira del torneo.");
    }
}