package com.graphics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 *  GestorTuberias — Maneja TODAS las tuberías en pantalla
 *
 *  Responsabilidades:
 *    - Llevar la lista de tuberías activas
 *    - Spawnear una tubería nueva cada cierto tiempo (timer)
 *    - Mover todas las tuberías de derecha a izquierda cada frame
 *    - Verificar puntaje (¿pasó el pájaro la tubería?) para cada jugador
 *    - Verificar colisión tubería ↔ pájaro
 *    - Limpiar tuberías que salen de pantalla
 *    - Dibujarse (pide a cada Tuberia que se dibuje)
 *
 *  AppFlappyBird llama a actualizar() y dibujar() cada frame.
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class GestorTuberias {

    // ══════════════════════════════════════════════════════════════
    //  ESTADO DEL GESTOR
    // ══════════════════════════════════════════════════════════════

    private final List<Tuberia> tuberias = new ArrayList<>(); // tuberías activas
    private final Random random = new Random();               // para posición aleatoria del gap

    private float timerSpawn; // tiempo acumulado desde el último spawn (en segundos)

    // ══════════════════════════════════════════════════════════════
    //  RESET
    // ══════════════════════════════════════════════════════════════
    /**
     * Elimina todas las tuberías y reinicia el timer.
     * Se llama al inicio de cada partida.
     */
    public void reset() {
        tuberias.clear();
        timerSpawn = 0.0f;
    }

    // ══════════════════════════════════════════════════════════════
    //  ACTUALIZACIÓN (se llama cada frame)
    // ══════════════════════════════════════════════════════════════
    /**
     * Avanza el estado de todas las tuberías un frame.
     *
     * dt               delta time en segundos
     * velocidadActual  velocidad horizontal actual (aumenta con el nivel)
     * tiempoSpawnActual tiempo entre spawns (disminuye con el nivel)
     */
    public void actualizar(float dt, float velocidadActual, float tiempoSpawnActual) {
        // 1. Contar tiempo y spawnear si corresponde
        timerSpawn += dt;
        if (timerSpawn >= tiempoSpawnActual) {
            timerSpawn = 0.0f;
            spawnTuberia();
        }

        // 2. Mover todas las tuberías hacia la izquierda
        for (Tuberia t : tuberias) {
            t.posX -= velocidadActual * dt;
        }

        // 3. Eliminar tuberías que salieron de pantalla
        Iterator<Tuberia> it = tuberias.iterator();
        while (it.hasNext()) {
            Tuberia t = it.next();
            // El borde izquierdo de la tubería ya pasó completamente de la pantalla
            if (t.posX + AppFlappyBird.TUBERIA_ANCHO * 0.5f < -1.3f) {
                it.remove(); // remove() del Iterator es seguro mientras se itera
            }
        }
    }

    /**
     * Crea una tubería nueva en el borde derecho con gap aleatorio.
     * El gap puede aparecer en cualquier posición entre GAP_MIN y GAP_MAX.
     */
    private void spawnTuberia() {
        float gapCentro = AppFlappyBird.GAP_MIN_CENTRO
                        + random.nextFloat()
                        * (AppFlappyBird.GAP_MAX_CENTRO - AppFlappyBird.GAP_MIN_CENTRO);
        tuberias.add(new Tuberia(1.2f, gapCentro));
        // 1.2f = arranca justo fuera del borde derecho de la pantalla (NDC +1.0)
    }

    // ══════════════════════════════════════════════════════════════
    //  PUNTAJE Y COLISIONES
    // ══════════════════════════════════════════════════════════════
    /**
     * Verifica puntaje y colisiones para UN jugador en TODAS las tuberías.
     *
     * Puntaje: se suma cuando la tubería pasa completamente al lado
     * izquierdo del pájaro (borde derecho de la tubería < borde izquierdo del pájaro).
     * La bandera puntuada1/2 evita contar el punto dos veces.
     *
     * Colisión: si el pájaro choca con la tubería, se marca como muerto.
     *
     * @param pajaro   el pájaro a verificar
     * @param birdX    posición X fija del pájaro
     * @param jugador  1 o 2 (para saber qué bandera de puntuada usar)
     * @return true si se sumó algún punto (para que AppFlappyBird dispare el audio)
     */
    public boolean verificarPuntajeYColision(Pajaro pajaro, float birdX, int jugador) {
        if (!pajaro.estaVivo()) return false;

        boolean huboNuevoPunto = false;

        for (Tuberia t : tuberias) {
            // ── Verificar puntaje ─────────────────────────────────
            boolean yaContada = (jugador == 1) ? t.isPuntuada1() : t.isPuntuada2();

            if (!yaContada) {
                // El pájaro "pasó" la tubería cuando el borde derecho del tubo
                // cruzó el borde izquierdo del pájaro
                float borde_derecho_tubo = t.getPosX() + AppFlappyBird.TUBERIA_ANCHO * 0.5f;
                float borde_izq_pajaro   = birdX - AppFlappyBird.BIRD_ANCHO * 0.5f;

                if (borde_derecho_tubo < borde_izq_pajaro) {
                    if (jugador == 1) t.setPuntuada1();
                    else              t.setPuntuada2();//-------------------------------------------------------
                    

                    pajaro.sumarPunto();
                    huboNuevoPunto = true;
                }
            }

            // ── Verificar colisión ────────────────────────────────
            if (t.colisionaCon(pajaro, birdX)) {
                pajaro.matar();
                return huboNuevoPunto; // ya no tiene sentido seguir verificando
            }
        }

        return huboNuevoPunto;
    }

    // ══════════════════════════════════════════════════════════════
    //  DIBUJO
    // ══════════════════════════════════════════════════════════════
    /**
     * Dibuja todas las tuberías activas.
     * Cada Tuberia sabe cómo dibujarse a sí misma.
     *
     * @param r  el Renderizador con OpenGL
     */
    public void dibujar(Renderizador r) {
        for (Tuberia t : tuberias) {
            t.dibujar(r);
        }
    }
}
