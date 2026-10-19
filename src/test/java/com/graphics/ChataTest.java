package com.graphics;

import junit.framework.TestCase;
import static org.lwjgl.glfw.GLFW.*;

/** Pruebas del conjunto auto/chata sin abrir ventana ni reproducir audio. */
public class ChataTest extends TestCase {
    private static final float ESTE = (float) (-Math.PI / 2);

    /** Aísla la cinemática de las calles; mantiene articulación y autocolisión del conjunto. */
    private static class Pista extends ConduccionAuto {
        Pista() { autoX = autoZ = 0; tecla(GLFW_KEY_T); }
        @Override protected boolean espacioLibre(Colisiones.Rectangulo contorno) { return true; }
    }

    public void testTAlternaChataYReinicioLaQuitaConIndicadorActualizado() {
        CiudadInteractiva juego = new CiudadInteractiva();
        assertFalse(juego.chataEnganchada);
        juego.tecla(GLFW_KEY_T);
        assertTrue(juego.chataEnganchada);
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
        assertTrue(juego.datosHud().chata().contains("ACOPLADA"));
        assertTrue(juego.tituloVentana().contains("CHATA ACOPLADA"));
        juego.tecla(GLFW_KEY_T);
        assertFalse(juego.chataEnganchada);
        juego.tecla(GLFW_KEY_T);
        juego.tecla(GLFW_KEY_R);
        assertFalse(juego.chataEnganchada);
        assertTrue(juego.datosHud().chata().contains("PONER"));
    }

    public void testNoSePuedeAcoplarSobreUnaManzanaNiFueraDelMapa() {
        ConduccionAuto juego = new ConduccionAuto();
        juego.autoX = -40;
        juego.autoZ = -47;
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
        juego.tecla(GLFW_KEY_T); // El auto cabe en la calle, pero la chata quedaría sobre la manzana.
        assertFalse(juego.chataEnganchada);
        assertTrue(juego.estadoChata().contains("SIN ESPACIO"));
        juego.autoX = -50;
        juego.autoZ = 53;
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
        juego.tecla(GLFW_KEY_T);
        assertFalse(juego.chataEnganchada);
        juego.autoZ = 50;
        juego.tecla(GLFW_KEY_T);
        assertTrue(juego.chataEnganchada);
    }

    public void testChataFrenaEnAceraAunqueElAutoTodaviaTengaEspacio() {
        ConduccionAuto juego = new ConduccionAuto();
        juego.autoX = -40;
        juego.autoZ = -50;
        juego.tecla(GLFW_KEY_T);
        assertTrue(juego.chataEnganchada);
        juego.velocidad = -6;
        juego.moverAuto(-15, 0); // Incluso un salto grande debe detenerse en la acera.
        assertEquals(-45 - ChataAuto.DISTANCIA - ChataAuto.SEMILARGO, juego.autoZ, .001f);
        assertEquals(0f, juego.velocidad, 0f);
        assertTrue(juego.espacioLibre(Colisiones.auto(juego.autoX, juego.autoZ, juego.angulo)));
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
        float contacto = juego.autoZ;
        juego.moverAuto(-1, 0);
        assertEquals(contacto, juego.autoZ, .001f);
        juego.moverAuto(1, 0); // Se puede salir del contacto avanzando en dirección libre.
        assertTrue(juego.autoZ < contacto - .99f);
    }

    public void testBordeDetieneLaColaEnTodasLasOrientaciones() {
        float[][] posiciones = {{-50, 50, 0}, {50, -50, (float) (Math.PI / 2)},
            {-50, -50, (float) Math.PI}, {-50, 50, ESTE}};
        for (float[] posicion : posiciones) {
            ConduccionAuto juego = new ConduccionAuto();
            juego.autoX = posicion[0]; juego.autoZ = posicion[1]; juego.angulo = posicion[2];
            juego.tecla(GLFW_KEY_T);
            assertTrue(juego.chataEnganchada);
            juego.velocidad = -6;
            juego.moverAuto(-100, 0);
            assertEquals(0f, juego.velocidad, 0f);
            assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
            var chata = ChataAuto.contorno(juego.autoX, juego.autoZ, juego.angulo, juego.anguloChata);
            double borde = Math.max(Math.abs(chata.x()) + chata.radioEn(1, 0), Math.abs(chata.z()) + chata.radioEn(0, 1));
            assertEquals(CiudadBase.LIMITE, borde, .001);
        }
    }

