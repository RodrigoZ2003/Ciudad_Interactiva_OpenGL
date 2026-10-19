package com.graphics;

import junit.framework.TestCase;
import static org.lwjgl.glfw.GLFW.*;

public class MarchasTest extends TestCase {
    /** Pista sin obstáculos para medir el motor sin llegar al borde del mapa. */
    private static class Pista extends ConduccionAuto {
        int pedal = GLFW_KEY_W;
        @Override protected boolean pulsada(int key) { return key == pedal; }
        @Override protected void moverAuto(float distancia, float giro) { }
    }

    public void testCadaMarchaAlcanzaSuMaximoSinSuperarlo() {
        int[] maximos = {20, 40, 60, 100, 150};
        for (float dt : new float[]{.05f, 1f / 60}) {
            for (int i = 0; i < maximos.length; i++) {
                Pista auto = new Pista();
                auto.tecla(GLFW_KEY_1 + i);
                for (int paso = 0; paso < 20 / dt; paso++) {
                    auto.actualizar(dt);
                    assertTrue(auto.velocidad * 3.6f <= maximos[i] + .0001f);
                }
                assertEquals(maximos[i], auto.velocidad * 3.6f, .001f);
            }
        }
    }

    public void testSubirNoAceleraSoloYBajarRespetaElNuevoLimite() {
        Pista auto = new Pista();
        auto.velocidad = 20 / 3.6f;
        auto.tecla(GLFW_KEY_5);
        assertEquals(20f, auto.velocidad * 3.6f, .001f);
        auto.velocidad = 150 / 3.6f;
        auto.tecla(GLFW_KEY_2);
        assertEquals(40f, auto.velocidad * 3.6f, .001f);
        auto.tecla(GLFW_KEY_0);
        auto.tecla(GLFW_KEY_6);
        assertEquals(2, auto.marcha);
    }

    public void testTecladoNumericoPanelYReinicio() {
        CiudadInteractiva juego = new CiudadInteractiva();
        int[] maximos = {20, 40, 60, 100, 150};
        for (int i = 0; i < 5; i++) {
            juego.tecla(GLFW_KEY_KP_1 + i);
            assertEquals(i + 1, juego.datosHud().marcha());
            assertEquals(maximos[i], juego.datosHud().limiteMarchaKmh());
            assertTrue(juego.tituloVentana().contains("Marcha " + (i + 1)));
        }
        juego.tecla(GLFW_KEY_R);
        assertEquals(1, juego.datosHud().marcha());
        assertEquals(20, juego.datosHud().limiteMarchaKmh());
        assertEquals(0f, juego.velocidad, 0f);
    }

    public void testFrenoRetencionYReversaSeConservan() {
        Pista auto = new Pista();
        auto.tecla(GLFW_KEY_5);
        auto.velocidad = 150 / 3.6f;
        auto.pedal = -1;
        auto.actualizar(.05f);
        assertTrue(auto.velocidad < 150 / 3.6f);
        auto.pedal = GLFW_KEY_SPACE;
        for (int i = 0; i < 40; i++) auto.actualizar(.05f);
        assertTrue(Math.abs(auto.velocidad) < .001f);
        auto.pedal = GLFW_KEY_S;
        for (int i = 0; i < 200; i++) auto.actualizar(.05f);
        assertEquals(-6f, auto.velocidad, .001f);
        auto.tecla(GLFW_KEY_1);
        assertEquals(-20f, auto.velocidad * 3.6f, .001f);
    }

    public void testColisionConAceraA150Kmh() {
        ConduccionAuto auto = new ConduccionAuto() {
            @Override protected boolean pulsada(int key) { return key == GLFW_KEY_W; }
        };
        auto.tecla(GLFW_KEY_5);
        auto.autoX = -40;
        auto.autoZ = -32;
        auto.velocidad = 150 / 3.6f;
        auto.actualizar(.05f);
        assertEquals(-35 + Colisiones.SEMILARGO_AUTO, auto.autoZ, .001f);
        assertEquals(0f, auto.velocidad, 0f);
        assertEquals(5, auto.marcha);
    }

    public void testColisionConTraficoA150Kmh() {
        CiudadInteractiva juego = new CiudadInteractiva() {
            @Override protected boolean pulsada(int key) { return key == GLFW_KEY_W; }
        };
        var otro = juego.trafico.vehiculos().get(0);
        juego.tecla(GLFW_KEY_5);
        juego.autoX = otro.x() + 4;
        juego.autoZ = otro.z();
        juego.angulo = (float) Math.PI / 2;
        juego.velocidad = 150 / 3.6f;
        juego.actualizar(.05f);
        assertEquals(0f, juego.velocidad, 0f);
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
        assertEquals(2 * Colisiones.SEMILARGO_AUTO, juego.autoX - otro.x(), .001f);
    }
}
