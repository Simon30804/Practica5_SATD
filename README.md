# Torneo de Palas de Playa con JADE

## 1. Descripción general

Este proyecto implementa un **torneo de palas de playa** usando la plataforma de agentes **JADE** y una interfaz gráfica en **Java Swing**.

Elementos principales:

- Un **agente organizador** (`OrganizerAgent`) que:
  - Crea dinámicamente agentes jugador y equipo.
  - Genera el calendario de partidos (liga "todos contra todos").
  - Coordina las rondas de juego.
  - Recoge resultados y actualiza una clasificación.
  - Ofrece una GUI para seguir y controlar el torneo.
- Agentes **jugador** (`PlayerAgent`) que representan a cada jugador individual.
- Agentes **equipo** (`TeamAgent`), que reciben órdenes de jugar y responden con su rendimiento.
- Clases de **modelo** (`TeamInfo`, `Partido`) y una clase de **utilidad** (`TeamPerformance`) que encapsulan la lógica de dominio.

La aplicación se lanza creando un *main container* de JADE y un agente `organizer` de clase `palasDePlaya.agentes.OrganizerAgent`. Desde ahí se ponen en marcha el resto de agentes y el torneo.

---

## 2. Estructura del proyecto

Rutas relevantes del código fuente:

```text
palasDePlaya/
  agentes/
    OrganizerAgent.java   # Agente organizador (núcleo de la lógica del torneo)
    PlayerAgent.java      # Agente jugador individual
    TeamAgent.java        # Agente equipo (no incluido aquí, pero esperado por OrganizerAgent)
  model/
    TeamInfo.java         # Información de cada equipo
    Partido.java          # Modelo de un partido (equipoA vs equipoB)
  utils/
    TeamPerformance.java  # Modelo de rendimiento y fatiga de los equipos
```

Archivos auxiliares:

- `APDescription.txt` y `MTPs-Main-Container.txt`: ficheros de configuración/soporte de la práctica.

---

## 3. Flujo de ejecución

### 3.1. Arranque de la plataforma JADE

La plataforma JADE se arranca con un comando similar a:

```bash
java -cp "RUTA_A_jade.jar;RUTA_A_out/production/Prac5" jade.Boot -gui organizer:palasDePlaya.agentes.OrganizerAgent
```

Parámetros importantes:

- `-gui`: inicia la GUI de administración remota de JADE.
- `organizer:palasDePlaya.agentes.OrganizerAgent`: crea un agente llamado `organizer` usando la clase `OrganizerAgent` en el *main container*.

### 3.2. Ciclo de vida de `OrganizerAgent`

El método clave es `setup()`:

1. **Mensaje de arranque** en consola indicando que el agente organizador está listo.
2. **Inicialización de la GUI** usando `SwingUtilities.invokeLater(this::createAndShowGUI)` para crear la ventana Swing en el hilo de eventos (EDT).
3. **Obtención del `ContainerController`** con `getContainerController()` para poder crear agentes dinámicamente.
4. **Creación de jugadores (`PlayerAgent`)**:
   - Se elige un número aleatorio de jugadores entre 12 y 22.
   - Para cada índice se crea un agente `PlayerAgent` con nombre `Jugador1`, `Jugador2`, ..., `JugadorN`.
5. **Agrupación en equipos**:
   - Se baraja la lista de nombres de jugadores.
   - Se agrupan por parejas (2 jugadores por equipo) de forma aleatoria.
   - Si sobra un jugador impar, queda sin equipo.
6. **Creación de equipos (`TeamAgent` + `TeamInfo`)**:
   - Para cada pareja se define un nombre de equipo `Equipo1`, `Equipo2`, ...
   - Se genera una `habilidadBase` aleatoria en el rango `[0.4, 1.0]`.
   - Se crea un agente `TeamAgent` para cada equipo, pasándole la `habilidadBase` como argumento.
   - Se crea un objeto `TeamInfo` con nombre del equipo, jugadores y habilidad base, que se almacena en el mapa `equipos`.
7. **Generación del calendario de partidos**:
   - A partir de los nombres de todos los equipos se genera una liga a una vuelta (todos contra todos): para cada par `(i, j)` con `i < j` se crea un `Partido`.
8. **División en rondas**:
   - Se calcula un número de partidos por ronda aproximado (`teamNames.size() / 2`).
   - La lista de partidos se trocea en sublistas, cada una representa una ronda.
9. **Arranque del comportamiento principal (`SimpleBehaviour`)**:
   - Gestiona el avance de las rondas apoyándose en la GUI.
   - Alterna entre:
     - Esperar que el usuario pulse el botón de la GUI.
     - Ejecutar la ronda: enviar mensajes de juego, recibir resultados, actualizar clasificación.

### 3.3. Interacción con la GUI

La GUI se construye en `createAndShowGUI()`:

- Ventana "Torneo de Palas de Playa".
- Panel izquierdo: lista de partidos (`partidosListModel`) y botón "Siguiente / Iniciar Ronda".
- Panel derecho: lista de clasificación (`rankingModel`).
- El botón tiene un `ActionListener` que marca un flag:

