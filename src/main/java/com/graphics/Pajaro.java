package com.graphics;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 *  Pajaro — Un jugador del juego
 *
 *  Responsabilidades:
 *    - Guardar su posición Y, velocidad Y, puntaje y estado (vivo/muerto)
 *    - Aplicar física (gravedad + salto)
 *    - Detectar colisión con los bordes de la pantalla
 *    - Dibujarse a sí mismo como un pájaro compuesto de varias partes
 *
 *  NO sabe nada de GLFW, shaders ni la ventana.
 *  Para dibujar, recibe un Renderizador y llama a sus métodos.
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class Pajaro {

    // ══════════════════════════════════════════════════════════════
    //  ESTADO DEL PÁJARO
    // ══════════════════════════════════════════════════════════════

    private final float xFija;  // posición X fija (el pájaro no se mueve horizontalmente)
    private float posY;         // posición Y actual (cambia con la física)
    private float velY;         // velocidad vertical actual (positivo = sube, negativo = cae)
    private boolean vivo;       // false = chocó y ya no se dibuja ni se mueve
    private int puntaje;        // puntos acumulados en esta partida

    // Color del pájaro (RGB entre 0.0 y 1.0)
    private final float colorR, colorG, colorB;

    // ══════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════
    /**
     * Crea un pájaro en la posición inicial.
     *
     * @param xFija   posición X que mantiene siempre (en NDC)
     * @param posYInicial  posición Y de inicio
     * @param r, g, b color base del pájaro
     */
    public Pajaro(float xFija, float posYInicial, float r, float g, float b) {
        this.xFija  = xFija;
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
        reset(posYInicial);
    }

    // ══════════════════════════════════════════════════════════════
    //  RESET
    // ══════════════════════════════════════════════════════════════
    /**
     * Vuelve al pájaro al estado inicial.
     * Se llama al iniciar la partida y al reiniciar.
     */
    public void reset(float posYInicial) {
        posY    = posYInicial;
        velY    = 0.0f;
        vivo    = true;
        puntaje = 0;
    }

    // ══════════════════════════════════════════════════════════════
    //  FÍSICA
    // ══════════════════════════════════════════════════════════════
    /**
     * Avanza la física del pájaro un frame.
     *
     * Secuencia:
     *   1. Aplicar gravedad → la velocidad vertical cae cada frame
     *   2. Limitar velocidad máxima de caída (para que no sea imposible)
     *   3. Mover el pájaro según su velocidad
     *   4. Detectar si tocó el techo o el suelo
     *
     * dt  delta time en segundos (tiempo desde el frame anterior)
     */
    public void actualizarFisica(float dt) {
        if (!vivo) return;

        // 1. Gravedad: acelera hacia abajo cada frame
        velY += AppFlappyBird.GRAVEDAD * dt;

        // 2. Limitar la velocidad máxima de caída
        if (velY < AppFlappyBird.VELOCIDAD_MAX_CAIDA) {
            velY = AppFlappyBird.VELOCIDAD_MAX_CAIDA;
        }

        // 3. Mover el pájaro
        posY += velY * dt;

        // 4. Colisión con bordes (techo y suelo visible)
        float bordeArriba = 0.88f;  // el pájaro no puede pasar del techo
        float bordeAbajo  = -0.88f; // -0.88 porque el suelo visible está ahí

        boolean tocaTecho = posY + AppFlappyBird.BIRD_ALTO * 0.5f >= bordeArriba;
        boolean tocaSuelo = posY - AppFlappyBird.BIRD_ALTO * 0.5f <= bordeAbajo;

        if (tocaTecho || tocaSuelo) {
            vivo = false;
        }
    }

    /**
     * Aplica un impulso vertical hacia arriba (el "salto").
     * Solo funciona si el pájaro está vivo.
     */
    public void saltar() {
        if (vivo) {
            velY = AppFlappyBird.IMPULSO_SALTO;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DIBUJO
    // ══════════════════════════════════════════════════════════════
    /**
     * Dibuja el pajaro como una figura compuesta de multiples primitivas.
     *
     * Partes (en orden de dibujo, de atrás hacia adelante):
     *   1. Cuerpo       → rectangulo principal (color del jugador)
     *   2. Ala animada  → rect pequeño que sube/baja según la velocidad
     *   3. Cola         → triángulo apuntando a la izquierda
     *   4. Pico         → triángulo apuntando a la derecha
     *   5. Ojo blanco   → círculo blanco
     *   6. Pupila negra → círculo negro dentro del ojo
     *
     * La "animacion" del ala se logra desplazandola verticalmente
     * segun velY: si sube → ala arriba, si cae → ala abajo.
     *
     *  r  el Renderizador que maneja OpenGL
     */
    public void dibujar(Renderizador r) {
        float cx = xFija;
        float cy = posY;

        // ── 1. Cuerpo principal ───────────────────────────────────
        r.dibujarRect(cx, cy, AppFlappyBird.BIRD_ANCHO, AppFlappyBird.BIRD_ALTO,
                      colorR, colorG, colorB);

        // ── 2. Ala animada ────────────────────────────────────────
        // El denominador (|velY| + 0.001) evita división por cero y suaviza
        float desplazamientoAla = (velY / (Math.abs(velY) + 0.001f)) * 0.02f;
        float alaY = cy + desplazamientoAla;
        r.dibujarRect(cx - 0.005f, alaY,
                      AppFlappyBird.BIRD_ANCHO * 0.55f,
                      AppFlappyBird.BIRD_ALTO  * 0.28f,
                      colorR * 0.80f, colorG * 0.80f, colorB * 0.80f);

        // ── 3. Cola (triángulo izquierdo) ────────────────────────
        float colaCx = cx - AppFlappyBird.BIRD_ANCHO * 0.5f;
        r.dibujarTriangulo(
            colaCx,          cy + 0.015f,   // vértice superior (toca el cuerpo)
            colaCx - 0.035f, cy,            // punta de la cola
            colaCx,          cy - 0.015f,   // vértice inferior (toca el cuerpo)
            colorR * 0.75f, colorG * 0.75f, colorB * 0.75f // más oscuro que el cuerpo
        );

        // ── 4. Pico (triángulo derecho, naranja) ──────────────────
        float picoCx = cx + AppFlappyBird.BIRD_ANCHO * 0.5f;
        r.dibujarTriangulo(
            picoCx,          cy + 0.012f,   // vértice superior (toca el cuerpo)
            picoCx + 0.04f,  cy,            // punta del pico
            picoCx,          cy - 0.012f,   // vértice inferior (toca el cuerpo)
            1.0f, 0.55f, 0.0f             // naranja siempre
        );

        // ── 5. Ojo blanco ─────────────────────────────────────────
        float ojoX = cx + AppFlappyBird.BIRD_ANCHO * 0.22f;
        float ojoY = cy + AppFlappyBird.BIRD_ALTO  * 0.20f;
        r.dibujarCirculo(ojoX, ojoY, 0.018f, 1.0f, 1.0f, 1.0f);

        // ── 6. Pupila negra ───────────────────────────────────────
        r.dibujarCirculo(ojoX + 0.005f, ojoY - 0.003f, 0.009f, 0.0f, 0.0f, 0.0f);
    }

    // ══════════════════════════════════════════════════════════════
    //  GETTERS
    // ══════════════════════════════════════════════════════════════
    public boolean estaVivo()  { return vivo; }
    public float   getPosY()   { return posY; }
    public float   getVelY()   { return velY; }
    public float   getXFija()  { return xFija; }
    public int     getPuntaje(){ return puntaje; }

    /** Suma un punto al puntaje de este pájaro. */
    public void sumarPunto() { puntaje++; }

    /** Marca al pájaro como muerto (chocó con una tubería). */
    public void matar() { vivo = false; }
}
