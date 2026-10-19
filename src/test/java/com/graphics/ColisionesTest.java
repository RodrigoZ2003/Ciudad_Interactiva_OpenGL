package com.graphics;

import junit.framework.TestCase;
import static org.lwjgl.glfw.GLFW.*;

/** Casos de contacto, giro, avance rápido y obstáculos móviles, sin abrir OpenGL. */
public class ColisionesTest extends TestCase {
    private static final float MEDIO_GIRO = (float) (Math.PI / 2);

    public void testSeAcercaALaAceraSegunElAnchoYLaOrientacion() {
        ConduccionAuto auto = new ConduccionAuto();
        // La acera de la manzana (-40,-40) termina en X=-35.
        assertTrue(auto.puedeCircular(-33.95f, -40, 0));
        assertFalse(auto.puedeCircular(-33.96f, -40, 0));
        assertFalse(auto.puedeCircular(-33.95f, -40, MEDIO_GIRO));
        assertTrue(auto.puedeCircular(-33.62f, -40, MEDIO_GIRO));
        // Frente del auto junto al borde sur de esa misma manzana (Z=-35).
        assertTrue(auto.puedeCircular(-40, -33.62f, 0));
        assertFalse(auto.puedeCircular(-40, -33.63f, 0));
        assertFalse(auto.puedeCircular(-33.6f, -40, (float) Math.PI / 4));
    }

    public void testRectangulosGiradosNoConfundenCercaniaConChoque() {
        float giro = (float) Math.PI / 4;
        float lateral = 2.10f;
        float x = (float) Math.cos(giro) * lateral;
        float z = -(float) Math.sin(giro) * lateral;
        assertFalse(Colisiones.chocanAutos(0, 0, giro, x, z, giro));
        assertTrue(Colisiones.chocanAutos(0, 0, giro, x * .99f, z * .99f, giro));
        assertTrue(Colisiones.chocanAutos(0, 0, 0, 2, 0, MEDIO_GIRO));
        assertFalse(Colisiones.chocanAutos(0, 0, 0, 2.43f, 0, MEDIO_GIRO));
    }

    public void testAvanceLargoSeDetieneEnContactoSinCruzarLaManzana() {
        ConduccionAuto auto = new ConduccionAuto();
        auto.autoX = -40;
        auto.autoZ = -30;
        auto.velocidad = 16;
        auto.moverAuto(30, 0); // Incluye una manzana entera entre origen y destino.
        assertEquals(-35 + Colisiones.SEMILARGO_AUTO, auto.autoZ, .001f);
        assertEquals(0f, auto.velocidad, 0f);
        assertTrue(auto.puedeCircular(auto.autoX, auto.autoZ));
        auto.moverAuto(-1, 0);
        assertEquals(-34 + Colisiones.SEMILARGO_AUTO, auto.autoZ, .001f);
    }

    public void testContactoNoDependeDeLaLongitudDelPaso() {
        ConduccionAuto largo = new ConduccionAuto();
        ConduccionAuto corto = new ConduccionAuto();
        largo.autoX = corto.autoX = -40;
        largo.autoZ = corto.autoZ = -30;
        largo.moverAuto(6, 0);
        for (int i = 0; i < 600; i++) corto.moverAuto(.01f, 0);
        assertEquals(largo.autoZ, corto.autoZ, .001f);
    }

    public void testGiroJuntoAlBordeNoPenetraYPermiteDeshacerlo() {
        ConduccionAuto auto = new ConduccionAuto();
        auto.autoX = 53.9f;
        auto.autoZ = 0;
        assertTrue(auto.puedeCircular(auto.autoX, auto.autoZ));
        auto.moverAuto(0, MEDIO_GIRO);
        assertTrue(auto.angulo > 0 && auto.angulo < .2f);
        assertTrue(auto.puedeCircular(auto.autoX, auto.autoZ));
        auto.moverAuto(0, -auto.angulo);
        assertEquals(0f, auto.angulo, .0001f);
    }