```java
siguienteBoton.addActionListener(e -> guiListoParaSiguiente = true);
```

Ese `guiListoParaSiguiente` es leído por el `SimpleBehaviour` para saber si debe continuar con la siguiente fase.

### 3.4. Detalle del `SimpleBehaviour` del organizador

Variables internas del comportamiento:

- `roundIndex`: índice de la ronda actual.
- `waitingForGUI`: indica si el agente está esperando a que el usuario pulse el botón.
- `finished`: indica si el torneo ha terminado.

Flujo:

1. **Comprobación de fin de torneo**:
   - Si `roundIndex >= rondas.size()` se llama a `showFinalScreen()` y se marca `finished = true`.

2. **Fase de espera de GUI (`waitingForGUI == true`)**:
   - Se toma la lista de partidos de la ronda correspondiente.
   - Se actualiza la GUI para mostrar esos partidos.
   - Mientras `guiListoParaSiguiente` sea `false`, el comportamiento hace `block(500)` y no avanza.
   - Cuando el usuario pulsa el botón, `guiListoParaSiguiente` pasa a `true`, se reinicia a `false` y se pone `waitingForGUI = false` para la siguiente iteración.

3. **Fase de ejecución de ronda (`waitingForGUI == false`)**:
   1. Para cada `Partido` de `rondaActualPartido` se envían dos mensajes `REQUEST` con contenido `"PLAY"` a los dos equipos implicados.
   2. Se espera un máximo de `nPartidos * 2` respuestas (una por equipo) con formato `"RESULT;score;partidosJugados"` y `performative` `INFORM`.
   3. Se almacenan los `score` de cada equipo en un mapa `resultados`.
   4. Para cada partido se comparan `scoreA` y `scoreB`:
      - Se actualizan `puntosA`, `puntosB` y `ganador` en el objeto `Partido`.
      - Se incrementan `puntos` y `partidosJugados` en los correspondientes `TeamInfo`.
   5. Se actualiza la GUI:
      - Lista de partidos con resultados.
      - Clasificación ordenada por puntos (`updateRanking()`).
   6. Se avanza a la siguiente ronda:
      - `waitingForGUI = true`.
      - `roundIndex++`.

4. **Pantalla final**:
   - `showFinalScreen()` ordena los equipos por puntos, toma los 3 primeros y muestra un cuadro de diálogo con el podio (oro, plata, bronce).

---

## 4. Descripción de las clases principales

### 4.1. `palasDePlaya.agentes.OrganizerAgent`

Rol: **Agente organizador del torneo**.

Responsabilidades:

- Crear y lanzar agentes jugador (`PlayerAgent`) y equipo (`TeamAgent`).
- Crear el calendario completo de partidos (`Partido`).
- Controlar el flujo de rondas:
  - Esperar interacción del usuario vía GUI.
  - Enviar órdenes de juego a los equipos.
  - Recibir resultados, decidir ganadores y actualizar puntuaciones.
- Mantener y mostrar la clasificación actualizada.
- Mostrar la pantalla con el ranking final.

Métodos relevantes:

- `setup()`: arranca la GUI, crea los agentes y genera el calendario.
- `createAndShowGUI()`: construye la interfaz Swing.
- `updateRanking()`: recalcula y pinta la clasificación en base al mapa `equipos`.
- `showFinalScreen()`: presenta la clasificación final (top 3) en un `JOptionPane`.
- `takeDown()`: cierra la ventana y muestra un mensaje al terminar el agente.

Atributos destacados:

- `equipos: Map<String, TeamInfo>`: estado dinámico de todos los equipos.
- `rondaActualPartido: List<Partido>`: partidos de la ronda en curso.
- `guiListoParaSiguiente: boolean`: bandera para sincronizar botón de la GUI y behaviour.

---

### 4.2. `palasDePlaya.agentes.PlayerAgent`

Rol: **Agente jugador individual**.

Responsabilidades:

- Representar a un jugador humano en el sistema multiagente.
- Mostrar mensajes en consola indicando su ciclo de vida.

Métodos:

- `setup()`: inicializa el agente mostrando `"Jugador <nombre> listo para jugar!"`.
- `takeDown()`: al terminar el agente, muestra `"Jugador <nombre> se retira del juego."`.

En esta versión, los `PlayerAgent` no intervienen directamente en el cálculo de los resultados, pero sirven para modelar un entorno con múltiples agentes y para extender funcionalidades en el futuro.

---

### 4.3. `palasDePlaya.agentes.TeamAgent`

> Nota: el archivo no está incluido aquí, pero se infiere su comportamiento a partir del uso que hace el `OrganizerAgent`.

Rol: **Agente que representa a un equipo**.

Responsabilidades esperadas:

- Recibir mensajes `REQUEST` con contenido `"PLAY"`.
- Mantener un número de partidos jugados y una `habilidadBase`.
- Calcular un `score` de rendimiento usando `TeamPerformance` y algo de aleatoriedad.
- Enviar de vuelta un `ACLMessage INFORM` al organizador con contenido `"RESULT;score;partidosJugados"`.

Campos típicos:

