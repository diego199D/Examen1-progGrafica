package com.graphics;

import org.lwjgl.glfw.GLFW;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 *  AppFlappyBird — Clase principal del juego
 *
 *  Solo hace tres cosas:
 *    1. Inicializar todo (ventana, OpenGL, recursos)
 *    2. Correr el bucle principal (game loop)
 *    3. Limpiar la memoria al salir
 *
 *  Toda la lógica "pesada" está en las otras clases.
 * ╚══════════════════════════════════════════════════════════════╝
 *
 *  FLUJO:
 *    main() → new AppFlappyBird().run()
 *    run()  → init() → resetGame() → loop() → cleanup()
 *    loop() → procesarInput() → actualizar(dt) → render() → swap → repeat
 */
public class AppFlappyBird {

    static boolean bandera = false;
    // ══════════════════════════════════════════════════════════════
    //  CONSTANTES DE VENTANA
    // ══════════════════════════════════════════════════════════════
    static final int ANCHO = 900;
    static final int ALTO  = 700;

    // ══════════════════════════════════════════════════════════════
    //  CONSTANTES COMPARTIDAS DE FISICA Y GEOMETRIA
    //  (static final = constantes globales accesibles desde cualquier clase)
    // ══════════════════════════════════════════════════════════════

    // Posición X fija de cada pájaro (en NDC: -1 a +1)
    static final float BIRD1_X = -0.50f;
    static final float BIRD2_X = -0.30f;
    static final float BIRD3_X = -0.70f;

    static final float BIRD_ANCHO = 0.10f;
    static final float BIRD_ALTO  = 0.10f;

    // Fisica vertical
    static final float GRAVEDAD            = -1.9f;
    static final float IMPULSO_SALTO       =  0.85f;
    static final float VELOCIDAD_MAX_CAIDA = -1.8f;

    // Tuberias
    static final float TUBERIA_ANCHO           = 0.18f;
    static final float GAP_ALTO                = 0.48f;
    static final float VELOCIDAD_TUBERIAS_BASE = 0.62f;
    static final float TIEMPO_SPAWN_BASE       = 1.5f;
    static final float GAP_MIN_CENTRO          = -0.40f;
    static final float GAP_MAX_CENTRO          =  0.40f;

    // Limites de dificultad
    static final float VELOCIDAD_MAX    = 1.4f;
    static final float TIEMPO_SPAWN_MIN = 0.9f;
    static final int   PUNTOS_POR_NIVEL = 5;

    // ═══════════════════════════════════════════════════════════════════════════════════════════════════════
    //  REFERENCIAS A LOS MODULOS DEL JUEGO
    // ══════════════════════════════════════════════════════════════
    private Renderizador   renderizador;   // OpenGL: shaders, VAO/VBO, primitivas
    private ControladorInput input;        // Teclado GLFW
    private Pajaro         pajaro1;        // Jugador 1 (ESPACIO, amarillo)
    private Pajaro         pajaro2;        // Jugador 2 (W/↑, naranja)
    private Pajaro         pajaro3;
    private GestorTuberias gestor;         // Lista de tuberias, spawn, puntaje
    private Fondo          fondo;          // Nubes, arboles, suelo
    private Audio          audio;          // Efectos de sonido

    // Ventana GLFW (se comparte con ControladorInput y Renderizador)
    private long ventana;

    // Estado global del juego
    private boolean empezado; // false = pantalla de inicio
    private boolean gameOver; // true  = ambos jugadores muertos
    private int     nivel;    // nivel actual de dificultad

    // Velocidad/tiempo actuales (se ajustan al subir de nivel)
    private float velocidadTuberias;
    private float tiempoSpawn;