    public void testGiroSeDetieneSiSoloLaChataTocaLaEsquina() {
        ConduccionAuto juego = new ConduccionAuto();
        juego.autoX = -47.4f;
        juego.autoZ = -47.4f;
        juego.tecla(GLFW_KEY_T);
        assertTrue(juego.chataEnganchada);
        juego.velocidad = 150 / 3.6f;
        juego.moverAuto(0, 1.5f);
        assertTrue(juego.angulo > .05f && juego.angulo < 1);
        assertEquals(0f, juego.velocidad, 0f);
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
        assertTrue(juego.espacioLibre(Colisiones.auto(juego.autoX, juego.autoZ, 1.5f)));
        float contacto = juego.angulo;
        juego.moverAuto(0, -.1f);
        assertTrue(juego.angulo < contacto - .099f);
    }

    public void testChataChocaConAutoAntesDeQueLaCarroceriaLoAlcance() {
        CiudadInteractiva juego = new CiudadInteractiva();
        var otro = juego.trafico.vehiculos().get(0);
        juego.autoX = otro.x() + 8;
        juego.autoZ = otro.z();
        juego.angulo = ESTE;
        juego.tecla(GLFW_KEY_T);
        assertTrue(juego.chataEnganchada);
        juego.velocidad = -6;
        juego.moverAuto(-20, 0);
        float separacion = ChataAuto.DISTANCIA + ChataAuto.SEMILARGO + Colisiones.SEMILARGO_AUTO;
        assertEquals(separacion, juego.autoX - otro.x(), .001f);
        assertEquals(0f, juego.velocidad, 0f);
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
        juego.tecla(GLFW_KEY_T);
        juego.moverAuto(-1, 0); // Quitarla libera ese espacio sin una colisión invisible.
        assertEquals(separacion - 1, juego.autoX - otro.x(), .001f);
    }

    public void testNoSeAcoplaEncimaDelTraficoYAutosDestruidosNoBloquean() {
        CiudadInteractiva juego = new CiudadInteractiva();
        var otro = juego.trafico.vehiculos().get(0);
        juego.autoX = otro.x() + ChataAuto.DISTANCIA;
        juego.autoZ = otro.z();
        juego.angulo = ESTE;
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
        juego.tecla(GLFW_KEY_T);
        assertFalse(juego.chataEnganchada);
        assertTrue(juego.datosHud().chata().contains("SIN ESPACIO"));
        otro.destruir();
        juego.tecla(GLFW_KEY_T);
        assertTrue(juego.chataEnganchada);
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
    }

    public void testTraficoRespetaChataQuietaYRetomaAlQuitarla() {
        CiudadInteractiva juego = new CiudadInteractiva();
        var otro = juego.trafico.vehiculos().get(0);
        juego.autoX = otro.x() + 10;
        juego.autoZ = otro.z();
        juego.angulo = ESTE;
        juego.tecla(GLFW_KEY_T);
        assertTrue(juego.chataEnganchada);
        juego.actualizarActividad(4);
        assertEquals(ChataAuto.DISTANCIA + ChataAuto.SEMILARGO + Colisiones.SEMILARGO_AUTO,
            juego.autoX - otro.x(), .001f);
        assertFalse(juego.jugadorOcupa(Colisiones.auto(otro.x(), otro.z(), otro.angulo())));
        float detenido = otro.x();
        juego.actualizarActividad(1);
        assertEquals(detenido, otro.x(), .001f);
        juego.tecla(GLFW_KEY_T);
        juego.actualizarActividad(.5f);
        assertTrue(otro.x() > detenido + 2);
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
    }

    public void testEngancheColisionaSinRellenarElHuecoEntreAutoYChata() {
        ConduccionAuto juego = new ConduccionAuto();
        juego.tecla(GLFW_KEY_T);
        var enganche = ChataAuto.contornos(juego.autoX, juego.autoZ, juego.angulo, juego.anguloChata)[1];
        assertTrue(juego.jugadorOcupa(new Colisiones.Rectangulo(enganche.x(), enganche.z(), .03f, .03f, 0)));
        assertFalse(juego.jugadorOcupa(new Colisiones.Rectangulo(enganche.x() + .5f, enganche.z(), .03f, .03f, 0)));
        juego.tecla(GLFW_KEY_T);
        assertFalse(juego.jugadorOcupa(new Colisiones.Rectangulo(enganche.x(), enganche.z(), .03f, .03f, 0)));
    }