- `double habilidadBase`: recibida como argumento al crear el agente.
- `int partidosJugados`: contador de partidos disputados.
- `TeamPerformance performance`: para modelar fatiga.

---

### 4.4. `palasDePlaya.model.TeamInfo`

Rol: **Información de dominio de cada equipo**.

Campos:

- `nombreEquipo`: nombre del equipo (p. ej. `"Equipo1"`).
- `jugadores`: lista de nombres de agentes jugador (
  `List<String>` con valores tipo `"Jugador3"`).
- `puntos`: puntos acumulados en la clasificación (inicialmente 0).
- `partidosJugados`: número de partidos disputados.
- `habilidadBase`: habilidad inicial del equipo (coherente con la de `TeamAgent`).

Se utiliza principalmente desde `OrganizerAgent` para mantener el estado del torneo y para ordenar la clasificación.

---

### 4.5. `palasDePlaya.model.Partido`

Rol: **Entidad que representa un partido concreto entre dos equipos**.

Campos:

- `equipoA`, `equipoB`: nombres de los equipos (coinciden con los `localName` de los `TeamAgent`).
- `puntosA`, `puntosB`: puntuaciones del partido para cada equipo.
- `ganador`: nombre del equipo ganador o `"Empate"` si hay empate.

Constructor:

```java
public Partido(String equipoA, String equipoB) {
    this.equipoA = equipoA;
    this.equipoB = equipoB;
    this.puntosA = 0;
    this.puntosB = 0;
    this.ganador = null;
}
```

`toString()` devuelve una representación legible para la GUI, por ejemplo:

- Antes de jugarse: `"Equipo1 vs Equipo2 (pendiente)"`.
- Tras jugarse: `"Equipo1 57 - 43 Equipo2 (ganador: Equipo1)"`.

---

### 4.6. `palasDePlaya.utils.TeamPerformance`

Rol: **Modelo de rendimiento/fatiga de un equipo**.

Campos:

- `habilidadBase`: nivel máximo inicial de rendimiento.
- `caidaRendimiento`: parámetro que controla cómo decrece el rendimiento con los partidos.

Método principal:

```java
public double getRendimiento(int partidosJugados) {
    double exponente = Math.pow(partidosJugados, 1.5);
    return habilidadBase * Math.exp(-caidaRendimiento * exponente);
}
```

Interpretación:

- Rinde más al principio; conforme aumentan los `partidosJugados`, el rendimiento cae de manera exponencial.
- El exponente `1.5` hace que la caída se acelere con el número de partidos, simulando fatiga creciente.

Se usa desde `TeamAgent` para generar el `score` que se envía al organizador.

---

## 5. Ejecución del proyecto

### 5.1. Requisitos

- Java JDK 8+ (en este caso, se ha usado OpenJDK 24.0.1).
- JADE 4.6.0 (o compatible), con `jade.jar` accesible.
- Un IDE como IntelliJ IDEA, Eclipse o similar, o bien compilación por línea de comandos.

### 5.2. Ejecución desde IntelliJ IDEA

1. Importar el proyecto como proyecto Java.
2. Asegurarse de que `jade.jar` está añadido como dependencia del módulo.
3. Crear una configuración de ejecución (`Run Configuration`) de tipo **Application** con:
   - `Main class`: `jade.Boot`.
   - `Use classpath of module`: el módulo que contiene el código (`Prac5`, por ejemplo).
   - `Program arguments`:

     ```
     -gui organizer:palasDePlaya.agentes.OrganizerAgent
     ```

4. Ejecutar la configuración.
5. Observar en consola la creación de agentes y, en paralelo, la aparición de:
   - La GUI de administración de JADE.
   - La ventana Swing "Torneo de Palas de Playa".

### 5.3. Ejecución desde línea de comandos (Windows)

Suponiendo que el código compilado está en `out/production/Prac5`:

```bat
cd /d D:\4UNI\1Cuatri\SATD\Prac5

java -cp "D:\4UNI\1Cuatri\SATD\JADE-all-4.6.0\JADE-bin-4.6.0\jade\lib\jade.jar;D:\4UNI\1Cuatri\SATD\Prac5\out\production\Prac5" jade.Boot -gui organizer:palasDePlaya.agentes.OrganizerAgent
```

Ajustar las rutas si el módulo o la carpeta de salida tienen otros nombres.

---

## 6. Posibles extensiones / trabajo futuro

- Hacer que los `PlayerAgent` participen de forma más activa (por ejemplo, que cada jugador tenga una habilidad propia y el rendimiento del equipo sea una función de ambas).
- Añadir persistencia de resultados (guardar y cargar torneos desde fichero).
- Implementar distintos formatos de torneo (eliminación directa, liguilla con play-off, etc.).
- Añadir estadísticas detalladas de partidos (por ejemplo, puntos por set, historial de enfrentamientos).
- Desarrollar una interfaz gráfica más rica (gráficas de rendimiento, filtros por equipo, etc.).

---

## 7. Créditos

Proyecto desarrollado como práctica académica para la asignatura de sistemas multiagente / tecnologías de agentes, utilizando JADE 4.6.0 sobre Java.