    // ══════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ═══════════════════════════════════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        new AppFlappyBird().run();
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════════════════
    //  FLUJO PRINCIPAL
    // ══════════════════════════════════════════════════════════════
    /**
     * Arranca el juego completo.
     * Este metodo es el "director de orquesta": llama a cada paso en orden.
     */
    public void run() {
        init();       // 1. Crear ventana, OpenGL, shaders
        resetGame();  // 2. Poner todo en posición inicial
        loop();       // 3. Correr el juego hasta que se cierre
        cleanup();    // 4. Liberar memoria de GPU y cerrar
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  INICIALIZACIÓN
    // ──────────────────────────────────────────────────────────────
    /**
     * Crea la ventana GLFW y prepara todos los modulos.
     * Solo se llama UNA vez al inicio.
     */
    private void init() {
        // 1. Renderizador crea la ventana GLFW y todo OpenGL
        renderizador = new Renderizador(ANCHO, ALTO, "Flappy Bird OpenGL");
        ventana = renderizador.getVentana();

        // 2. El resto de modulos se instancian con lo que necesiten
        input  = new ControladorInput(ventana);
        audio  = new Audio();
        fondo  = new Fondo();
        gestor = new GestorTuberias();

        // Los pájaros se crean con su X fija y su color
        pajaro1 = new Pajaro(BIRD1_X, 0.1f,  0.98f, 0.85f, 0.20f); // amarillo
        pajaro2 = new Pajaro(BIRD2_X, -0.1f, 0.95f, 0.50f, 0.10f); // naranja
        pajaro3 = new Pajaro(BIRD3_X, -0.1f, 0.80f, 0.50f, 0.10f);
    }

    // ──────────────────────────────────────────────────────────────
    //  RESET
    // ────────────────────────────────────────────────────────────────────────────────
    /**
     * Pone todo en el estado inicial para empezar (o reiniciar) la partida.
     * Se llama al inicio y cuando el jugador presiona R/SPACE en game over.
     */
    void resetGame() {
        pajaro1.reset(0.1f);
        pajaro2.reset(-0.1f);
        pajaro3.reset(-0.3f);
        gestor.reset();

        empezado          = false;
        gameOver          = false;
        nivel             = 1;
        velocidadTuberias = VELOCIDAD_TUBERIAS_BASE;
        tiempoSpawn       = TIEMPO_SPAWN_BASE;

        actualizarTitulo();
    }

    // ══════════════════════════════════════════════════════════════
    //  BUCLE PRINCIPAL (GAME LOOP)
    // ════════════════════════════════════════════════════════════════════════════════════════════════════════════════
    /**
     * Se repite 60 veces por segundo hasta que el usuario cierre la ventana.
     *
     * ¿Que es dt (delta time)?
     *   Es el tiempo en segundos que paso desde el frame anterior.
     *   A 60 FPS, dt ≈ 0.016 segundos.
     *   Multiplicar por dt hace que la física sea igual sin importar el FPS.
     *
     * ¿Por qué capamos dt a 0.033?
     *   Si el juego se congela (lag), dt seria enorme y los objetos
     *   "saltarian" una distancia absurda. 0.033s = minimo 30 FPS.
     */
    private void loop() {
        float ultimoTiempo = (float) GLFW.glfwGetTime();

        while (!GLFW.glfwWindowShouldClose(ventana)) {
            // Calcular delta time
            float ahora = (float) GLFW.glfwGetTime();
            float dt    = Math.min(ahora - ultimoTiempo, 0.033f); // cap anti-lag
            ultimoTiempo = ahora;

            procesarInput();    // leer teclado
            actualizar(dt);     // mover fisica
            render();           // dibujar frame

            GLFW.glfwSwapBuffers(ventana); // mostrar lo dibujado
            GLFW.glfwPollEvents();         // atender eventos del OS
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════════════════
    //  INPUT
    // ══════════════════════════════════════════════════════════════
    /**
     * Lee el teclado y actua en consecuencia.
     * Usa deteccion de flanco (edge): solo actua cuando la tecla
     * pasa de NO presionada → presionada (evita disparos multiples).
     */
    private void procesarInput() {
        input.actualizar(); // leer estado actual del teclado

        // ESC → cerrar juego
        if (input.escapePulsado()) {
            GLFW.glfwSetWindowShouldClose(ventana, true);
            return;
        }

        // R → reiniciar (solo durante game over)
        if (input.reiniciarPulsado() && gameOver) {
            resetGame();
            return;
        }

        // Jugador 1: SPACE
        if (input.saltarJ1()) {
            if (gameOver) {
                resetGame();
                empezado = true;
            }
            if (pajaro1.estaVivo()) {
                empezado = true;
                pajaro1.saltar();
                audio.sonarSalto();
            }
        }

        // Jugador 2: W o FLECHA ARRIBA
        if (input.saltarJ2()) {
            if (gameOver) {
                resetGame();
                empezado = true;
            }
            if (pajaro2.estaVivo()) {
                empezado = true;
                pajaro2.saltar();
                audio.sonarSalto();
            }
        }
        if (input.saltarJ3()) {
            if (gameOver) {
                resetGame();
                empezado = true;
            }
            if (pajaro3.estaVivo()) {
                empezado = true;
                pajaro3.saltar();
                audio.sonarSalto();
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ACTUALIZACION DE LOGICA
    // ══════════════════════════════════════════════════════════════
    /**
     * Avanza la simulacion un frame.
     * Orden: fisica → spawn → mover tuberias → colisiones → nivel → game over.
     */
    private void actualizar(float dt) {
        if (!empezado || gameOver) return;

        // 1. Fisica de cada pajaro
        pajaro1.actualizarFisica(dt);
        pajaro2.actualizarFisica(dt);
        pajaro3.actualizarFisica(dt);

        // 2. Spawn y movimiento de tuberias
        gestor.actualizar(dt, velocidadTuberias, tiempoSpawn);

        // 3. Colisiones tuberia ↔ pajaro y puntaje
        boolean puntoJ1 = gestor.verificarPuntajeYColision(pajaro1, BIRD1_X, 1);
        boolean puntoJ2 = gestor.verificarPuntajeYColision(pajaro2, BIRD2_X, 2);
        boolean puntoJ3 = gestor.verificarPuntajeYColision(pajaro3, BIRD3_X, 3);

        if (puntoJ1 || puntoJ2 || puntoJ3) {
            audio.sonarPunto();
            actualizarTitulo();
        }

        // 4. Actualizar nivel de dificultad
        int puntajeTotal = pajaro1.getPuntaje() + pajaro2.getPuntaje() + pajaro3.getPuntaje();
        int nivelNuevo   = 1 + puntajeTotal / PUNTOS_POR_NIVEL;
        if (nivelNuevo != nivel) {
            nivel             = nivelNuevo;
            velocidadTuberias = Math.min(VELOCIDAD_MAX,    VELOCIDAD_TUBERIAS_BASE + (nivel - 1) * 0.12f);
            tiempoSpawn       = Math.max(TIEMPO_SPAWN_MIN, TIEMPO_SPAWN_BASE       - (nivel - 1) * 0.08f);
            actualizarTitulo();
        }

        // 5. Game over: ambos muertos
        if (!pajaro1.estaVivo() && !pajaro2.estaVivo() && !pajaro3.estaVivo()) {
            gameOver = true;
            audio.sonarGameOver();
            actualizarTitulo();
        }
        if(nivel == 2){
            //gameOver = true;
            gameOver = true;
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    //  RENDER
    // ══════════════════════════════════════════════════════════════
    /**
     * Pinta el frame completo.
     * Orden importa: lo que se dibuja primero queda "atras" (fondo),
     * lo que se dibuja ultimo queda "adelante" (overlay).
     */
    private void render() {
        renderizador.limpiarPantalla(0.52f, 0.80f, 0.92f); // cielo celeste

        // 1. Fondo decorativo (nubes, arboles, suelo)
        fondo.dibujar(renderizador);

        // 2. Tuberias
        gestor.dibujar(renderizador);

        // 3. Pájaros (solo si están vivos)
        if (pajaro1.estaVivo()) pajaro1.dibujar(renderizador);
        if (pajaro2.estaVivo()) pajaro2.dibujar(renderizador);
        if (pajaro3.estaVivo()) pajaro3.dibujar(renderizador);

        // 4. Overlays de UI--------------------------------------------------------------------------------------------------+++++++
        if (gameOver) {
            // Franja oscura de "game over"
            if(nivel == 2)
                renderizador.dibujarRect(0.0f, 0.0f, 2.0f, 0.40f, 0.10f, 0.40f, 0.10f);
            else
                renderizador.dibujarRect(0.0f, 0.0f, 2.0f, 0.28f, 0.10f, 0.12f, 0.15f);
        }
        
        if (!empezado && !gameOver) {
            // Franja de "presioná SPACE para empezar"
            renderizador.dibujarRect(0.0f, 0.60f, 2.0f, 0.22f, 0.10f, 0.12f, 0.15f);            
        }
        
    }

    // ══════════════════════════════════════════════════════════════
    //  HUD / TÍTULO
    // ══════════════════════════════════════════════════════════════
    /**
     * Actualiza el texto de la barra de título con puntaje y estado.
     * OpenGL Core Profile no tiene fuentes nativas, así que usamos
     * el título de la ventana como HUD (truco válido y simple).
     */
    private void actualizarTitulo() {//----------------------------------------------------------------
        String base = String.format(
            "Flappy Bird | J1(SPACE): %d pts  J2(W/↑): %d pts | J3(E): %d pts | Nivel %d",
            pajaro1.getPuntaje(), pajaro2.getPuntaje(), pajaro3.getPuntaje(), nivel
        );

        if (!empezado) {
            GLFW.glfwSetWindowTitle(ventana, base + " | SPACE / W para empezar");
        } else if (gameOver) {
            int p1 = pajaro1.getPuntaje(), p2 = pajaro2.getPuntaje(), p3 = pajaro3.getPuntaje();
            String ganador = (p1 > p2) ? "¡Ganó J1!" : (p2 > p1) ? "¡Ganó J2!" : "¡Empate!";
            GLFW.glfwSetWindowTitle(ventana, base + " | GAME OVER - " + ganador + " - R/SPACE para reiniciar");
        } else {
            GLFW.glfwSetWindowTitle(ventana, base);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  LIMPIEZA
    // ══════════════════════════════════════════════════════════════
    /**
     * Libera todos los recursos al cerrar.
     * Siempre se libera en orden inverso a la creación.
     */
    private void cleanup() {
        renderizador.limpiar(); // VAO, VBO, programa GLSL, ventana GLFW
    }
}
