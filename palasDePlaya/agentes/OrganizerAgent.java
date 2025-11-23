package palasDePlaya.agentes;

import jade.core.Agent;
import jade.wrapper.ContainerController;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import palasDePlaya.model.TeamInfo;
import palasDePlaya.model.Partido;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class OrganizerAgent extends Agent {
    private JFrame frame;
    private DefaultListModel<String> partidosListModel;
    private DefaultListModel<String> rankingModel;
    private JButton siguienteBoton;
    private Map<String, TeamInfo> equipos = new HashMap<>();
    private List<Partido> rondaActualPartido = new ArrayList<>();
    private ContainerController container;
    private volatile boolean guiListoParaSiguiente = false;

    @Override
    protected void setup() {
        System.out.println("Organizador " + getLocalName() + " listo.");

        // Inicializar el GUI en el hilo EDT
        SwingUtilities.invokeLater(this::createAndShowGUI);

        // Obtengo el controlador del contenedor para crear agentes de manera dinámica
        container = getContainerController();

        // Primero: Creo los jugadores y los equipos
        int nPlayers = 12 + new Random().nextInt(11); // Entre 12 y 22 jugadores
        System.out.println("Organizador: cuenta jugadores: " + nPlayers + " jugadores.");

        // Creamos los PlayerAgents
        List<String> nombresJugadores = new ArrayList<>();
        try {
            for (int i = 1; i <= nPlayers; i++) {
                String nombreJugador = "Jugador" + i;
                nombresJugadores.add(nombreJugador);
                AgentController playerAgent = container.createNewAgent(nombreJugador, "palasDePlaya.agentes.PlayerAgent", null);
                playerAgent.start();
            }
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }

        // Creamos los equipos de 2 jugadores cada uno de manera aleatoria
        Collections.shuffle(nombresJugadores);
        List<List<String>> parejas = new ArrayList<>();
        for (int i = 0; i + 1 < nombresJugadores.size(); i += 2) {
            parejas.add(Arrays.asList(nombresJugadores.get(i), nombresJugadores.get(i + 1)));
        }

        // Si hay un jugador impar, lo dejamos sin equipo

        //Creamos los TeamAgents con habilidad base aleatoria
        try {
            int idx = 1;
            for (List<String> pair : parejas) {
                String nombreEquipo = "Equipo" + idx++;
                double habilidadBase = 0.4 + Math.random() * 0.6; // Habilidad base entre 0.5 y 1.0
                Object[] args = new Object[]{habilidadBase};
                AgentController teamAgentController = container.createNewAgent(nombreEquipo, "palasDePlaya.agentes.TeamAgent", args);
                teamAgentController.start();
                TeamInfo teamInfo = new TeamInfo(nombreEquipo, pair, habilidadBase);
                equipos.put(nombreEquipo, teamInfo);
            }
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }

        // Iniciamos la primera ronda
        List<String> teamNames = new ArrayList<>(equipos.keySet());
        List<Partido> todosPartidos = new ArrayList<>();
        for (int i = 0; i < teamNames.size(); i++) {
            for (int j = i + 1; j < teamNames.size(); j++) {
                todosPartidos.add(new Partido(teamNames.get(i), teamNames.get(j)));
            }
        }

        // Divimos los partidos en rondas
        int partidosPorRonda = Math.max(1, teamNames.size() / 2);
        List<List<Partido>> rondas = new ArrayList<>();
        for (int i = 0; i < todosPartidos.size(); i += partidosPorRonda) {
            rondas.add(todosPartidos.subList(i, Math.min(i + partidosPorRonda, todosPartidos.size())));
        }

        // Añadimos el comportamiento para gestionar las rondas de manera secuencial, esperamos las pulsaciones desde la GUI.
        addBehaviour(new SimpleBehaviour(this) {
            private int roundIndex = 0;
            private boolean waitingForGUI = true;
            private boolean finished = false;

            @Override
            public void action() {
                if (roundIndex >= rondas.size()) {
                    if (!finished) {
                        showFinalScreen();
                        finished = true;
                    }
                    block();
                    return;
                }
                if (waitingForGUI) {
                    // mostrar la ronda actual en la GUI y esperar a que el usuario pulse "Iniciar Ronda"
                    rondaActualPartido = new ArrayList<>(rondas.get(roundIndex));
                    SwingUtilities.invokeLater(() -> {
                        partidosListModel.clear();
                        for (Partido partido : rondaActualPartido) {
                            partidosListModel.addElement(partido.toString());
                        }
                    });
                    // Esperar hasta que la GUI indique que podemos seguir
                    if (!guiListoParaSiguiente) {
                        block(500);
                        return;
                    }
                    guiListoParaSiguiente = false;
                    waitingForGUI = false;
                } else {
                    // Ejecutar la ronda:
                    for (Partido partido : rondaActualPartido) {
                        // Enviar mensajes request con content "PLAY" a los equipos para jugar el partido
                        ACLMessage playRequestA = new ACLMessage(ACLMessage.REQUEST);
                        playRequestA.addReceiver(new AID(partido.equipoA, AID.ISLOCALNAME));
                        playRequestA.setContent("PLAY");
                        send(playRequestA);

                        ACLMessage playRequestB = new ACLMessage(ACLMessage.REQUEST);
                        playRequestB.addReceiver(new AID(partido.equipoB, AID.ISLOCALNAME));
                        playRequestB.setContent("PLAY");
                        send(playRequestB);
                    }

                    // Esperar las respuestas de los equipos y actualizar los resultados
                    Map<String, Double> resultados = new HashMap<>();
                    int respuestasEsperadas = rondaActualPartido.size() * 2;
                    int respuestasRecibidas = 0;
                    long deadline = System.currentTimeMillis() + 10000; // 10 segundos de timeout
                    while (respuestasRecibidas < respuestasEsperadas && System.currentTimeMillis() < deadline) {
                        ACLMessage msg = receive();
                        if (msg != null) {
                            if (msg.getPerformative() == ACLMessage.INFORM) {
                                String[] parts = msg.getContent().split(";");
                                if (parts.length >= 2 && "RESULT".equals(parts[0])) {
                                    double score = Double.parseDouble(parts[1]);
                                    resultados.put(msg.getSender().getLocalName(), score);
                                    respuestasRecibidas++;
                                }
                            }
                        } else {
                            block(1000); // Esperar un segundo antes de volver a comprobar
                        }
                    }

                    // Determinamos los ganadores por partido
                    for (Partido partido : rondaActualPartido) {
                        double scoreA = resultados.getOrDefault(partido.equipoA, Math.random() * 0.2);
                        double scoreB = resultados.getOrDefault(partido.equipoB, Math.random() * 0.2);
                        partido.puntosA = (int) Math.round(scoreA * 100);
                        partido.puntosB = (int) Math.round(scoreB * 100);
                        if (scoreA > scoreB) {
                            partido.ganador = partido.equipoA;
                            equipos.get(partido.equipoA).puntos += 1; // 1 puntos por victoria
                        } else if (scoreB > scoreA) {
                            partido.ganador = partido.equipoB;
                            equipos.get(partido.equipoB).puntos += 1; // 1 puntos por victoria
                        } else {
                            partido.ganador = "Empate";
                        }
                        equipos.get(partido.equipoA).partidosJugados += 1;
                        equipos.get(partido.equipoB).partidosJugados += 1;
                    }
                    // Actualizar la GUI con los resultados y la clasificación parcial
                    SwingUtilities.invokeLater(() -> {
                        partidosListModel.clear();
                        for (Partido partido : rondaActualPartido) {
                            partidosListModel.addElement(partido.toString());
                        }
                        updateRanking();
                    });

                    // Tras mostrar los resultados, esperamos a que el usuario pulse "Siguiente Ronda"
                    waitingForGUI = true;
                    roundIndex++;
                    block();
                }
            }

            @Override
            public boolean done() {
                return finished;
            }
        });
    }

    private void createAndShowGUI() {
        frame = new JFrame("Torneo de Palas de Playa");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        partidosListModel = new DefaultListModel<>();
        JList<String> partidosList = new JList<>(partidosListModel);
        rankingModel = new DefaultListModel<>();
        JList<String> rankingList = new JList<>(rankingModel);

        siguienteBoton = new JButton("Siguiente / Iniciar Ronda");
        siguienteBoton.setEnabled(true);
        siguienteBoton.addActionListener(e -> guiListoParaSiguiente = true);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Partidos / Resultados"), BorderLayout.NORTH);
        panel.add(new JScrollPane(partidosList), BorderLayout.CENTER);
        panel.add(siguienteBoton, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout());
        right.add(new JLabel("Clasificación"), BorderLayout.NORTH);
        right.add(new JScrollPane(rankingList), BorderLayout.CENTER);

        frame.getContentPane().setLayout(new GridLayout(1, 2));
        frame.getContentPane().add(panel);
        frame.getContentPane().add(right);

        frame.setVisible(true);
    }

    private void updateRanking() {
        rankingModel.clear();
        List<TeamInfo> ranking = new ArrayList<>(equipos.values());
        ranking.sort((a, b) -> Integer.compare(b.puntos, a.puntos));
        for (TeamInfo team : ranking) {
            rankingModel.addElement(team.nombreEquipo + " - Puntos: " + team.puntos + ", Partidos Jugados: " + team.partidosJugados);
        }
    }

    private void showFinalScreen() {
        List<TeamInfo> ranking = new ArrayList<>(equipos.values());
        ranking.sort((a, b) -> Integer.compare(b.puntos, a.puntos));
        StringBuilder sb = new StringBuilder("Torneo Finalizado!\n\nClasificación Final:\n");
        for (int i = 0; i < Math.min(3, ranking.size()); i++) {
            String medalla = i == 0 ? "Oro" : (i == 1 ? "Plata" : "Bronce");
            TeamInfo team = ranking.get(i);
            sb.append(String.format("%s: %s - %d pts\n", medalla, team.nombreEquipo, team.puntos));
        }
        JOptionPane.showMessageDialog(frame, sb.toString(), "Torneo Finalizado", JOptionPane.PLAIN_MESSAGE);
    }

    @Override
    protected void takeDown() {
        System.out.println("[Organizer] terminating.");
        frame.dispose();
    }
}

