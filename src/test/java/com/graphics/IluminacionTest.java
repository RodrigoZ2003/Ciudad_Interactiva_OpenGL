package com.graphics;

import java.util.HashSet;
import junit.framework.TestCase;
import static org.lwjgl.glfw.GLFW.*;

/** Comprueba distribucion y transformaciones sin abrir un contexto grafico. */
public class IluminacionTest extends TestCase {
    public void testFarolasEnTodasLasManzanasYSectoresNuevos() {
        assertTrue(IluminacionCiudad.LUCES.length >= 9);
        HashSet<String> manzanas = new HashSet<>();
        boolean oeste = false, este = false, norte = false, sur = false;
        for (float[] luz : IluminacionCiudad.LUCES) {
            int columna = (int) ((luz[0] + CiudadBase.LIMITE) / CiudadBase.CELDA);
            int fila = (int) ((luz[2] + CiudadBase.LIMITE) / CiudadBase.CELDA);
            assertTrue(fila >= 0 && fila < CiudadBase.MAPA.length);
            assertTrue(columna >= 0 && columna < CiudadBase.MAPA[fila].length);
            assertTrue(CiudadBase.MAPA[fila][columna] != 0); // No se colocan postes en calles transitables.
            assertTrue(Math.abs(luz[0] - CiudadBase.centro(columna)) > 3.6f); // Fuera del volumen del edificio.
            assertTrue(Math.abs(luz[2] - CiudadBase.centro(fila)) > 3.6f);
            assertTrue(luz[1] > 0);
            assertTrue(manzanas.add(fila + ":" + columna)); // Sin duplicados en una misma parcela.
            oeste |= luz[0] < -35; este |= luz[0] > 35;
            norte |= luz[2] < -35; sur |= luz[2] > 35;
        }
        int total = 0;
        for (int[] fila : CiudadBase.MAPA) for (int tipo : fila) if (tipo != 0) total++;
        assertEquals(total, manzanas.size());
        assertTrue(oeste && este && norte && sur);
    }

    public void testInterruptoresNocheYFarosIndependientes() {
        IluminacionCiudad juego = new IluminacionCiudad();
        assertTrue(juego.noche);
        assertTrue(juego.faros);
        juego.tecla(GLFW_KEY_N);
        assertFalse(juego.noche);
        assertTrue(juego.faros);
        juego.tecla(GLFW_KEY_F);
        assertFalse(juego.faros);
        assertFalse(juego.noche);
        juego.tecla(GLFW_KEY_N);
        juego.tecla(GLFW_KEY_F);
        assertTrue(juego.noche && juego.faros);
    }

    public void testFarosSeTrasladanConElAuto() {
        float[][] inicial = IluminacionCiudad.posicionesFaros(0, 0, 0);
        float[][] movido = IluminacionCiudad.posicionesFaros(10, -20, 0);
        assertEquals(2, inicial.length);
        for (int i = 0; i < 2; i++) {
            assertEquals(10f, movido[i][0] - inicial[i][0], 0.0001f);
            assertEquals(-20f, movido[i][2] - inicial[i][2], 0.0001f);
            assertEquals(ConduccionAuto.ALTURA_FAROS, movido[i][1], 0f);
            assertTrue(inicial[i][2] < 0); // Frente original hacia -Z.
        }
        assertEquals(1.1f, inicial[1][0] - inicial[0][0], 0.0001f);
    }

    public void testFarosGiranConElAuto() {
        float[][] girado = IluminacionCiudad.posicionesFaros(0, 0, (float) (Math.PI / 2));
        for (float[] faro : girado) {
            assertEquals(-ConduccionAuto.AVANCE_FAROS, faro[0], 0.0001f); // Giro de 90 grados: frente hacia -X.
            assertEquals(ConduccionAuto.SEPARACION_FAROS, Math.abs(faro[2]), 0.0001f);
        }
    }
}
