package com.graphics; // Mantiene esta lección junto a las clases anteriores.

import static org.lwjgl.glfw.GLFW.*; // Incluye las constantes de las teclas N y F.

/**
 * CLASE 3: ILUMINACIÓN DE LA CIUDAD.
 * Conserva ciudad y conducción; añade sol, farolas y focos del vehículo.
 * El shader calcula iluminación local: este ejemplo todavía no proyecta sombras.
 */
public class IluminacionCiudad extends ConduccionAuto {

    // ==================== 1. ESTADO Y POSICIONES DE LAS LUCES ====================
    protected boolean noche = true; // Inicia la escena con iluminación nocturna.
    protected boolean faros = true; // Inicia los focos del auto encendidos.
    // ILUMINACION 2: distancias en unidades del mundo; cada celda mide 10.
    protected static final float ALCANCE_FAROS = 24; // Luz frontal nula a partir de esta distancia.
    protected static final float ALCANCE_FAROLAS = 28; // Evita acumular aportes de farolas muy lejanas.
    protected static final float[][] LUCES = crearFarolas(); // Una por manzana: 25 en el mapa actual.

    /** Distribuye farolas en las aceras, incluyendo edificios y parques de los sectores nuevos. */
    protected static float[][] crearFarolas() {
        java.util.List<float[]> posiciones = new java.util.ArrayList<>();
        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                if (MAPA[fila][columna] != 0) { // Las calles se dejan libres para conducir.
                    float esquinaX = centro(columna) - CELDA * 0.4f; // Esquina noroeste, dentro de la acera.
                    float esquinaZ = centro(fila) - CELDA * 0.4f;
                    posiciones.add(new float[]{esquinaX, 4.5f, esquinaZ});
                }
            }
        }
        return posiciones.toArray(new float[0][]);
    }

    /** ILUMINACION 2: lleva los dos faros del espacio local al mundo, igual que pieza(). */
    protected static float[][] posicionesFaros(float x, float z, float giro) {
        float seno = (float) Math.sin(giro);
        float coseno = (float) Math.cos(giro);
        float[][] posiciones = new float[2][3];
        for (int indice = 0; indice < 2; indice++) {
            float lateral = indice == 0 ? -SEPARACION_FAROS : SEPARACION_FAROS;
            posiciones[indice][0] = x + coseno * lateral - seno * AVANCE_FAROS;
            posiciones[indice][1] = ALTURA_FAROS;
            posiciones[indice][2] = z - seno * lateral - coseno * AVANCE_FAROS;
        }
        return posiciones;
    }

    // ==================== 2. CONTROLES E INDICADORES ====================

    /** Añade los interruptores de iluminación a los controles existentes. */
    @Override // Amplía el método de teclado de ConduccionAuto.
    protected void tecla(int key) {
        super.tecla(key); // Conserva ESC, C y R.
        if (key == GLFW_KEY_N) { // Comprueba la tecla de cambio de ambiente.
            noche = !noche; // Alterna entre noche y día.
        }
        if (key == GLFW_KEY_F) { // Comprueba el interruptor de los focos del auto.
            faros = !faros; // Enciende los faros si estaban apagados y viceversa.
        }
    }

    /** Prepara el texto que ConduccionAuto incorpora al título de la ventana. */
    @Override // Añade información al espacio reservado para indicadores.
    protected String estadoExtra() {
        String ambiente = "dia"; // Usa día como texto inicial.
        if (noche) { // Comprueba si está seleccionado el ambiente nocturno.
            ambiente = "noche"; // Reemplaza el texto por el estado real.
        }
        String estadoFaros = "OFF"; // Usa apagado como texto inicial de los focos.
        if (faros) { // Comprueba si las luces del auto están activas.
            estadoFaros = "ON"; // Indica que los faros están encendidos.
        }
        return " | N:" + ambiente + " F:" + estadoFaros; // Devuelve ambos indicadores y sus teclas.
    }

    // ==================== 3. DATOS QUE RECIBE EL SHADER ====================

    /** Actualiza los uniforms de iluminación antes de dibujar la ciudad. */
    @Override // Sustituye el método vacío de CiudadBase.
    protected void prepararLuces() {
        entero("uNoche", 0); // Inicialmente configura iluminación diurna.
        if (noche) { // Revisa el modo elegido por el usuario.
            entero("uNoche", 1); // Comunica al shader que debe usar iluminación nocturna.
        }
        entero("uFaros", 0); // Inicialmente desactiva los conos de luz del auto.
        if (faros) { // Revisa el interruptor de los faros.
            entero("uFaros", 1); // Activa el cálculo de los dos focos en el shader.
        }
        entero("uEmision", 0); // Hace que los objetos normales reciban iluminación.

        for (int indice = 0; indice < LUCES.length; indice++) { // Recorre todas las bombillas del mapa ampliado.
            float x = LUCES[indice][0]; // Lee la coordenada horizontal de la bombilla.
            float y = LUCES[indice][1]; // Lee su altura sobre el suelo.
            float z = LUCES[indice][2]; // Lee su coordenada en profundidad.
            vector("uLuces[" + indice + "]", x, y, z); // Envía esa posición al arreglo GLSL.
        }

        float frenteX = -(float) Math.sin(angulo); // Calcula hacia dónde apunta el auto en X.
        float frenteZ = -(float) Math.cos(angulo); // Calcula hacia dónde apunta el auto en Z.
        // ILUMINACION 2: las fuentes coinciden con las piezas dibujadas, incluso al girar.
        float[][] origenes = posicionesFaros(autoX, autoZ, angulo);
        for (int indice = 0; indice < origenes.length; indice++) {
            vector("uOrigenFaros[" + indice + "]", origenes[indice][0], origenes[indice][1], origenes[indice][2]);
        }
        decimal("uAlcanceFaros", ALCANCE_FAROS);
        decimal("uAlcanceFarolas", ALCANCE_FAROLAS);
        vector("uFrente", frenteX, 0, frenteZ); // Envía la dirección frontal del vehículo.
    }

    /** ILUMINACION 2: emitir color en la lente no sustituye la luz calculada sobre el suelo. */
    @Override
    protected void dibujarFaro(float x) {
        if (faros) {
            entero("uEmision", 1); // La lente parece encendida tanto de dia como de noche.
            pieza(x, ALTURA_FAROS, -AVANCE_FAROS, 0.38f, 0.2f, 0.07f, 1, 0.95f, 0.65f);
        } else {
            pieza(x, ALTURA_FAROS, -AVANCE_FAROS, 0.38f, 0.2f, 0.07f, 0.18f, 0.20f, 0.22f);
        }
        entero("uEmision", 0); // No aplicar emision al resto del auto ni a los postes.
    }

    // ==================== 4. MODELOS DE LAS FAROLAS ====================

    @Override
    protected void configurarFondo() {
        if (noche) org.lwjgl.opengl.GL33.glClearColor(.025f, .045f, .085f, 1);
        else super.configurarFondo();
    }

    /** Dibuja postes y bombillas en las mismas posiciones usadas para calcular la luz. */
    @Override // Añade geometría a la ciudad y al auto heredados.
    protected void escena() {
        super.escena(); // Dibuja la ciudad y el vehículo de las primeras dos lecciones.
        for (float[] luz : LUCES) { // Selecciona una farola a la vez.
            float x = luz[0]; // Lee la posición horizontal del poste.
            float y = luz[1]; // Lee la altura de la bombilla.
            float z = luz[2]; // Lee la posición del poste en profundidad.
            caja(x, 2.2f, z, 0.18f, 4.4f, 0.18f, 0.20f, 0.24f, 0.28f); // Dibuja el poste delgado y oscuro.
            if (!vistaMapa) {
                caja(x, .43f, z, .42f, .26f, .42f, .25f, .29f, .30f);
                caja(x, y + .22f, z, .88f, .12f, .88f, .16f, .21f, .24f);
            }
            if (noche) { // Las bombillas aparentan estar encendidas únicamente de noche.
                entero("uEmision", 1); // Evita que la bombilla sea oscurecida por la iluminación.
            }
            caja(x, y, z, 0.7f, 0.35f, 0.7f, 1, 0.83f, 0.42f); // Dibuja la bombilla de color cálido.
            entero("uEmision", 0); // Restablece el material normal para el siguiente objeto.
        }
    }

    // ==================== 5. CÁLCULO DE LUZ EN LA GPU ====================

    /** Devuelve el programa GLSL que calcula el color iluminado de cada fragmento. */
    @Override // Reemplaza el shader de color plano de CiudadBase.
    protected String fragmentShader() {
        return """
            #version 330 core // Selecciona la versión GLSL correspondiente a OpenGL 3.3.
            in vec3 vMundo; // Recibe la posición del fragmento en la ciudad.
            in vec3 vNormal; // Recibe la dirección perpendicular a la superficie.
            uniform vec3 uColor; // Recibe el color base de la caja.
            const int CANTIDAD_FAROLAS = %d; // ILUMINACION 2: Java coloca aqui LUCES.length al compilar.
            uniform vec3 uLuces[CANTIDAD_FAROLAS]; // Mismas posiciones para dibujar e iluminar.
            uniform vec3 uOrigenFaros[2]; // Recibe las posiciones mundiales de las dos lentes.
            uniform float uAlcanceFaros; // Alcance frontal en unidades del mundo.
            uniform float uAlcanceFarolas; // Radio de influencia de cada farola.
            uniform vec3 uFrente; // Recibe la dirección hacia la que apunta el vehículo.
            uniform int uNoche; // Vale 1 de noche y 0 de día.
            uniform int uFaros; // Vale 1 cuando los focos están encendidos.
            uniform int uEmision; // Vale 1 si el objeto debe conservar su color sin oscurecerse.
            uniform int uMapa; // Vale 1 durante el dibujo del minimapa de CiudadInteractiva.
            out vec4 color; // Entrega el color RGBA final al framebuffer.

            void main() { // Se ejecuta para cada fragmento visible de una caja.
                if (uEmision == 1 || uMapa == 1) { // Bombillas y minimapa usan colores directos.
                    color = vec4(uColor, 1.0); // Conserva el color base con opacidad completa.
                    return; // Termina el shader sin calcular iluminación.
                }

                vec3 normal = normalize(vNormal); // Convierte la normal interpolada en un vector unitario.
                vec3 luz = vec3(0.48); // Define la luz ambiental diurna que llega a todas las caras.
                float intensidadSol = 0.65; // Define la fuerza de la iluminación direccional diurna.
                if (uNoche == 1) { // Ajusta el ambiente si es de noche.
                    luz = vec3(0.12, 0.16, 0.24); // Usa una luz ambiental tenue y azulada.
                    intensidadSol = 0.10; // Conserva una pequeña luz direccional nocturna.
                }
                vec3 direccionSol = normalize(vec3(0.4, 1.0, 0.3)); // Define una fuente lejana por su dirección.
                float incidenciaSol = max(dot(normal, direccionSol), 0.0); // Lambert: una cara recibe más luz si mira al sol.
                luz += vec3(intensidadSol) * incidenciaSol; // Suma la contribución direccional al ambiente.

                if (uNoche == 1) { // Calcula la iluminación de las farolas solo de noche.
                    for (int indice = 0; indice < CANTIDAD_FAROLAS; indice++) { // Acumula el aporte de cada bombilla.
                        vec3 haciaLuz = uLuces[indice] - vMundo; // Forma el vector desde la superficie hacia la farola.
                        float distancia = length(haciaLuz); // Mide cuántas unidades separan superficie y bombilla.
                        if (distancia >= uAlcanceFarolas) { continue; } // Aporte nulo fuera del radio.
                        float borde = 1.0 - smoothstep(uAlcanceFarolas * 0.75, uAlcanceFarolas, distancia);
                        float difusa = max(dot(normal, haciaLuz / max(distancia, 0.001)), 0.0); // Calcula la incidencia de la luz sobre la cara.
                        float atenuacion = 1.0 + 0.12 * distancia + 0.045 * distancia * distancia; // Reduce el alcance con la distancia.
                        vec3 colorFarola = vec3(1.0, 0.73, 0.34); // Define el tono cálido de la farola.
                        luz += colorFarola * difusa * 4.5 * borde / atenuacion; // Suma el aporte atenuado de esta bombilla.
                    }
                }

                if (uFaros == 1) { // Calcula los conos únicamente si están encendidos.
                    for (int indice = 0; indice < 2; indice++) { // Repite el cálculo para los dos faros.
                        vec3 origen = uOrigenFaros[indice]; // Coincide con la lente visible del auto.
                        vec3 haciaSuperficie = vMundo - origen; // Forma el vector del faro al fragmento.
                        float distancia = length(haciaSuperficie); // Mide la distancia recorrida por la luz.
                        if (distancia >= uAlcanceFaros) { continue; } // ILUMINACION 2: corte real a 24 unidades.
                        float borde = 1.0 - smoothstep(uAlcanceFaros * 0.75, uAlcanceFaros, distancia);
                        vec3 direccionFragmento = haciaSuperficie / max(distancia, 0.001);
                        vec3 eje = normalize(uFrente + vec3(0.0, -0.10, 0.0)); // Inclina el foco ligeramente hacia el suelo.
                        float alineacion = dot(direccionFragmento, eje); // Un valor cercano a 1 indica el centro del haz.
                        float cono = smoothstep(0.85, 0.97, alineacion); // Suaviza el borde entre el exterior y el interior del foco.
                        float difusa = max(dot(normal, -direccionFragmento), 0.0); // Mide cuánto mira la cara hacia el faro.
                        float atenuacion = 1.0 + 0.04 * distancia * distancia; // Disminuye la intensidad al alejarse.
                        vec3 colorFaro = vec3(1.0, 0.94, 0.72); // Define una luz frontal blanca y cálida.
                        luz += colorFaro * cono * difusa * 8.0 * borde / atenuacion; // Añade el aporte del foco a la iluminación total.
                    }
                }

                color = vec4(uColor * luz, 1.0); // Multiplica el material por toda la luz acumulada.
            }
            """.formatted(LUCES.length); // Finaliza el texto GLSL que Java enviará a OpenGL.
    }

    /** Punto de entrada de la tercera lección. */
    public static void main(String[] args) {
        IluminacionCiudad aplicacion = new IluminacionCiudad(); // Crea la etapa con iluminación.
        aplicacion.run(); // Inicia el ciclo de vida heredado de CiudadBase.
    }
}
