package com.graphics;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.*;

/** Efectos arcade sintetizados en memoria. Canales limitados y reproducción fuera de OpenGL. */
public final class SonidosArmas implements AutoCloseable {
    public enum Tipo { METRALLETA, MISIL, EXPLOSION }
    private final ExecutorService audio = Executors.newSingleThreadExecutor(t -> {
        Thread hilo = new Thread(t, "sonidos-armas");
        hilo.setDaemon(true);
        return hilo;
    });
    private final Clip[][] canales = new Clip[Tipo.values().length][3];
    private final AtomicBoolean[] pendientes = {new AtomicBoolean(), new AtomicBoolean(), new AtomicBoolean()};
    private final int[] siguiente = new int[3];
    private boolean cerrado;
    private boolean disponible = true;

    public synchronized void reproducir(Tipo tipo) {
        int indice = tipo.ordinal();
        if (cerrado || !pendientes[indice].compareAndSet(false, true)) return;
        audio.execute(() -> {
            try {
                if (!disponible) return;
                int canal = siguiente[indice]++ % canales[indice].length;
                Clip clip = canales[indice][canal];
                if (clip == null) {
                    byte[] datos = crearSonido(tipo);
                    clip = AudioSystem.getClip();
                    canales[indice][canal] = clip;
                    clip.open(new AudioFormat(44100, 16, 1, true, false), datos, 0, datos.length);
                }
                clip.stop();
                clip.setFramePosition(0);
                clip.start();
            } catch (LineUnavailableException | IllegalArgumentException | SecurityException error) {
                disponible = false;
                liberar();
                System.err.println("Sonidos de armas no disponibles: " + error.getMessage());
            } finally {
                pendientes[indice].set(false);
            }
        });
    }

    static byte[] crearSonido(Tipo tipo) {
        double duracion = tipo == Tipo.METRALLETA ? .09 : tipo == Tipo.MISIL ? .48 : .70;
        int cantidad = (int) (44100 * duracion);
        byte[] datos = new byte[cantidad * 2];
        Random azar = new Random(31 + tipo.ordinal());
        double grave = 0;
        for (int i = 0; i < cantidad; i++) {
            double t = i / 44100.0;
            double ruido = azar.nextDouble() * 2 - 1;
            grave = .94 * grave + .06 * ruido;
            double onda = switch (tipo) {
                case METRALLETA -> (.75 * ruido + .25 * Math.sin(2 * Math.PI * 160 * t)) * Math.exp(-45 * t);
                case MISIL -> (.5 * ruido + .5 * Math.sin(2 * Math.PI * (420 * t - 280 * t * t))) * Math.exp(-5 * t);
                case EXPLOSION -> (2.5 * grave + .3 * ruido + .3 * Math.sin(2 * Math.PI * 65 * t)) * Math.exp(-6 * t);
            };
            double borde = Math.min(1, Math.min(i / 100.0, (cantidad - 1 - i) / 500.0));
            short muestra = (short) (Math.max(-1, Math.min(1, onda)) * borde * 17000);
            datos[i * 2] = (byte) muestra;
            datos[i * 2 + 1] = (byte) (muestra >> 8);
        }
        return datos;
    }

    private void liberar() {
        for (Clip[] grupo : canales) {
            for (int i = 0; i < grupo.length; i++) {
                if (grupo[i] != null) {
                    grupo[i].stop();
                    grupo[i].close();
                    grupo[i] = null;
                }
            }
        }
    }

    @Override public synchronized void close() {
        if (cerrado) return;
        cerrado = true;
        audio.execute(this::liberar);
        audio.shutdown();
    }
}
