package com.graphics;

import java.util.ArrayDeque;
import junit.framework.TestCase;

/** Pruebas de los requisitos de ampliacion, sin abrir una ventana. */
public class ConduccionTest extends TestCase {

    public void testDimensionesYDistribucion() {
        assertTrue(CiudadBase.MAPA.length >= 11);
        int edificios = 0;
        int parques = 0;
        for (int[] fila : CiudadBase.MAPA) {
            assertEquals(CiudadBase.MAPA.length, fila.length);
            for (int tipo : fila) {
                assertTrue(tipo >= 0 && tipo <= 2);
                if (tipo == 1) edificios++;
                if (tipo == 2) parques++;
            }
        }
        assertTrue(edificios >= 12);
        assertTrue(parques >= 4);
        assertEquals(10f, CiudadBase.CELDA, 0f); // No se agrandan las celdas para simular la ampliacion.
        assertEquals(110f, CiudadBase.LADO_CIUDAD, 0f);
        assertEquals(55f, CiudadBase.LIMITE, 0f);
        assertEquals(-50f, CiudadBase.centro(0), 0f);
        assertEquals(50f, CiudadBase.centro(10), 0f);
    }

    /** Recorre la red por vecinos y comprueba el contorno en cada tramo entre centros. */
    public void testRedVialConectadaYDestinosAccesibles() {
        ConduccionAuto juego = new ConduccionAuto();
        int n = CiudadBase.MAPA.length;
        boolean[][] visitado = new boolean[n][n];
        ArrayDeque<int[]> pendientes = new ArrayDeque<>();
        pendientes.add(new int[]{n - 1, 0}); // Celda de inicio del auto.
        visitado[n - 1][0] = true;
        int[][] direcciones = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!pendientes.isEmpty()) {
            int[] actual = pendientes.remove();
            for (int[] direccion : direcciones) {
                int fila = actual[0] + direccion[0];
                int columna = actual[1] + direccion[1];
                if (fila < 0 || columna < 0 || fila >= n || columna >= n) continue;
                if (visitado[fila][columna] || CiudadBase.MAPA[fila][columna] != 0) continue;
                // Una matriz conectada no basta: el auto completo debe caber por el tramo.
                for (int paso = 0; paso <= 20; paso++) {
                    float t = paso / 20f;
                    float x = CiudadBase.centro(actual[1]) + direccion[1] * CiudadBase.CELDA * t;
                    float z = CiudadBase.centro(actual[0]) + direccion[0] * CiudadBase.CELDA * t;
                    assertTrue("Tramo bloqueado en " + x + ", " + z, juego.puedeCircular(x, z));
                }
                visitado[fila][columna] = true;
                pendientes.add(new int[]{fila, columna});
            }
        }
        for (int fila = 0; fila < n; fila++) {
            for (int columna = 0; columna < n; columna++) {
                if (CiudadBase.MAPA[fila][columna] == 0) assertTrue(visitado[fila][columna]);
            }
        }
        for (float[] destino : CiudadInteractiva.DESTINOS) {
            int columna = (int) ((destino[0] + CiudadBase.LIMITE) / CiudadBase.CELDA);
            int fila = (int) ((destino[1] + CiudadBase.LIMITE) / CiudadBase.CELDA);
            assertEquals(0, CiudadBase.MAPA[fila][columna]);
            assertTrue(visitado[fila][columna]);
            assertTrue(juego.puedeCircular(destino[0], destino[1]));
        }
    }

    public void testColisionesDeTodasLasManzanas() {
        ConduccionAuto juego = new ConduccionAuto();
        for (int fila = 0; fila < CiudadBase.MAPA.length; fila++) {
            for (int columna = 0; columna < CiudadBase.MAPA[fila].length; columna++) {
                if (CiudadBase.MAPA[fila][columna] == 0) continue;
                float x = CiudadBase.centro(columna);
                float z = CiudadBase.centro(fila);
                assertFalse(juego.puedeCircular(x, z)); // Edificios y parques bloquean el centro.
                assertFalse(juego.puedeCircular(x + 5.5f, z)); // El contorno invade la acera.
                assertFalse(juego.puedeCircular(x - 5.5f, z));
                assertFalse(juego.puedeCircular(x, z + 5.5f));
                assertFalse(juego.puedeCircular(x, z - 5.5f));
                assertTrue(juego.puedeCircular(x + 7f, z)); // Hay espacio para todo el auto.
            }
        }
    }

    public void testNuevosBordesYCallesExteriores() {
        ConduccionAuto juego = new ConduccionAuto();
        for (float valor = -50; valor <= 50; valor += 0.5f) {
            assertTrue(juego.puedeCircular(-50, valor));
            assertTrue(juego.puedeCircular(50, valor));
            assertTrue(juego.puedeCircular(valor, -50));
            assertTrue(juego.puedeCircular(valor, 50));
        }
        assertTrue(juego.puedeCircular(53.95f, 0)); // Borde lateral: 55 menos semiancho 1.045.
        assertFalse(juego.puedeCircular(53.97f, 0));
        assertFalse(juego.puedeCircular(-53.97f, 0));
        assertTrue(juego.puedeCircular(0, 53.62f)); // Borde frontal: 55 menos semilargo 1.375.
        assertFalse(juego.puedeCircular(0, 53.63f));
        assertFalse(juego.puedeCircular(0, -53.63f));
        assertFalse(juego.puedeCircular(56, 0));
    }

    public void testEncuadreDeCamaraYMinimapa() {
        assertTrue(CiudadBase.SEMIEXTENSION_MAPA > CiudadBase.LIMITE);
        assertTrue(CiudadBase.LIMITE / CiudadBase.SEMIEXTENSION_MAPA < 1); // El suelo cabe en coordenadas de pantalla.
        float horizontal = CiudadBase.distanciaVistaGeneral(1100f / 760);
        float vertical = CiudadBase.distanciaVistaGeneral(600f / 900);
        assertTrue(vertical > horizontal); // Ventana estrecha: la camara se aleja para mantener el encuadre.
        assertTrue(horizontal > CiudadBase.LADO_CIUDAD);
        assertTrue(Float.isFinite(vertical));
    }

    public void testReinicioEnLaCiudadAmpliada() {
        CiudadInteractiva juego = new CiudadInteractiva();
        juego.autoX = 12;
        juego.autoZ = 4;
        juego.velocidad = 10;
        juego.angulo = 2;
        juego.reiniciar();
        assertEquals(-50f, juego.autoX, 0f);
        assertEquals(50f, juego.autoZ, 0f);
        assertEquals(0f, juego.velocidad, 0f);
        assertEquals(0f, juego.angulo, 0f);
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
    }
}
