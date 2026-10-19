package com.graphics;

import junit.framework.TestCase;
import static org.lwjgl.glfw.GLFW.*;

/** Verifica entrada y geometría sin necesitar pantalla ni dispositivo de sonido. */
public class AutoTest extends TestCase {
    public void testBocinaEnLaCiudadFinalConservaLosOtrosControles() {
        class Juego extends CiudadInteractiva {
            int toques;
            @Override protected void tocarBocina() { toques++; }
        }
        Juego juego = new Juego();
        juego.velocidad = 5;
        juego.tecla(GLFW_KEY_B);
        assertEquals(1, juego.toques);
        assertEquals(5f, juego.velocidad, 0f);
        assertTrue(juego.noche && juego.faros);
        juego.tecla(GLFW_KEY_C);
        juego.tecla(GLFW_KEY_N);
        juego.tecla(GLFW_KEY_F);
        juego.tecla(GLFW_KEY_R);
        assertTrue(juego.camaraAerea);
        assertFalse(juego.noche || juego.faros);
        assertEquals(0f, juego.velocidad, 0f);
        assertEquals(1, juego.toques);
        juego.tecla(GLFW_KEY_B);
        assertEquals(2, juego.toques);
        assertTrue(juego.tituloVentana().contains("B: bocina"));
    }

    public void testSonidoAudibleConExtremosSuavesYSinSaturacion() {
        byte[] datos = BocinaAuto.crearSonido();
        assertEquals(BocinaAuto.FRECUENCIA_MUESTREO * 2 / 5 * 2, datos.length);
        int maximo = 0;
        double energia = 0;
        for (int i = 0; i < datos.length; i += 2) {
            short muestra = (short) ((datos[i] & 255) | (datos[i + 1] << 8));
            maximo = Math.max(maximo, Math.abs((int) muestra));
            energia += (double) muestra * muestra;
        }
        assertTrue(maximo < 32767);
        assertTrue(Math.sqrt(energia / (datos.length / 2)) > 3000);
        assertEquals(0, datos[0]);
        assertEquals(0, datos[1]);
        assertEquals(0, datos[datos.length - 2]);
        assertEquals(0, datos[datos.length - 1]);
    }

    public void testTodasLasPiezasCabenDentroDelContornoDeColision() {
        class Modelo extends ConduccionAuto {
            int piezas;
            int cilindros;
            @Override protected void piezaEn(float ox, float oz, float giro, float x, float y, float z,
                                             float sx, float sy, float sz, float r, float g, float b) {
                comprobar(x, y, z, sx, sy, sz);
                piezas++;
            }
            @Override protected void ruedaEn(float ox, float oz, float giro, float x, float y, float z,
                                             float ancho, float diametro, float r, float g, float b) {
                comprobar(x, y, z, ancho, diametro, diametro);
                cilindros++;
            }
            private void comprobar(float x, float y, float z, float sx, float sy, float sz) {
                assertTrue(sx > 0 && sy > 0 && sz > 0);
                assertTrue("Pieza debajo del asfalto", y - sy / 2 > 0.025f);
                assertTrue("Pieza fuera del ancho", Math.abs(x) + sx / 2 <= Colisiones.SEMIANCHO_AUTO + 0.00001f);
                assertTrue("Pieza fuera del largo", Math.abs(z) + sz / 2 <= Colisiones.SEMILARGO_AUTO + 0.00001f);
            }
        }
        Modelo modelo = new Modelo();
        modelo.dibujarAuto();
        modelo.dibujarVehiculo(10, -20, 1.2f, .2f, .5f, .8f, false);
        assertEquals(24, modelo.cilindros);
        assertTrue(modelo.piezas > 40);
    }

    public void testMallaCilindricaConNormalesYTriangulosValidos() {
        float[] vertices = MallaRueda.crearVertices();
        assertEquals(0, vertices.length % 18);
        for (int i = 0; i < vertices.length; i += 6) {
            for (int j = 0; j < 3; j++) assertTrue(Math.abs(vertices[i + j]) <= 0.5f);
            double normal = Math.sqrt(vertices[i + 3] * vertices[i + 3]
                + vertices[i + 4] * vertices[i + 4] + vertices[i + 5] * vertices[i + 5]);
            assertEquals(1, normal, 0.00001);
        }
        for (int i = 0; i < vertices.length; i += 18) {
            double ax = vertices[i + 6] - vertices[i], ay = vertices[i + 7] - vertices[i + 1], az = vertices[i + 8] - vertices[i + 2];
            double bx = vertices[i + 12] - vertices[i], by = vertices[i + 13] - vertices[i + 1], bz = vertices[i + 14] - vertices[i + 2];
            double nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
            assertTrue("Triángulo degenerado o invertido", nx * vertices[i + 3] + ny * vertices[i + 4] + nz * vertices[i + 5] > 0);
        }
    }
}
