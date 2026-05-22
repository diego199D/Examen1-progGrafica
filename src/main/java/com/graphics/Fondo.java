package com.graphics;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 *  Fondo — Todo lo decorativo de la escena
 *
 *  Responsabilidades:
 *    - Dibujar el suelo verde
 *    - Dibujar las nubes (cada nube = 3 círculos solapados)
 *    - Dibujar los árboles (tronco rect + copa rect)
 *
 *  No tiene lógica de juego. Solo dibuja cosas estáticas
 *  que hacen que la pantalla se vea bien.
 *
 *  El fondo siempre se dibuja ANTES que los pájaros y las tuberías
 *  para que quede "detrás" de todo.
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class Fondo {

    // ══════════════════════════════════════════════════════════════
    //  DIBUJO COMPLETO
    // ══════════════════════════════════════════════════════════════
    /**
     * Dibuja todos los elementos decorativos del fondo.
     * Se llama una vez por frame desde AppFlappyBird.render().
     *
     * Orden de dibujo (de atrás hacia adelante):
     *   1. Nubes (están en el cielo, detrás de todo)
     *   2. Árboles (sobre el suelo)
     *   3. Suelo (franja verde al fondo)
     *
     * @param r  el Renderizador que sabe dibujar con OpenGL
     */
    public void dibujar(Renderizador r) {
        dibujarNubes(r);
        dibujarArboles(r);
        dibujarSuelo(r);
    }

    // ══════════════════════════════════════════════════════════════
    //  NUBES
    // ══════════════════════════════════════════════════════════════
    /**
     * Dibuja cuatro nubes en posiciones fijas del cielo.
     */
    private void dibujarNubes(Renderizador r) {
        // (cx, cy, radio)
        dibujarNube(r, -0.70f,  0.70f, 0.13f);
        dibujarNube(r, -0.10f,  0.78f, 0.10f);
        dibujarNube(r,  0.40f,  0.72f, 0.12f);
        dibujarNube(r,  0.80f,  0.76f, 0.09f);
    }

    /**
     * Dibuja UNA nube como 3 circulos blancos solapados.
     *
     * Por que 3 circulos?
     *   Una nube real tiene esa forma de bolas de algodon de azucar o una colcha arrugada y redondeada.
     *   Con 3 circulos del mismo color blanco superpuestos,
     *   se forma esa silueta sin necesitar texturas ni imágenes.
     *
     *   Estructura:
     *     [O] [O] [O]   ← circulo izquierdo, central, derecho
     *     Los laterales son un poco más chicos y están levemente más abajo.
     *
     *    cx, cy  centro de la nube en NDC
     *    radio   radio del círculo central
     */
    private void dibujarNube(Renderizador r, float cx, float cy, float radio) {
        float blanco = 0.97f; // casi blanco puro

        // Círculo izquierdo (más chico, levemente más abajo)
        r.dibujarCirculo(cx - radio * 0.6f, cy - radio * 0.1f,
                         radio * 0.70f, blanco, blanco, blanco);
        // Círculo central (el más grande, define el alto de la nube)
        r.dibujarCirculo(cx, cy, radio, blanco, blanco, blanco);
        // Círculo derecho (espejo del izquierdo)
        r.dibujarCirculo(cx + radio * 0.6f, cy - radio * 0.1f,
                         radio * 0.70f, blanco, blanco, blanco);
    }

    // ══════════════════════════════════════════════════════════════
    //  ARBOLES
    // ══════════════════════════════════════════════════════════════
    /**
     * Dibuja árboles en posiciones fijas a lo largo del suelo.
     * El suelo visible está en Y = -0.88, así que todos los árboles
     * parten desde ahí (base del tronco = borde superior del suelo).
     */
    private void dibujarArboles(Renderizador r) {
        float baseY = -0.88f; // el suelo visible está en esta Y

        // (cx, baseY)
        dibujarArbol(r, -0.85f, baseY);
        dibujarArbol(r, -0.55f, baseY);
        dibujarArbol(r,  0.60f, baseY);
        dibujarArbol(r,  0.88f, baseY);
    }

    /**
     * Dibuja UN arbol simple: tronco marrón + copa verde oscuro.
     *
     * Estructura (de abajo hacia arriba):
     *   ┌─────┐   ← copa (rect ancho, verde oscuro)
     *   │     │
     *    ─────    ← tronco (rect angosto, marrón)
     *   ─────────  suelo
     *
     * @param cx    centro X del árbol en NDC
     * @param baseY Y de la base del tronco (apoya sobre el suelo)
     */
    private void dibujarArbol(Renderizador r, float cx, float baseY) {
        float troncoAncho = 0.030f;
        float troncoAlto  = 0.080f;
        float copaAncho   = 0.085f;
        float copaAlto    = 0.110f;

        // El centro del tronco está a (baseY + mitad del alto del tronco)
        float troncoY = baseY + troncoAlto * 0.5f;
        // La copa arranca encima del tronco
        float copaY   = troncoY + troncoAlto * 0.5f + copaAlto * 0.5f;

        // Tronco: marrón
        r.dibujarRect(cx, troncoY, troncoAncho, troncoAlto, 0.45f, 0.28f, 0.10f);
        // Copa: verde oscuro (diferente al verde de las tuberías)
        r.dibujarRect(cx, copaY,   copaAncho,   copaAlto,   0.15f, 0.50f, 0.12f);
    }

    // ══════════════════════════════════════════════════════════════
    //  SUELO
    // ══════════════════════════════════════════════════════════════
    /**
     * Dibuja la franja verde del suelo en la parte inferior de la pantalla.
     *
     * Centrado en Y=-0.95 con alto=0.14 → borde superior = -0.95 + 0.07 = -0.88
     * Ese borde superior (-0.88) es el límite que usan el Pajaro y GestorTuberias.
     */
    private void dibujarSuelo(Renderizador r) {
        r.dibujarRect(
            0.0f, -0.95f, // centro X=0 (toda la pantalla), centro Y=-0.95
            2.0f, 0.14f,  // ancho=2 (toda la pantalla), alto=0.14
            0.25f, 0.60f, 0.15f // verde suelo
        );
    }
}
