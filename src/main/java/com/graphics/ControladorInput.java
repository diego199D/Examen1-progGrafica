package com.graphics;

import org.lwjgl.glfw.GLFW;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 *  ControladorInput — Lectura del teclado GLFW
 *
 *  Responsabilidades:
 *    - Leer el estado actual de cada tecla relevante
 *    - Implementar detección de FLANCO (edge detection):
 *        solo reporta "pulsado" cuando la tecla pasa de
 *        NO presionada → presionada (evita disparos múltiples)
 *    - Ofrecer métodos semánticos: saltarJ1(), saltarJ2(), etc.
 *
 *  AppFlappyBird llama a actualizar() cada frame ANTES de
 *  consultar los métodos de acción.
 *
 *  ¿Qué es detección de flanco?
 *    Sin flanco: si mantenés SPACE 10 frames, saltarJ1() devuelve
 *                true 10 veces → el pájaro "saltaría" 10 veces.
 *    Con flanco: solo devuelve true el primer frame que se presiona.
 *                Tenés que soltar y volver a presionar para otro salto.
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class ControladorInput {

    // ══════════════════════════════════════════════════════════════
    //  REFERENCIA A LA VENTANA
    // ══════════════════════════════════════════════════════════════
    private final long ventana; // handle de GLFW (necesario para consultar teclas)

    // ═══════════════════════════════════════════════════════════════════════
    //  ESTADO ACTUAL (este frame)
    // ══════════════════════════════════════════════════════════════
    private boolean space;   // SPACE presionada ahora
    private boolean w;       // W presionada ahora
    private boolean arriba;  // FLECHA ARRIBA presionada ahora
    private boolean r;       // R presionada ahora
    private boolean escape;  // ESC presionada ahora
    private boolean e; //para mi jugador 3

    // ═══════════════════════════════════════════════════════════════════════
    //  ESTADO ANTERIOR (frame anterior)
    //  — necesario para detectar el flanco (cambio de estado)
    // ══════════════════════════════════════════════════════════════
    private boolean prevSpace;
    private boolean prevW;
    private boolean prevArriba;
    private boolean prevR;
    private boolean prevE;
    // ESC no necesita detección de flanco (se actúa al sostenerse)

    // ═══════════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════
    /**
     * ventana  = handle de la ventana GLFW (obtenido de Renderizador)
     */
    public ControladorInput(long ventana) {
        this.ventana = ventana;
    }

    // ══════════════════════════════════════════════════════════════
    //  ACTUALIZACIÓN (se llama UNA VEZ por frame)
    // ══════════════════════════════════════════════════════════════
    /**
     * Lee el estado actual del teclado y actualiza el estado anterior.
     *
     * Debe llamarse ANTES de consultar saltarJ1(), saltarJ2(), etc.
     *
     * Secuencia:
     *   1. Guardar estado actual como "anterior"
     *   2. Leer el nuevo estado actual desde GLFW
     */
    public void actualizar() {
        // 1. Guardar estado anterior
        prevSpace  = space;
        prevW      = w;
        prevArriba = arriba;
        prevR      = r;
        prevE = e;

        // 2. Leer estado actual
        // GLFW_PRESS = tecla presionada, GLFW_RELEASE = tecla suelta
        space  = GLFW.glfwGetKey(ventana, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        w      = GLFW.glfwGetKey(ventana, GLFW.GLFW_KEY_W)     == GLFW.GLFW_PRESS;
        arriba = GLFW.glfwGetKey(ventana, GLFW.GLFW_KEY_UP)    == GLFW.GLFW_PRESS;
        r      = GLFW.glfwGetKey(ventana, GLFW.GLFW_KEY_R)     == GLFW.GLFW_PRESS;
        escape = GLFW.glfwGetKey(ventana, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;
        e      = GLFW.glfwGetKey(ventana, GLFW.GLFW_KEY_E) == GLFW.GLFW_PRESS;
    }

    // ══════════════════════════════════════════════════════════════
    //  ACCIONES (con detección de flanco)
    // ══════════════════════════════════════════════════════════════

    /**
     * ¿El jugador 1 quiere saltar?
     * True solo en el frame en que se PRESIONÓ SPACE (no mientras se mantiene).
     * Control: tecla SPACE
     */
    public boolean saltarJ1() {
        return space && !prevSpace; // flanco de subida
    }

    /**
     * ¿El jugador 2 quiere saltar?
     * True en el frame que se presionó W o FLECHA ARRIBA.
     * Cualquiera de las dos teclas vale.
     */
    public boolean saltarJ2() {
        boolean flancoW      = w      && !prevW;
        boolean flancoArriba = arriba && !prevArriba;
        return flancoW || flancoArriba;
    }

    public boolean saltarJ3() {
        return e && !prevE; 
    }

    /**
     * Se pidio reiniciar?
     * True solo en el frame que se presiono R.
     */
    public boolean reiniciarPulsado() {
        return r && !prevR;
    }

    /**
     * ¿Se presionó ESC?
     * Sin detección de flanco: actúa mientras se mantiene presionada.
     * (Está bien para cerrar el juego)
     */
    public boolean escapePulsado() {
        return escape;
    }
}
