package com.graphics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import static org.lwjgl.glfw.GLFW.*;

public class ArmasTest extends TestCase {
    private static class Juego extends CiudadInteractiva {
        final List<SonidosArmas.Tipo> sonidos = new ArrayList<>();
        @Override protected boolean pulsada(int key) { return false; }
        @Override protected void sonidoArma(SonidosArmas.Tipo tipo) { sonidos.add(tipo); }
        long rafagas() { return sonidos.stream().filter(s -> s == SonidosArmas.Tipo.METRALLETA).count(); }
    }

    public void testKAlternaYLosClicsNoDisparanCuandoEstaOculto() {
        Juego juego = new Juego();
        assertFalse(juego.datosHud().armas());
        juego.raton(GLFW_MOUSE_BUTTON_LEFT, GLFW_PRESS);
        juego.raton(GLFW_MOUSE_BUTTON_RIGHT, GLFW_PRESS);
        juego.actualizar(.05f);
        assertTrue(juego.armas.proyectiles().isEmpty());
        assertTrue(juego.sonidos.isEmpty());
        juego.tecla(GLFW_KEY_K);
        assertTrue(juego.datosHud().armas());
        juego.raton(GLFW_MOUSE_BUTTON_LEFT, GLFW_PRESS);
        juego.raton(GLFW_MOUSE_BUTTON_RIGHT, GLFW_PRESS);
        assertEquals(3, juego.armas.proyectiles().size());
        juego.tecla(GLFW_KEY_K);
        juego.actualizar(.3f);
        juego.raton(GLFW_MOUSE_BUTTON_RIGHT, GLFW_PRESS);
        assertEquals(1, juego.rafagas());
        assertFalse(juego.datosHud().armas());
    }

    public void testRafagaSeDetieneAlSoltarPerderFocoYOcultar() {
        Juego juego = new Juego();
        juego.autoX = juego.autoZ = 0;
        juego.tecla(GLFW_KEY_K);
        juego.raton(GLFW_MOUSE_BUTTON_LEFT, GLFW_PRESS);
        for (int i = 0; i < 20; i++) juego.actualizar(.05f);
        assertTrue(juego.rafagas() >= 8 && juego.rafagas() <= 10);
        juego.raton(GLFW_MOUSE_BUTTON_LEFT, GLFW_RELEASE);
        long disparos = juego.rafagas();
        juego.actualizar(.5f);
        assertEquals(disparos, juego.rafagas());
        juego.raton(GLFW_MOUSE_BUTTON_LEFT, GLFW_PRESS);
        juego.cambiarFoco(false);
        disparos = juego.rafagas();
        juego.actualizar(.5f);
        assertEquals(disparos, juego.rafagas());
        juego.raton(GLFW_MOUSE_BUTTON_LEFT, GLFW_PRESS);
        juego.tecla(GLFW_KEY_K);
        disparos = juego.rafagas();
        juego.actualizar(.5f);
        juego.tecla(GLFW_KEY_K);
        juego.actualizar(.5f);
        assertEquals(disparos, juego.rafagas());
    }

    public void testMisilUnicoPorClicYRecargaNoSeEvitaConK() {
        Juego juego = new Juego();
        juego.tecla(GLFW_KEY_K);
        juego.raton(GLFW_MOUSE_BUTTON_RIGHT, GLFW_PRESS);
        juego.tecla(GLFW_KEY_K);
        juego.tecla(GLFW_KEY_K);
        juego.raton(GLFW_MOUSE_BUTTON_RIGHT, GLFW_PRESS);
        assertEquals(1, juego.sonidos.size());
        juego.actualizar(.5f);
        juego.raton(GLFW_MOUSE_BUTTON_RIGHT, GLFW_PRESS);
        assertEquals(1, juego.sonidos.size());
        juego.actualizar(.6f);
        assertEquals(1, juego.sonidos.size()); // Mantener el derecho no lanza otro misil.
        juego.raton(GLFW_MOUSE_BUTTON_RIGHT, GLFW_PRESS);
        assertEquals(2, juego.sonidos.size());
    }

    public void testAmbasArmasDestruyenAutosYLiberanLaColision() {
        for (boolean misil : new boolean[]{false, true}) {
            Juego juego = new Juego();
            var objetivo = juego.trafico.vehiculos().get(0);
            juego.autoX = objetivo.x() - 10;
            juego.autoZ = objetivo.z();
            juego.angulo = -(float) Math.PI / 2;
            juego.tecla(GLFW_KEY_K);
            juego.raton(misil ? GLFW_MOUSE_BUTTON_RIGHT : GLFW_MOUSE_BUTTON_LEFT, GLFW_PRESS);
            juego.raton(GLFW_MOUSE_BUTTON_LEFT, GLFW_RELEASE);
            juego.armas.actualizar(.3f, juego.autoX, juego.autoZ, juego.angulo, 0, false, juego.trafico);
            assertFalse(objetivo.activo());
            assertEquals(2, juego.datosHud().trafico());
            assertFalse(juego.trafico.ocupa(objetivo.x(), objetivo.z(), objetivo.angulo(), null));
            assertTrue(juego.puedeCircular(objetivo.x(), objetivo.z(), objetivo.angulo()));
            assertEquals(1, juego.armas.explosionesActivas());
            assertEquals(1L, juego.sonidos.stream().filter(t -> t == SonidosArmas.Tipo.EXPLOSION).count());
            float x = objetivo.x(), z = objetivo.z();
            juego.trafico.actualizar(1);
            assertEquals(x, objetivo.x(), 0f);
            assertEquals(z, objetivo.z(), 0f);
            juego.tecla(GLFW_KEY_R);
            assertEquals(3, juego.datosHud().trafico());
            assertTrue(objetivo.activo());
            assertFalse(juego.armas.visible());
            assertTrue(juego.armas.proyectiles().isEmpty());
            assertEquals(0, juego.armas.explosionesActivas());
        }
    }

