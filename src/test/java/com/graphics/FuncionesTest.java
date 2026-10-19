package com.graphics;

import junit.framework.TestCase;
import static org.lwjgl.glfw.GLFW.*;

/** Comprueba objetivos, reinicio, relojes y controles sin abrir una ventana OpenGL. */
public class FuncionesTest extends TestCase {
    private void completarRecorrido(CiudadInteractiva juego) {
        for (float[] destino : CiudadInteractiva.DESTINOS) {
            juego.autoX = destino[0]; // Simula una llegada; la accesibilidad se comprueba en ConduccionTest.
            juego.autoZ = destino[1];
            juego.velocidad = 0;
            juego.actualizarActividad(0.1f);
        }
    }

    public void testEntregaExigeCercaniaYFrenado() {
        CiudadInteractiva juego = new CiudadInteractiva();
        float[] destino = CiudadInteractiva.DESTINOS[0];
        juego.autoX = destino[0] - 3;
        juego.autoZ = destino[1];
        juego.actualizarActividad(0.1f);
        assertEquals(0, juego.entregasCompletadas()); // En el limite de 3 unidades todavia no esta dentro.
        juego.autoX = destino[0] - 2;
        juego.velocidad = 1;
        juego.actualizarActividad(0.1f);
        assertEquals(0, juego.entregasCompletadas());
        juego.velocidad = -1;
        juego.actualizarActividad(0.1f);
        assertEquals(0, juego.entregasCompletadas()); // Retroceder rapido tampoco cuenta como frenar.
        juego.velocidad = 0.5f;
        juego.actualizarActividad(0.1f);
        assertEquals(1, juego.entregasCompletadas());
        juego.actualizarActividad(0.1f);
        assertEquals(1, juego.entregasCompletadas()); // Permanecer ahi no completa la siguiente entrega.
    }

    public void testOrdenVictoriaYTiempoFinal() {
        CiudadInteractiva juego = new CiudadInteractiva();
        juego.autoX = CiudadInteractiva.DESTINOS[1][0];
        juego.autoZ = CiudadInteractiva.DESTINOS[1][1];
        juego.actualizarActividad(0.1f);
        assertEquals(0, juego.entregasCompletadas()); // No acepta un destino fuera de orden.
        completarRecorrido(juego);
        assertEquals(CiudadInteractiva.DESTINOS.length, juego.entregasCompletadas());
        assertTrue(juego.tituloVentana().contains("GANASTE"));
        float tiempoFinal = juego.tiempoPartida();
        for (int i = 0; i < 300; i++) juego.actualizarActividad(0.05f);
        assertEquals(tiempoFinal, juego.tiempoPartida(), 0f);
        assertEquals(CiudadInteractiva.DESTINOS.length, juego.entregasCompletadas());
    }

    public void testReinicioConRPermiteGanarOtraVez() {
        CiudadInteractiva juego = new CiudadInteractiva();
        completarRecorrido(juego);
        juego.velocidad = 8;
        juego.angulo = 1.5f;
        juego.tecla(GLFW_KEY_R); // Usa el mismo camino de teclado que la aplicacion.
        assertEquals(0, juego.entregasCompletadas());
        assertEquals(0f, juego.tiempoPartida(), 0f);
        assertEquals(ConduccionAuto.INICIO_X, juego.autoX, 0f);
        assertEquals(ConduccionAuto.INICIO_Z, juego.autoZ, 0f);
        assertEquals(0f, juego.velocidad, 0f);
        assertEquals(0f, juego.angulo, 0f);
        assertFalse(juego.tituloVentana().contains("GANASTE"));
        completarRecorrido(juego);
        assertTrue(juego.tituloVentana().contains("GANASTE"));
    }

    public void testSemaforosRepitenDespuesDeGanar() {
        CiudadInteractiva juego = new CiudadInteractiva();
        completarRecorrido(juego); // El reloj de escena queda en 0.3 segundos.
        float tiempoFinal = juego.tiempoPartida();
        assertEquals(0, juego.luzSemaforoActiva()); // Rojo.
        juego.actualizarActividad(5);
        assertEquals(2, juego.luzSemaforoActiva()); // Verde aunque la partida ya termino.
        juego.actualizarActividad(5);
        assertEquals(1, juego.luzSemaforoActiva()); // Amarillo.
        juego.actualizarActividad(2);
        assertEquals(0, juego.luzSemaforoActiva()); // Vuelve al rojo.
        assertEquals(tiempoFinal, juego.tiempoPartida(), 0f); // No altera el tiempo de victoria.
    }

    public void testReinicioNoInterrumpeElRelojDeLaCiudad() {
        CiudadInteractiva juego = new CiudadInteractiva();
        juego.actualizarActividad(6);
        assertEquals(2, juego.luzSemaforoActiva());
        juego.tecla(GLFW_KEY_R);
        assertEquals(2, juego.luzSemaforoActiva()); // Reinicia la partida, la ciudad continua.
        assertEquals(0f, juego.tiempoPartida(), 0f);
    }

    public void testTituloBreveYControlesActualizados() {
        CiudadInteractiva juego = new CiudadInteractiva();
        juego.ancho = 600;
        juego.velocidad = 10;
        assertTrue(juego.tituloVentana().startsWith("36 km/h"));
        assertTrue(juego.tituloVentana().contains("Entregas 0/3"));
        assertTrue(juego.tituloVentana().contains("N:noche F:ON M:ON"));
        assertTrue(juego.tituloVentana().length() < 80);
        juego.tecla(GLFW_KEY_M);
        juego.tecla(GLFW_KEY_N);
        juego.tecla(GLFW_KEY_F);
        assertFalse(juego.mapaVisible());
        assertTrue(juego.tituloVentana().contains("N:dia F:OFF M:OFF"));
        juego.tecla(GLFW_KEY_M);
        assertTrue(juego.mapaVisible());
        juego.tecla(GLFW_KEY_C);
        assertTrue(juego.camaraAerea);
    }
}
