package com.graphics;

import junit.framework.TestCase;
import static org.lwjgl.glfw.GLFW.*;

/** EXTRA 5: seguridad de rutas completas, tiempo entre cuadros y datos de la interfaz. */
public class ExtrasTest extends TestCase {
    public void testTresVehiculosConEstadoPropio() {
        TraficoAutonomo trafico = new TraficoAutonomo();
        assertEquals(3, trafico.vehiculos().size());
        assertNotSame(trafico.vehiculos().get(0), trafico.vehiculos().get(1));
        for (TraficoAutonomo.Vehiculo auto : trafico.vehiculos()) {
            assertTrue(auto.velocidad() > 0);
            assertTrue(Float.isFinite(auto.angulo()));
        }
        assertTrue(trafico.vehiculos().get(0).velocidad() != trafico.vehiculos().get(1).velocidad());
    }

    public void testRutasContinuasSinInvadirManzanasDuranteCincoMinutos() {
        TraficoAutonomo trafico = new TraficoAutonomo();
        ConduccionAuto ciudad = new ConduccionAuto();
        int[] giros = new int[3];
        for (int cuadro = 0; cuadro < 6000; cuadro++) {
            float[] x = new float[3], z = new float[3], angulo = new float[3];
            for (int i = 0; i < 3; i++) {
                var auto = trafico.vehiculos().get(i);
                x[i] = auto.x(); z[i] = auto.z(); angulo[i] = auto.angulo();
            }
            trafico.actualizar(0.05f);
            for (int i = 0; i < 3; i++) {
                var auto = trafico.vehiculos().get(i);
                assertTrue("Ruta fuera de calle: vehiculo " + i, ciudad.puedeCircular(auto.x(), auto.z(), auto.angulo()));
                double dx = auto.x() - x[i], dz = auto.z() - z[i];
                double avance = Math.hypot(dx, dz);
                assertTrue(avance > auto.velocidad() * 0.05 * 0.99);
                assertTrue(avance <= auto.velocidad() * 0.05 + 0.00002);
                double giro = Math.atan2(Math.sin(auto.angulo() - angulo[i]), Math.cos(auto.angulo() - angulo[i]));
                assertTrue("Giro brusco o teletransporte", Math.abs(giro) < 0.1);
                if (Math.abs(giro) > 0.001) giros[i]++;
                double alineacion = (dx * -Math.sin(auto.angulo()) + dz * -Math.cos(auto.angulo())) / avance;
                assertTrue("El frente debe seguir el movimiento", alineacion > 0.995);
            }
        }
        for (int girosVehiculo : giros) assertTrue(girosVehiculo > 100);
    }

    public void testMovimientoIndependienteDeLaFrecuenciaDeDibujo() {
        TraficoAutonomo lento = new TraficoAutonomo();
        TraficoAutonomo rapido = new TraficoAutonomo();
        for (int i = 0; i < 240; i++) lento.actualizar(1f / 20);
        for (int i = 0; i < 720; i++) rapido.actualizar(1f / 60);
        for (int i = 0; i < 3; i++) {
            assertEquals(lento.vehiculos().get(i).x(), rapido.vehiculos().get(i).x(), 0.001f);
            assertEquals(lento.vehiculos().get(i).z(), rapido.vehiculos().get(i).z(), 0.001f);
            assertEquals(lento.vehiculos().get(i).angulo(), rapido.vehiculos().get(i).angulo(), 0.001f);
        }
    }

    public void testReinicioRestauraElTrafico() {
        TraficoAutonomo trafico = new TraficoAutonomo();
        float[] inicioX = new float[3], inicioZ = new float[3], inicioAngulo = new float[3];
        for (int i = 0; i < 3; i++) {
            var auto = trafico.vehiculos().get(i);
            inicioX[i] = auto.x(); inicioZ[i] = auto.z(); inicioAngulo[i] = auto.angulo();
        }
        trafico.actualizar(135);
        trafico.reiniciar();
        for (int i = 0; i < 3; i++) {
            var auto = trafico.vehiculos().get(i);
            assertEquals(inicioX[i], auto.x(), 0f);
            assertEquals(inicioZ[i], auto.z(), 0f);
            assertEquals(inicioAngulo[i], auto.angulo(), 0f);
        }
    }

    public void testHudReflejaVelocidadLucesMapaYAyuda() {
        CiudadInteractiva juego = new CiudadInteractiva();
        juego.velocidad = -5;
        var estado = juego.datosHud();
        assertEquals(18f, estado.velocidadKmh(), 0f);
        assertEquals(3, estado.trafico());
        assertEquals("SUROESTE", estado.sector());
        assertEquals("NORESTE", estado.destino());
        assertFalse(estado.ayuda());
        juego.tecla(GLFW_KEY_H);
        juego.tecla(GLFW_KEY_N);
        juego.tecla(GLFW_KEY_F);
        juego.tecla(GLFW_KEY_M);
        estado = juego.datosHud();
        assertTrue(estado.ayuda());
        assertFalse(estado.noche());
        assertFalse(estado.faros());
        assertFalse(estado.mapa());
        juego.tecla(GLFW_KEY_H);
        assertFalse(juego.datosHud().ayuda());
    }

    public void testVictoriaYReinicioActualizanPanelYTrafico() {
        CiudadInteractiva juego = new CiudadInteractiva();
        float traficoInicialX = juego.trafico.vehiculos().get(0).x();
        for (float[] destino : CiudadInteractiva.DESTINOS) {
            juego.autoX = destino[0]; juego.autoZ = destino[1]; juego.velocidad = 0;
            juego.actualizarActividad(1);
        }
        assertEquals(3, juego.datosHud().entregas());
        assertEquals("COMPLETO", juego.datosHud().destino());
        float xAntes = juego.trafico.vehiculos().get(0).x();
        juego.actualizarActividad(2);
        assertTrue(juego.trafico.vehiculos().get(0).x() != xAntes);
        assertEquals(3f, juego.datosHud().tiempo(), 0f);
        juego.tecla(GLFW_KEY_R);
        assertEquals(0, juego.datosHud().entregas());
        assertEquals(0f, juego.datosHud().tiempo(), 0f);
        assertEquals("NORESTE", juego.datosHud().destino());
        assertEquals(traficoInicialX, juego.trafico.vehiculos().get(0).x(), 0f);
        juego.autoX = 0; juego.autoZ = 0;
        assertEquals("CENTRO", juego.datosHud().sector());
    }
}