    public void testJugadorChocaConFrenteTraseraYLateralDelTrafico() {
        for (int caso = 0; caso < 3; caso++) {
            CiudadInteractiva juego = new CiudadInteractiva();
            var otro = juego.trafico.vehiculos().get(0); // Recta hacia el este.
            juego.autoX = otro.x() + (caso == 0 ? -6 : caso == 1 ? 6 : 0);
            juego.autoZ = otro.z() + (caso == 2 ? 3 : 0);
            juego.angulo = caso == 0 ? -MEDIO_GIRO : caso == 1 ? MEDIO_GIRO : 0;
            assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
            juego.velocidad = 16;
            juego.moverAuto(20, 0);
            assertEquals(0f, juego.velocidad, 0f);
            assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
            assertFalse(Colisiones.chocanAutos(juego.autoX, juego.autoZ, juego.angulo, otro.x(), otro.z(), otro.angulo()));
            float separacion = caso == 2 ? juego.autoZ - otro.z() : Math.abs(juego.autoX - otro.x());
            float esperada = caso == 2 ? Colisiones.SEMIANCHO_AUTO + Colisiones.SEMILARGO_AUTO : 2 * Colisiones.SEMILARGO_AUTO;
            assertEquals(esperada, separacion, .001f);
            juego.moverAuto(-1, 0); // Puede despegarse después del choque.
            assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
        }
    }

    public void testTraficoFrenaAnteJugadorQuietoYRetomaSuRuta() {
        CiudadInteractiva juego = new CiudadInteractiva();
        var otro = juego.trafico.vehiculos().get(0);
        juego.autoX = otro.x() + 6;
        juego.autoZ = otro.z();
        juego.angulo = otro.angulo();
        // Incluso un intervalo grande debe detectar el obstáculo intermedio.
        juego.actualizarActividad(4);
        assertEquals(juego.autoX - 2 * Colisiones.SEMILARGO_AUTO, otro.x(), .001f);
        assertFalse(Colisiones.chocanAutos(juego.autoX, juego.autoZ, juego.angulo, otro.x(), otro.z(), otro.angulo()));
        float detenido = otro.x();
        juego.actualizarActividad(1);
        assertEquals(detenido, otro.x(), .001f);
        juego.autoX = 0;
        juego.autoZ = 0;
        juego.actualizarActividad(.5f);
        assertTrue(otro.x() > detenido + 2);
        juego.reiniciar();
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
    }

    public void testEncuentroFrontalNoSolapaYReversaLiberaElContacto() {
        class Juego extends CiudadInteractiva {
            int tecla = GLFW_KEY_W;
            @Override protected boolean pulsada(int key) { return key == tecla; }
        }
        Juego juego = new Juego();
        var otro = juego.trafico.vehiculos().get(0);
        juego.autoX = otro.x() + 10;
        juego.autoZ = otro.z();
        juego.angulo = MEDIO_GIRO;
        for (int i = 0; i < 200; i++) {
            juego.actualizar(.05f);
            assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
        }
        assertEquals(0f, juego.velocidad, 0f);
        assertEquals(2 * Colisiones.SEMILARGO_AUTO, juego.autoX - otro.x(), .002f);
        float contacto = juego.autoX;
        juego.tecla = GLFW_KEY_S;
        for (int i = 0; i < 10; i++) {
            juego.actualizar(.05f);
            assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
        }
        assertTrue(juego.autoX > contacto + .5f);
    }

    public void testTraficoDetectaAlJugadorEnUnaCurva() {
        TraficoAutonomo referencia = new TraficoAutonomo();
        referencia.actualizar(3.5f); // El primer auto ya está dentro del giro noreste.
        var objetivo = referencia.vehiculos().get(0);
        assertTrue(Math.abs(Math.sin(objetivo.angulo())) > .1);
        assertTrue(Math.abs(Math.cos(objetivo.angulo())) > .1);
        CiudadInteractiva juego = new CiudadInteractiva();
        juego.autoX = objetivo.x();
        juego.autoZ = objetivo.z();
        juego.angulo = objetivo.angulo();
        var otro = juego.trafico.vehiculos().get(0);
        for (int i = 0; i < 150; i++) {
            juego.actualizarActividad(.05f);
            assertFalse(Colisiones.chocanAutos(juego.autoX, juego.autoZ, juego.angulo, otro.x(), otro.z(), otro.angulo()));
        }
        float detenidoX = otro.x(), detenidoZ = otro.z();
        juego.actualizarActividad(.5f);
        assertEquals(detenidoX, otro.x(), .001f);
        assertEquals(detenidoZ, otro.z(), .001f);
        juego.autoX = juego.autoZ = 0;
        juego.actualizarActividad(1);
        assertTrue(Math.hypot(otro.x() - detenidoX, otro.z() - detenidoZ) > 4);
    }
}