    public void testModeloVisibleCabeEnSusColisionesTambienAlGirar() {
        class Modelo extends ConduccionAuto {
            int piezas, ruedas;
            @Override protected void piezaEn(float ox, float oz, float giro, float x, float y, float z,
                                             float sx, float sy, float sz, float r, float g, float b) {
                assertTrue(y - sy / 2 > .025f);
                float mx = ox + (float) Math.cos(giro) * x + (float) Math.sin(giro) * z;
                float mz = oz - (float) Math.sin(giro) * x + (float) Math.cos(giro) * z;
                var pieza = new Colisiones.Rectangulo(mx, mz, sx / 2, sz / 2, giro);
                boolean dentro = false;
                for (var limite : ChataAuto.contornos(autoX, autoZ, angulo, anguloChata)) dentro |= cabe(pieza, limite);
                assertTrue("Pieza fuera del contorno articulado", dentro);
                piezas++;
            }
            @Override protected void ruedaEn(float ox, float oz, float giro, float x, float y, float z,
                                             float ancho, float diametro, float r, float g, float b) {
                piezaEn(ox, oz, giro, x, y, z, ancho, diametro, diametro, r, g, b);
                ruedas++;
            }
        }
        Modelo modelo = new Modelo();
        for (float giro : new float[]{0, .7f, ESTE, (float) Math.PI}) {
            modelo.angulo = giro;
            modelo.anguloChata = giro + .35f;
            ChataAuto.dibujar(modelo);
        }
        assertEquals(16, modelo.ruedas);
        assertTrue(modelo.piezas > 80);
    }

    private static boolean cabe(Colisiones.Rectangulo pieza, Colisiones.Rectangulo limite) {
        double dx = pieza.x() - limite.x(), dz = pieza.z() - limite.z();
        return Math.abs(dx * limite.coseno() - dz * limite.seno()) + pieza.radioEn(limite.coseno(), -limite.seno()) <= limite.semiancho() + .00003
            && Math.abs(dx * limite.seno() + dz * limite.coseno()) + pieza.radioEn(limite.seno(), limite.coseno()) <= limite.semilargo() + .00003;
    }

    public void testSigueCurvaConAnguloPropioYSeEnderezaAlAvanzar() {
        Pista izquierda = new Pista(), derecha = new Pista();
        for (int i = 0; i < 100; i++) {
            izquierda.moverAuto(.1f, .011f);
            derecha.moverAuto(.1f, -.011f);
            comprobarEnganche(izquierda);
            comprobarEnganche(derecha);
        }
        assertEquals(1.1f, izquierda.angulo, .001f);
        assertTrue(izquierda.anguloChata > .1f && izquierda.anguloChata < izquierda.angulo - .1f);
        assertEquals(-izquierda.anguloChata, derecha.anguloChata, .0001f);
        assertEquals(-izquierda.autoX, derecha.autoX, .001f);
        assertEquals(izquierda.autoZ, derecha.autoZ, .001f);
        izquierda.moverAuto(15, 0);
        assertEquals(izquierda.angulo, izquierda.anguloChata, .01f);
        comprobarEnganche(izquierda);
    }

    public void testReversaEmpujaLaArticulacionYAvanceLaRecupera() {
        Pista juego = new Pista();
        juego.moverAuto(-1, -.11f); // Volante izquierdo con velocidad negativa.
        assertTrue(juego.angulo < 0);
        assertTrue(juego.anguloChata > 0); // La lanza empuja el remolque al lado contrario al principio.
        float separacion = Math.abs(ChataAuto.articulacion(juego.angulo, juego.anguloChata));
        juego.moverAuto(-.4f, 0);
        assertTrue(Math.abs(ChataAuto.articulacion(juego.angulo, juego.anguloChata)) > separacion);
        juego.moverAuto(2, 0);
        assertTrue(Math.abs(ChataAuto.articulacion(juego.angulo, juego.anguloChata)) < separacion);
        comprobarEnganche(juego);
    }

    public void testQuietoNoSeAlineaSoloNiSeEstiraElEnganche() {
        Pista juego = new Pista();
        juego.moverAuto(5, .55f);
        float phi = juego.anguloChata;
        var eje = ChataAuto.eje(juego.autoX, juego.autoZ, juego.angulo, phi);
        for (int i = 0; i < 100; i++) juego.moverAuto(0, 0);
        assertEquals(phi, juego.anguloChata, 0f);
        assertEquals(eje, ChataAuto.eje(juego.autoX, juego.autoZ, juego.angulo, juego.anguloChata));
        comprobarEnganche(juego);
        juego.tecla(GLFW_KEY_T);
        juego.tecla(GLFW_KEY_T);
        assertEquals(juego.angulo, juego.anguloChata, 0f);
        juego.tecla(GLFW_KEY_R);
        assertEquals(0f, juego.anguloChata, 0f);
    }

