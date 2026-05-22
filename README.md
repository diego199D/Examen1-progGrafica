# OpenGL Java Class (LWJGL)

Proyecto base de OpenGL en Java usando LWJGL + GLFW. Incluye:
- Un triangulo estatico
- Un triangulo movible con teclado
- Un juego Flappy Bird completo (2 jugadores)

## Requisitos

- Java 17 o superior
- Maven 3.9+
- Compatible con macOS (Intel y Apple Silicon), Windows y Linux

## Compilar

```bash
mvn compile
```

## Ejecutar

### App (triangulo base)

```bash
mvn compile exec:exec
```

### AppMovimientoTeclado (triangulo movible)

```bash
mvn compile exec:exec -DmainClass=com.graphics.AppMovimientoTeclado
```

Controles: WASD / Flechas, ESC para cerrar.

### AppFlappyBird (juego completo)

```bash
mvn compile exec:exec -DmainClass=com.graphics.AppFlappyBird
```

Controles:
- SPACE: saltar jugador 1 o reiniciar
- W / Flecha Arriba: saltar jugador 2 o reiniciar
- R: reiniciar (en game over)
- ESC: cerrar ventana

## Notas importantes

- Usar siempre `exec:exec` (NO `exec:java`). El POM configura `exec:exec` para pasar
  `-XstartOnFirstThread` en macOS.
- La clase por defecto se define via `<mainClass>` en `pom.xml`. Se sobreescribe
  con `-DmainClass=...`.
- Todas las coordenadas de dibujo estan en NDC (Normalized Device Coordinates,
  rango [-1, +1]).

## Documentacion de estudio

- `docs/apuntesv4.md` — Guia de estudio ultra-detallada con callouts y diagramas
- `AGENTS.md` — Instrucciones para asistentes AI sobre el proyecto

## Estructura del proyecto

```
opengl-java-class/
├── pom.xml
├── AGENTS.md
├── README.md
├── src/main/java/com/graphics/
│   ├── App.java
│   ├── AppMovimientoTeclado.java
│   ├── AppFlappyBird.java
│   ├── Audio.java
│   ├── ControladorInput.java
│   ├── Fondo.java
│   ├── GestorTuberias.java
│   ├── Pajaro.java
│   ├── Renderizador.java
│   └── Tuberia.java
└── docs/
    └── apuntesv4.md
```
