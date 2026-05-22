package com.graphics;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 *  Tuberia — Una sola tubería (par de columnas con un hueco)
 *
 *  Responsabilidades:
 *    - Guardar su posición X y el centro del hueco (gapCentroY)
 *    - Saber si ya fue puntuada para cada jugador
 *    - Dibujarse (parte superior + borde + parte inferior + borde)
 *    - Detectar si colisiona con un pájaro (AABB)
 *
 *  GestorTuberias crea y mueve las instancias de esta clase.
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class Tuberia {

    // ══════════════════════════════════════════════════════════════
    //  ESTADO DE LA TUBERÍA
    // ══════════════════════════════════════════════════════════════

    float   posX;        // centro horizontal actual (se mueve hacia la izquierda cada frame)
    float   gapCentroY;  // centro vertical del hueco (varía aleatoriamente al spawnear)
    boolean puntuada1;   // true = ya se contó el punto para el jugador 1
    boolean puntuada2;   // true = ya se contó el punto para el jugador 2
    boolean puntuada3;

    // ══════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════
    /**
     * Crea una tubería nueva en el borde derecho de la pantalla.
     *
     * @param posX        posición X inicial (normalmente 1.2, fuera de pantalla)
     * @param gapCentroY  centro del hueco por donde pasan los pájaros
     */
    public Tuberia(float posX, float gapCentroY) {
        this.posX       = posX;
        this.gapCentroY = gapCentroY;
        this.puntuada1  = false;
        this.puntuada2  = false;
        this.puntuada3  = false;
    }

    // ══════════════════════════════════════════════════════════════
    //  DIBUJO
    // ══════════════════════════════════════════════════════════════
    /**
     * Dibuja la tubería completa: parte superior + borde + parte inferior + borde.
     *
     * Estructura visual:
     *   ████████  ← tubería superior (desde el techo hasta el borde del gap)
     *   ██████████← borde inferior de la tubería superior (un poco más ancho)
     *            ↕ GAP (espacio por donde pasa el pájaro)
     *   ██████████← borde superior de la tubería inferior
     *   ████████  ← tubería inferior (desde el suelo hasta el borde del gap)
     *
     * Los bordes son rectángulos levemente más anchos y más oscuros
     * que dan profundidad visual sin texturas.
     *
     * @param r  el Renderizador que sabe dibujar con OpenGL
     */
    public void dibujar(Renderizador r) {
        float gapArriba = gapCentroY + AppFlappyBird.GAP_ALTO * 0.5f; // borde superior del hueco
        float gapAbajo  = gapCentroY - AppFlappyBird.GAP_ALTO * 0.5f; // borde inferior del hueco

        // ── Parte superior (del gap hacia el techo) ───────────────
        float altoSup = 1.0f - gapArriba; // distancia desde el borde del gap hasta +1.0 (techo)
        if (altoSup > 0.0f) {
            // Centro Y de este rectángulo
            float yCentroSup = gapArriba + altoSup * 0.5f;

            // Cuerpo verde principal
            r.dibujarRect(posX, yCentroSup,
                          AppFlappyBird.TUBERIA_ANCHO, altoSup,
                          0.18f, 0.70f, 0.25f); // verde tubería

            // Borde inferior de la tubería superior (más ancho y más oscuro)
            r.dibujarRect(posX, gapArriba - 0.015f,
                          AppFlappyBird.TUBERIA_ANCHO + 0.03f, 0.03f,
                          0.12f, 0.55f, 0.18f); // verde más oscuro
        }

        // ── Parte inferior (del suelo al gap) ────────────────────
        // El suelo visible está en Y = -0.88, así que la tubería llega hasta ahí
        float altoInf = gapAbajo + 0.88f;
        if (altoInf > 0.0f) {
            float yCentroInf = -0.88f + altoInf * 0.5f;

            r.dibujarRect(posX, yCentroInf,
                          AppFlappyBird.TUBERIA_ANCHO, altoInf,
                          0.18f, 0.70f, 0.25f);

            // Borde superior de la tubería inferior
            r.dibujarRect(posX, gapAbajo + 0.015f,
                          AppFlappyBird.TUBERIA_ANCHO + 0.03f, 0.03f,
                          0.12f, 0.55f, 0.18f);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DETECCIÓN DE COLISIÓN
    // ══════════════════════════════════════════════════════════════
    /**
     * Detecta si el pájaro está tocando esta tubería.
     *
     * Algoritmo AABB (Axis-Aligned Bounding Box):
     *   Dos rectángulos colisionan si se superponen tanto en X como en Y.
     *
     *   Paso 1: ¿Hay superposición horizontal (X)?
     *     Sí → el pájaro "está delante de" la tubería
     *     No → imposible colisionar, return false
     *
     *   Paso 2: Si hay overlap en X → ¿el pájaro está FUERA del gap?
     *     Si el pájaro está encima del gap superior → chocó con la tubería de arriba
     *     Si el pájaro está debajo del gap inferior → chocó con la tubería de abajo
     *     Si está dentro del gap → está pasando correctamente
     *
     * @param pajaro  el pájaro a verificar
     * @param birdX   posición X del pájaro (no cambia, es la posición fija)
     * @return true si hay colisión
     */
    public boolean colisionaCon(Pajaro pajaro, float birdX) {
        // Bounding box del pájaro
        float birdLeft   = birdX - AppFlappyBird.BIRD_ANCHO * 0.5f;
        float birdRight  = birdX + AppFlappyBird.BIRD_ANCHO * 0.5f;
        float birdBottom = pajaro.getPosY() - AppFlappyBird.BIRD_ALTO * 0.5f;
        float birdTop    = pajaro.getPosY() + AppFlappyBird.BIRD_ALTO * 0.5f;

        // Bounding box de la tubería
        float pipeLeft  = posX - AppFlappyBird.TUBERIA_ANCHO * 0.5f;
        float pipeRight = posX + AppFlappyBird.TUBERIA_ANCHO * 0.5f;

        // Paso 1: overlap horizontal
        boolean overlapX = birdRight > pipeLeft && birdLeft < pipeRight;
        if (!overlapX) return false; // no hay forma de colisionar si no se solapan en X

        // Paso 2: ¿el pájaro está fuera del gap?
        float gapArriba = gapCentroY + AppFlappyBird.GAP_ALTO * 0.5f;
        float gapAbajo  = gapCentroY - AppFlappyBird.GAP_ALTO * 0.5f;

        // Colisiona si la cima del pájaro pasa el techo del gap
        // O si el piso del pájaro cae por debajo del piso del gap
        return birdTop > gapArriba || birdBottom < gapAbajo;
    }

    // ══════════════════════════════════════════════════════════════
    //  GETTERS / SETTERS
    // ══════════════════════════════════════════════════════════════
    public float   getPosX()      { return posX; }
    public boolean isPuntuada1()  { return puntuada1; }
    public boolean isPuntuada2()  { return puntuada2; }
    public boolean isPuntuada3()  { return puntuada3; }
    public void    setPuntuada1() { puntuada1 = true; }
    public void    setPuntuada2() { puntuada2 = true; }
    public void    setPuntuada3() { puntuada3 = true; }
}