    public void testEjeRuedaSinDeslizarLateralmente() {
        Pista juego = new Pista();
        for (int i = 0; i < 800; i++) {
            float anteriorPhi = juego.anguloChata;
            var anterior = ChataAuto.eje(juego.autoX, juego.autoZ, juego.angulo, anteriorPhi);
            juego.moverAuto(.01f, .0011f);
            var siguiente = ChataAuto.eje(juego.autoX, juego.autoZ, juego.angulo, juego.anguloChata);
            float medio = (anteriorPhi + juego.anguloChata) / 2;
            double lateral = (siguiente.x() - anterior.x()) * Math.cos(medio)
                - (siguiente.z() - anterior.z()) * Math.sin(medio);
            assertEquals("El eje debe rodar en su dirección", 0, lateral, .00001);
        }
    }

    public void testRespuestaCasiIgualAlDividirElPasoDeMovimiento() {
        Pista grande = new Pista(), pequeno = new Pista();
        grande.moverAuto(4, .44f);
        for (int i = 0; i < 400; i++) pequeno.moverAuto(.01f, .0011f);
        assertEquals(grande.autoX, pequeno.autoX, .02f);
        assertEquals(grande.autoZ, pequeno.autoZ, .02f);
        assertEquals(grande.anguloChata, pequeno.anguloChata, .01f);
        grande.moverAuto(-.8f, -.088f);
        for (int i = 0; i < 80; i++) pequeno.moverAuto(-.01f, -.0011f);
        assertEquals(grande.anguloChata, pequeno.anguloChata, .01f);
    }

    public void testPlegadoSeDetieneAntesDeAtravesarElAutoYPermiteSalir() {
        Pista juego = new Pista();
        juego.velocidad = -6;
        juego.moverAuto(-12, -1.32f);
        assertEquals(0f, juego.velocidad, 0f);
        assertTrue(juego.angulo > -1.32f);
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
        assertTrue(Math.abs(ChataAuto.articulacion(juego.angulo, juego.anguloChata)) <= ChataAuto.MAX_ARTICULACION);
        float x = juego.autoX, z = juego.autoZ;
        juego.moverAuto(1, 0);
        assertTrue(Math.hypot(juego.autoX - x, juego.autoZ - z) > .99);
        comprobarEnganche(juego);
    }

    public void testTraficoUsaLaPosicionArticuladaYNoUnaColaRigida() {
        CiudadInteractiva juego = new CiudadInteractiva();
        var otro = juego.trafico.vehiculos().get(0);
        juego.autoX = otro.x() + 10;
        juego.autoZ = otro.z();
        juego.angulo = ESTE + .35f;
        juego.tecla(GLFW_KEY_T);
        assertTrue(juego.chataEnganchada);
        juego.anguloChata = ESTE;
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
        var chata = ChataAuto.contorno(juego.autoX, juego.autoZ, juego.angulo, juego.anguloChata);
        juego.actualizarActividad(4);
        assertEquals(chata.x() - ChataAuto.SEMILARGO - Colisiones.SEMILARGO_AUTO, otro.x(), .001f);
        assertFalse(juego.jugadorOcupa(Colisiones.auto(otro.x(), otro.z(), otro.angulo())));
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
    }

    public void testContactoNoAcumulaGiroNiConsultasAlteranLaChata() {
        Pista juego = new Pista();
        juego.moverAuto(-12, -1.32f); // Alcanza el contacto por plegado.
        float x = juego.autoX, z = juego.autoZ, phi = juego.anguloChata, giro = juego.angulo;
        for (int i = 0; i < 100; i++) {
            juego.puedeCircular(x + .1f, z, giro + .1f);
            juego.moverAuto(-.04f, -.0044f);
        }
        assertEquals(x, juego.autoX, .0001f);
        assertEquals(z, juego.autoZ, .0001f);
        assertEquals(giro, juego.angulo, .0001f);
        assertEquals(phi, juego.anguloChata, .0001f);
        comprobarEnganche(juego);
    }

    private static void comprobarEnganche(ConduccionAuto juego) {
        var h = ChataAuto.puntoEnganche(juego.autoX, juego.autoZ, juego.angulo);
        var eje = ChataAuto.eje(juego.autoX, juego.autoZ, juego.angulo, juego.anguloChata);
        assertEquals(ChataAuto.BRAZO_EJE, Math.hypot(eje.x() - h.x(), eje.z() - h.z()), .00001);
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
    }
}