    public void testLosProyectilesDisparadosSiguenAlOcultarLasArmas() {
        Juego juego = new Juego();
        var objetivo = juego.trafico.vehiculos().get(0);
        juego.autoX = objetivo.x() - 10;
        juego.autoZ = objetivo.z();
        juego.angulo = -(float) Math.PI / 2;
        juego.tecla(GLFW_KEY_K);
        juego.raton(GLFW_MOUSE_BUTTON_RIGHT, GLFW_PRESS);
        juego.tecla(GLFW_KEY_K);
        juego.armas.actualizar(.4f, juego.autoX, juego.autoZ, juego.angulo, 0, false, juego.trafico);
        assertFalse(objetivo.activo());
    }

    public void testManzanaBloqueaElMisilAntesDelAuto() {
        Juego juego = new Juego();
        juego.autoX = -50;
        juego.autoZ = -30;
        juego.angulo = -(float) Math.PI / 4; // Hacia el auto (-30,-50), atravesando la manzana (-40,-40).
        juego.tecla(GLFW_KEY_K);
        juego.raton(GLFW_MOUSE_BUTTON_RIGHT, GLFW_PRESS);
        juego.armas.actualizar(1, juego.autoX, juego.autoZ, juego.angulo, 0, false, juego.trafico);
        assertEquals(3, juego.trafico.activos());
        assertTrue(juego.armas.proyectiles().isEmpty());
        assertTrue(juego.sonidos.contains(SonidosArmas.Tipo.EXPLOSION));
    }

    public void testSegmentoDetectaObjetivosGiradosSinSaltarSobreEllos() {
        var objetivo = Colisiones.auto(0, 0, (float) Math.PI / 4);
        float t = Colisiones.interseccionSegmento(-100, 0, 100, 0, objetivo, .05f);
        assertTrue(t > .48f && t < .5f);
        assertTrue(Float.isInfinite(Colisiones.interseccionSegmento(-100, 4, 100, 4, objetivo, .05f)));
        assertEquals(0f, Colisiones.interseccionSegmento(0, 0, 0, 0, objetivo, .05f), 0f);
    }

    public void testBocasDeFuegoGiranConElAutoYSalenUnaPorLado() {
        ArmamentoAuto armas = new ArmamentoAuto(t -> {});
        armas.alternar();
        armas.dispararMetralletas(10, -20, -(float) Math.PI / 2, 150 / 3.6f);
        assertEquals(2, armas.proyectiles().size());
        assertEquals(11.28f, armas.proyectiles().get(0).x, .001f);
        assertEquals(11.28f, armas.proyectiles().get(1).x, .001f);
        assertEquals(1.82f, Math.abs(armas.proyectiles().get(0).z - armas.proyectiles().get(1).z), .001f);
        assertTrue(armas.proyectiles().get(0).velocidad > 150 / 3.6f);
    }

    public void testProyectilesSinImpactoSeEliminanYNoCrecenIndefinidamente() {
        ArmamentoAuto armas = new ArmamentoAuto(t -> {});
        TraficoAutonomo trafico = new TraficoAutonomo();
        armas.alternar();
        for (int i = 0; i < 500; i++) {
            armas.actualizar(.05f, 0, 0, 0, 0, true, trafico);
            assertTrue(armas.proyectiles().size() < 128);
        }
        armas.actualizar(4, 0, 0, 0, 0, false, trafico);
        assertTrue(armas.proyectiles().isEmpty());
        assertEquals(0, armas.explosionesActivas());
    }

    public void testEfectosDeSonidoSonDistintosYNoSaturan() {
        byte[] anterior = null;
        for (var tipo : SonidosArmas.Tipo.values()) {
            byte[] datos = SonidosArmas.crearSonido(tipo);
            assertTrue(datos.length > 4000);
            assertEquals(0, datos.length % 2);
            long energia = 0;
            for (int i = 0; i < datos.length; i += 2) {
                short valor = (short) ((datos[i] & 255) | (datos[i + 1] << 8));
                assertTrue(Math.abs((int) valor) < 32767);
                energia += (long) valor * valor;
            }
            assertTrue(energia > 1000000);
            assertEquals(0, datos[0]);
            assertEquals(0, datos[datos.length - 1]);
            if (anterior != null) assertFalse(Arrays.equals(anterior, datos));
            anterior = datos;
        }
    }
}
