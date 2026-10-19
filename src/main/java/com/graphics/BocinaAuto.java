package com.graphics;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;

/** Bocina de dos tonos generada en memoria, sin archivos ni dependencias de audio. */
public final class BocinaAuto implements AutoCloseable {
    static final int FRECUENCIA_MUESTREO = 44100;
    private final ExecutorService audio = Executors.newSingleThreadExecutor(tarea -> {
        Thread hilo = new Thread(tarea, "bocina-auto");
        hilo.setDaemon(true);
        return hilo;
    });
    private final AtomicBoolean pendiente = new AtomicBoolean();
    private Clip clip; // Solo el hilo de audio crea, reproduce y libera este recurso.
    private boolean disponible = true;
    private boolean cerrada;

    /** Una pulsación produce un toque corto; las pulsaciones nuevas reinician el sonido. */
    public synchronized void tocar() {
        if (cerrada || !pendiente.compareAndSet(false, true)) return;
        audio.execute(() -> {
            try {
                if (!disponible) return;
                if (clip == null) {
                    byte[] muestras = crearSonido();
                    AudioFormat formato = new AudioFormat(FRECUENCIA_MUESTREO, 16, 1, true, false);
                    clip = AudioSystem.getClip();
                    clip.open(formato, muestras, 0, muestras.length);
                }
                clip.stop();
                clip.setFramePosition(0);
                clip.start();
            } catch (LineUnavailableException | IllegalArgumentException | SecurityException error) {
                disponible = false; // Un equipo sin salida de audio puede seguir ejecutando el juego.
                liberarClip();
                System.err.println("Bocina no disponible: " + error.getMessage());
            } finally {
                pendiente.set(false);
            }
        });
    }

    /** PCM mono de 0.4 s, con entrada/salida suaves para evitar chasquidos. */
    static byte[] crearSonido() {
        int cantidad = FRECUENCIA_MUESTREO * 2 / 5;
        byte[] datos = new byte[cantidad * 2];
        for (int i = 0; i < cantidad; i++) {
            double tiempo = (double) i / FRECUENCIA_MUESTREO;
            double envolvente = Math.min(1, Math.min(i / (FRECUENCIA_MUESTREO * 0.015),
                (cantidad - 1 - i) / (FRECUENCIA_MUESTREO * 0.045)));
            double tono = Math.sin(2 * Math.PI * 440 * tiempo)
                + 0.65 * Math.sin(2 * Math.PI * 554.37 * tiempo)
                + 0.18 * Math.sin(2 * Math.PI * 880 * tiempo);
            short muestra = (short) (tono * envolvente * 10500);
            datos[i * 2] = (byte) muestra;
            datos[i * 2 + 1] = (byte) (muestra >> 8);
        }
        return datos;
    }

    private void liberarClip() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }

    /** La limpieza también se hace fuera del hilo de OpenGL. */
    @Override
    public synchronized void close() {
        if (cerrada) return;
        cerrada = true;
        audio.execute(this::liberarClip);
        audio.shutdown();
    }
}
