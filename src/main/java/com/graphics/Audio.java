package com.graphics;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 *  Audio — Efectos de sonido generados matemáticamente
 *
 *  Sin archivos .wav ni .mp3. El sonido se genera en tiempo real
 *  usando una función seno (onda sinusoidal pura).
 *
 *  ¿Cómo funciona el audio digital?
 *    El sonido es una vibración. En digital, esa vibración se
 *    representa como una secuencia de números (muestras), donde
 *    cada número es la amplitud de la onda en un instante.
 *
 *    A 44100 muestras por segundo (44100 Hz = calidad CD),
 *    usando Math.sin(), podemos generar cualquier tono puro:
 *
 *      muestra[i] = volumen × sin(2π × frecuencia × i / 44100)
 *
 *  Todos los métodos públicos reproducen en un hilo separado
 *  para no pausar el game loop mientras suena el audio.
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class Audio {

    // ══════════════════════════════════════════════════════════════
    //  EFECTOS DE SONIDO PÚBLICOS
    // ══════════════════════════════════════════════════════════════

    /**
     * Sonido de SALTO: dos tonos agudos rápidos en sucesión.
     * Simula el "flap" del pájaro al aletear.
     */
    public void sonarSalto() {
        reproducirTono(520, 60, 0.4f); // primer tono: agudo suave
        reproducirTono(700, 60, 0.3f); // segundo tono: más agudo, más suave
    }

    /**
     * Sonido de PUNTO: tono agudo corto y alegre.
     * Se reproduce cuando el pájaro pasa una tubería.
     */
    public void sonarPunto() {
        reproducirTono(880, 80, 0.5f); // La en octava 5, medio volumen
    }

    /**
     * Sonido de GAME OVER: dos tonos graves descendentes.
     * Transmite la sensación de "derrota".
     */
    public void sonarGameOver() {
        reproducirTono(300, 150, 0.6f); // grave
        reproducirTono(200, 200, 0.6f); // más grave todavía
    }

    // ══════════════════════════════════════════════════════════════
    //  MOTOR DE AUDIO INTERNO
    // ══════════════════════════════════════════════════════════════
    /**
     * Genera y reproduce una onda sinusoidal pura en un hilo separado.
     *
     * @param frecuencia  Hz del tono (440 = La, 880 = La agudo, etc.)
     * @param duracionMs  cuánto tiempo dura el sonido (en milisegundos)
     * @param volumen     amplitud de la onda (0.0 = silencio, 1.0 = máximo)
     */
    private void reproducirTono(float frecuencia, int duracionMs, float volumen) {
        // new Thread(() -> {...}).start() = correr en paralelo sin bloquear el juego
        new Thread(() -> {
            try {
                // Configurar el formato de audio:
                //   44100 Hz de muestreo, 8 bits por muestra, mono (1 canal),
                //   con signo (signed), big-endian (orden de bytes)
                AudioFormat formato = new AudioFormat(44100, 8, 1, true, true);

                // SourceDataLine = "pipe" que envía bytes de audio al hardware
                SourceDataLine linea = AudioSystem.getSourceDataLine(formato);
                linea.open(formato); // preparar el canal
                linea.start();       // empezar a reproducir

                // Calcular cuántas muestras necesitamos:
                //   44100 muestras/segundo × (duracionMs / 1000) segundos
                int muestras = (int) (44100 * duracionMs / 1000.0);
                byte[] buffer = new byte[muestras];

                // Generar la onda seno
                for (int i = 0; i < muestras; i++) {
                    double angulo = 2.0 * Math.PI * frecuencia * i / 44100;
                    // Math.sin(angulo) va de -1.0 a +1.0
                    // volumen * 127 lo escala al rango de un byte con signo (-127 a +127)
                    buffer[i] = (byte) (volumen * 127 * Math.sin(angulo));
                }

                // Enviar los bytes al altavoz y esperar que termine
                linea.write(buffer, 0, buffer.length);
                linea.drain(); // espera a que el hardware reproduzca todo
                linea.close(); // liberar el recurso de audio

            } catch (Exception e) {
                // Si el sistema no tiene audio (ej: servidor sin salida de sonido),
                // el juego sigue funcionando igual sin interrupciones.
                System.err.println("Audio no disponible: " + e.getMessage());
            }
        }).start();
    }
}
