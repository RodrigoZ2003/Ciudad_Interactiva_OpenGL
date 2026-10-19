package com.graphics; // Agrupa las cuatro lecciones dentro del mismo paquete.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar teclas y cambiar el título de la ventana.

/**
 * CLASE 2: CREACIÓN Y MOVIMIENTO DEL AUTO.
 * Hereda la ciudad de CiudadBase y agrega únicamente lo necesario para conducir.
 * Orden de lectura: variables, teclado, movimiento, colisiones, cámara y dibujo.
 */
public class ConduccionAuto extends CiudadBase {

    // ==================== 1. VARIABLES DEL AUTO ====================
    protected static final float INICIO_X = centro(0); // AMPLIACION 1: centro de la calle oeste (-50).
    protected static final float INICIO_Z = centro(MAPA.length - 1); // Calle sur (+50).
    protected float autoX = INICIO_X; // Posición horizontal inicial: centro de una calle.
    protected float autoZ = INICIO_Z; // Posición inicial sobre el eje que recorre el fondo de la ciudad.
    protected float angulo = 0; // Orientación en radianes; cero apunta hacia -Z.
    protected float velocidad = 0; // Unidades por segundo; un valor negativo significa reversa.
    private static final int[] LIMITES_MARCHAS_KMH = {20, 40, 60, 100, 150};
    protected int marcha = 1; // Selección manual con las teclas 1 a 5.
    protected boolean camaraAerea = false; // false: seguir el auto; true: observar toda la ciudad.
    private float distanciaCamara = 12; // Zoom de seguimiento; conserva el encuadre inicial.
    private BocinaAuto bocina; // Se crea con la primera pulsación de B.
    private MallaRueda ruedas; // Se crea al dibujar, cuando ya existe el contexto OpenGL.
    protected boolean chataEnganchada;
    protected float anguloChata; // Orientación independiente; la lanza articula sobre el enganche.
    private boolean chataSinEspacio;

    // ILUMINACION 2: medidas compartidas por las piezas visibles y las fuentes de luz.
    protected static final float SEPARACION_FAROS = 0.55f;
    protected static final float ALTURA_FAROS = 0.68f;
    protected static final float AVANCE_FAROS = 1.32f;

    // ==================== 2. TECLADO Y REINICIO ====================

    /** Atiende acciones que deben ocurrir una sola vez por pulsación. */
    @Override // Sustituye el método de CiudadBase y conserva sus acciones mediante super.
    protected void tecla(int key) {
        super.tecla(key); // Ejecuta el control ESC definido en CiudadBase.

        if (key >= GLFW_KEY_1 && key <= GLFW_KEY_5) {
            marcha = key - GLFW_KEY_1 + 1;
            limitarVelocidad();
        } else if (key >= GLFW_KEY_KP_1 && key <= GLFW_KEY_KP_5) {
            marcha = key - GLFW_KEY_KP_1 + 1;
            limitarVelocidad();
        }

        if (key == GLFW_KEY_B) {
            tocarBocina();
        }

        if (key == GLFW_KEY_T) {
            alternarChata();
        }

        if (key == GLFW_KEY_C) { // Comprueba si se presionó la tecla de cámara.
            camaraAerea = !camaraAerea; // Invierte el modo de cámara actual.
        }

        if (key == GLFW_KEY_R) { // Comprueba si el usuario quiere comenzar de nuevo.
            reiniciar(); // Restaura las variables de conducción.
        }
    }

    /** Reproduce un toque sin detener la conducción ni el renderizado. */
    protected void tocarBocina() {
        if (bocina == null) bocina = new BocinaAuto();
        bocina.tocar();
    }

    /** No permite crear la chata dentro de una manzana, del borde o de otro auto. */
    protected void alternarChata() {
        if (chataEnganchada) {
            chataEnganchada = false;
            chataSinEspacio = false;
        } else {
            chataSinEspacio = !chataPuedeCircular(autoX, autoZ, angulo, angulo);
            chataEnganchada = !chataSinEspacio;
            if (chataEnganchada) anguloChata = angulo;
        }
    }

    protected String estadoChata() {
        if (chataSinEspacio) return "T  SIN ESPACIO DETRÁS";
        return chataEnganchada ? "T  CHATA ACOPLADA" : "T  PONER CHATA";
    }

    /** Coloca nuevamente el auto en su punto de partida. */
    protected void reiniciar() {
        autoX = INICIO_X; // Recupera la coordenada X de inicio.
        autoZ = INICIO_Z; // Recupera la coordenada Z de inicio.
        angulo = 0; // Orienta el frente hacia -Z.
        velocidad = 0; // Detiene cualquier movimiento previo.
        marcha = 1; // Una partida nueva comienza en primera.
        chataEnganchada = false;
        anguloChata = angulo;
        chataSinEspacio = false;
    }

    protected int limiteMarchaKmh() {
        return LIMITES_MARCHAS_KMH[marcha - 1];
    }

    /** Convierte km/h a unidades/s; bajar de marcha aplica el nuevo máximo de inmediato. */
    private void limitarVelocidad() {
        float maximo = limiteMarchaKmh() / 3.6f;
        velocidad = Math.max(-Math.min(6, maximo), Math.min(maximo, velocidad));
    }

    // ==================== 3. MOVIMIENTO POR CUADRO ====================

    /** Actualiza la conducción; deltaTime contiene los segundos transcurridos entre cuadros. */
    @Override // Cambia la cámara orbital de CiudadBase por el control del vehículo.
    protected void actualizar(float deltaTime) {
        float acelerador = 0; // Sin teclas pulsadas no se aplica aceleración del motor.

        if (pulsada(GLFW_KEY_W) || pulsada(GLFW_KEY_UP)) { // Acepta W o flecha arriba para avanzar.
            acelerador += 1; // Solicita aceleración hacia delante.
        }

        if (pulsada(GLFW_KEY_S) || pulsada(GLFW_KEY_DOWN)) { // Acepta S o flecha abajo para retroceder.
            acelerador -= 1; // Primero reduce la velocidad positiva y después entra en reversa.
        }

        velocidad += acelerador * 9 * deltaTime; // Integra la aceleración de 9 unidades por segundo cuadrado.
        // Con el motor empujando se pueden alcanzar los 150 km/h; al soltar se conserva la retención.
        float resistencia = acelerador == 0 ? 0.7f : 0.1f;

        if (pulsada(GLFW_KEY_SPACE)) { // Detecta si el usuario mantiene presionado el freno.
            resistencia = 7; // Aumenta la pérdida de velocidad para detenerse rápidamente.
        }

        float factorFrenado = (float) Math.exp(-resistencia * deltaTime); // Calcula la fracción de velocidad conservada.
        velocidad *= factorFrenado; // Aplica resistencia de forma proporcional al tiempo transcurrido.
        limitarVelocidad(); // Aplica el máximo de la marcha; la reversa sigue limitada a 6 unidades/s.
        float direccion = 0; // Sin dirección presionada el volante permanece recto.

        if (pulsada(GLFW_KEY_A) || pulsada(GLFW_KEY_LEFT)) { // Comprueba el giro hacia la izquierda.
            direccion += 1; // Selecciona el sentido positivo de rotación.
        }

        if (pulsada(GLFW_KEY_D) || pulsada(GLFW_KEY_RIGHT)) { // Comprueba el giro hacia la derecha.
            direccion -= 1; // Selecciona el sentido negativo de rotación.
        }

        moverAuto(velocidad * deltaTime, direccion * velocidad * 0.11f * deltaTime);

        // FUNCIONES 3: el ciclo principal publica los indicadores al terminar todas las actualizaciones.
    }

    /** FUNCIONES 3: actualiza el titulo una vez, con el estado final de este cuadro. */
    @Override
    protected void actualizarIndicadores() {
        glfwSetWindowTitle(ventana, tituloVentana());
    }

    /** Coloca primero los datos de la partida para mantenerlos visibles en ventanas estrechas. */
    protected String tituloVentana() {
        int kilometrosPorHora = Math.round(Math.abs(velocidad) * 3.6f);
        String titulo = kilometrosPorHora + " km/h | Marcha " + marcha + estadoExtra() + " | " + estadoChata();
        if (ancho >= 1000) { // Las instrucciones largas solo ocupan espacio cuando caben.
            titulo += " | WASD: conducir | 1-5: marchas | Espacio: freno | B: bocina | C: camara | R: reinicio";
        }
        return titulo;
    }

    /** Deja un espacio para los indicadores de las próximas lecciones. */
    protected String estadoExtra() {
        return ""; // En ConduccionAuto todavía no hay información adicional.
    }

    // ==================== 4. COLISIONES CON LA CIUDAD ====================

    /** Divide traslado y giro para no saltarse un obstáculo entre dos cuadros. */
    protected void moverAuto(float distancia, float giro) {
        // Pasos pequeños para integrar el enganche y comprobar la trayectoria de ambos cuerpos.
        float pasoGiro = chataEnganchada ? .01f : .025f;
        float pasoDistancia = chataEnganchada ? .04f : .08f;
        int pasos = Math.max(1, (int) Math.ceil(Math.max(Math.abs(distancia) / pasoDistancia, Math.abs(giro) / pasoGiro)));
        float avance = distancia / pasos;
        float rotacion = giro / pasos;
        for (int paso = 0; paso < pasos; paso++) {
            float nuevoAngulo = angulo + rotacion;
            float dx = -(float) Math.sin(nuevoAngulo) * avance;
            float dz = -(float) Math.cos(nuevoAngulo) * avance;
            float nuevaChata = predecirAnguloChata(autoX + dx, autoZ + dz, nuevoAngulo);
            if (puedeCircular(autoX + dx, autoZ + dz, nuevoAngulo, nuevaChata)) {
                autoX += dx;
                autoZ += dz;
                angulo = nuevoAngulo;
                anguloChata = nuevaChata;
                if (distancia != 0 || giro != 0) chataSinEspacio = false;
            } else {
                // Aprovecha la parte válida del último paso hasta el punto de contacto.
                float libre = 0;
                float bloqueado = 1;
                float chataLibre = anguloChata;
                for (int intento = 0; intento < 12; intento++) {
                    float fraccion = (libre + bloqueado) / 2;
                    float candidatoChata = predecirAnguloChata(autoX + dx * fraccion, autoZ + dz * fraccion, angulo + rotacion * fraccion);
                    if (puedeCircular(autoX + dx * fraccion, autoZ + dz * fraccion, angulo + rotacion * fraccion, candidatoChata)) {
                        libre = fraccion;
                        chataLibre = candidatoChata;
                    } else {
                        bloqueado = fraccion;
                    }
                }
                autoX += dx * libre;
                autoZ += dz * libre;
                angulo += rotacion * libre; // El giro tampoco puede introducir el auto en una acera.
                anguloChata = chataLibre; // Solo se aplica la parte aceptada, sin girar a través del obstáculo.
                velocidad = 0;
                break;
            }
        }
    }

    private float predecirAnguloChata(float x, float z, float giro) {
        return chataEnganchada ? ChataAuto.seguirEnganche(autoX, autoZ, angulo, anguloChata, x, z, giro) : giro;
    }

    protected boolean puedeCircular(float x, float z) {
        return puedeCircular(x, z, angulo);
    }

    /** Comprueba el contorno orientado contra las aceras visibles y el borde del suelo. */
    protected boolean puedeCircular(float x, float z, float giro) {
        return puedeCircular(x, z, giro, anguloChata);
    }

    protected boolean puedeCircular(float x, float z, float giro, float giroChata) {
        return espacioLibre(Colisiones.auto(x, z, giro))
            && (!chataEnganchada || chataPuedeCircular(x, z, giro, giroChata));
    }

    private boolean chataPuedeCircular(float x, float z, float giro, float giroChata) {
        if (Math.abs(ChataAuto.articulacion(giro, giroChata)) > ChataAuto.MAX_ARTICULACION) return false;
        var auto = Colisiones.auto(x, z, giro);
        for (var contorno : ChataAuto.contornos(x, z, giro, giroChata)) {
            if (!espacioLibre(contorno) || Colisiones.chocan(auto, contorno)) return false;
        }
        return true;
    }

    /** Evalúa un contorno individual; el tráfico usa esto sin heredar la chata del jugador. */
    protected boolean espacioLibre(Colisiones.Rectangulo contorno) {
        if (!Colisiones.dentroDelMapa(contorno, LIMITE)) return false;
        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                if (MAPA[fila][columna] == 0) continue;
                Colisiones.Rectangulo acera = new Colisiones.Rectangulo(
                    centro(columna), centro(fila), CELDA / 2, CELDA / 2, 0);
                if (Colisiones.chocan(contorno, acera)) return false;
            }
        }
        return true;
    }

    /** El tráfico debe respetar tanto el auto como la plataforma y su enganche. */
    protected boolean jugadorOcupa(Colisiones.Rectangulo contorno) {
        if (Colisiones.chocan(contorno, Colisiones.auto(autoX, autoZ, angulo))) return true;
        if (chataEnganchada) {
            for (var parte : ChataAuto.contornos(autoX, autoZ, angulo, anguloChata)) {
                if (Colisiones.chocan(contorno, parte)) return true;
            }
        }
        return false;
    }

    // ==================== 5. CÁMARA ====================

    /** Rueda hacia arriba: acercar; hacia abajo: alejar. Solo afecta al seguimiento. */
    @Override
    protected void ruedaRaton(double desplazamiento) {
        if (camaraAerea || !Double.isFinite(desplazamiento)) return;
        distanciaCamara = (float) Math.max(4, Math.min(24, distanciaCamara - desplazamiento * 1.2));
    }

    /** Elige entre una vista general y una cámara situada detrás del auto. */
    @Override // Personaliza la cámara creada en CiudadBase.
    protected void configurarCamara() {
        if (camaraAerea) { // Comprueba si está activa la vista general.
            super.configurarCamara(); // Reutiliza la cámara oblicua de la primera lección.
            return; // Evita reemplazarla con la cámara de seguimiento.
        }

        float camaraX = autoX + (float) Math.sin(angulo) * distanciaCamara;
        float camaraZ = autoZ + (float) Math.cos(angulo) * distanciaCamara;
        float alturaCamara = 0.8f + 8.2f * distanciaCamara / 12; // Mantiene la inclinación al hacer zoom.
        vector("uOjo", camaraX, alturaCamara, camaraZ);
        vector("uObjetivo", autoX, 0.8f, autoZ); // Orienta la cámara hacia la carrocería.
        decimal("uAspecto", (float) ancho / alto); // Mantiene las proporciones al redimensionar la ventana.
    }

    // ==================== 6. DIBUJO DEL AUTO ====================

    /** Dibuja primero la ciudad existente y luego el vehículo. */
    @Override // Amplía el dibujo de CiudadBase sin copiar su código.
    protected void escena() {
        super.escena(); // Dibuja asfalto, calles, edificios y parques.
        dibujarAuto(); // Añade el modelo del vehículo sobre la ciudad.
    }

    /** Dibuja el auto del jugador con el mismo modelo que usa el trafico. */
    protected void dibujarAuto() {
        dibujarVehiculo(autoX, autoZ, angulo, 0.95f, 0.24f, 0.12f, true);
        if (chataEnganchada) ChataAuto.dibujar(this);
    }

    /** EXTRA 5: modelo reutilizable; recibe posicion, giro, color y tipo de vehiculo. */
    protected void dibujarVehiculo(float origenX, float origenZ, float giro, float rojo, float verde, float azul, boolean jugador) {
        // Carrocería compacta: bajos oscuros, capó y maletero diferenciados.
        piezaEn(origenX, origenZ, giro, 0, 0.40f, 0, 1.55f, 0.18f, 2.5f, 0.08f, 0.10f, 0.13f);
        piezaEn(origenX, origenZ, giro, 0, 0.65f, 0, 1.65f, 0.42f, 2.6f, rojo, verde, azul);
        piezaEn(origenX, origenZ, giro, 0, 0.87f, -0.92f, 1.56f, 0.12f, 0.73f, rojo, verde, azul);
        piezaEn(origenX, origenZ, giro, 0, 0.86f, 1.03f, 1.56f, 0.1f, 0.5f, rojo * 0.85f, verde * 0.85f, azul * 0.85f);
        // Cabina con marcos, parabrisas, luneta y techo del color del auto.
        piezaEn(origenX, origenZ, giro, 0, 1.10f, 0.12f, 1.34f, 0.48f, 1.38f, 0.065f, 0.09f, 0.12f);
        piezaEn(origenX, origenZ, giro, 0, 1.12f, -0.58f, 1.19f, 0.32f, 0.025f, 0.27f, 0.53f, 0.63f);
        piezaEn(origenX, origenZ, giro, 0, 1.12f, 0.82f, 1.19f, 0.30f, 0.025f, 0.16f, 0.34f, 0.43f);
        piezaEn(origenX, origenZ, giro, 0, 1.37f, 0.12f, 1.39f, 0.10f, 1.46f, rojo, verde, azul);
        for (float lado : new float[]{-1, 1}) {
            piezaEn(origenX, origenZ, giro, lado * 0.68f, 1.12f, -0.24f, 0.025f, 0.30f, 0.52f, 0.24f, 0.48f, 0.59f);
            piezaEn(origenX, origenZ, giro, lado * 0.68f, 1.12f, 0.44f, 0.025f, 0.30f, 0.52f, 0.19f, 0.39f, 0.49f);
            piezaEn(origenX, origenZ, giro, lado * 0.84f, 0.72f, 0.36f, 0.035f, 0.045f, 0.2f, 0.68f, 0.73f, 0.77f);
            piezaEn(origenX, origenZ, giro, lado * 0.91f, 1.0f, -0.42f, 0.26f, 0.16f, 0.24f, rojo, verde, azul);
            piezaEn(origenX, origenZ, giro, lado * 0.91f, 1.0f, -0.292f, 0.19f, 0.10f, 0.02f, 0.53f, 0.67f, 0.73f);
        }
        // Parachoques, parrilla con barras y placas delantera/trasera.
        for (float extremo : new float[]{-1, 1}) {
            piezaEn(origenX, origenZ, giro, 0, 0.47f, extremo * 1.3f, 1.5f, 0.14f, 0.12f, 0.12f, 0.15f, 0.18f);
            piezaEn(origenX, origenZ, giro, 0, 0.61f, extremo * 1.36f, 0.34f, 0.14f, 0.025f, 0.87f, 0.89f, 0.85f);
        }
        piezaEn(origenX, origenZ, giro, 0, 0.76f, -1.31f, 0.58f, 0.17f, 0.04f, 0.055f, 0.07f, 0.09f);
        for (float y : new float[]{0.71f, 0.77f, 0.82f}) {
            piezaEn(origenX, origenZ, giro, 0, y, -1.337f, 0.52f, 0.018f, 0.012f, 0.48f, 0.53f, 0.58f);
        }
        for (float x : new float[]{-0.88f, 0.88f}) {
            for (float z : new float[]{-0.82f, 0.82f}) {
                ruedaEn(origenX, origenZ, giro, x, 0.36f, z, 0.26f, 0.62f, 0.045f, 0.055f, 0.07f);
                float exterior = Math.signum(x);
                ruedaEn(origenX, origenZ, giro, x + exterior * 0.137f, 0.36f, z, 0.025f, 0.39f, 0.66f, 0.71f, 0.76f);
                ruedaEn(origenX, origenZ, giro, x + exterior * 0.153f, 0.36f, z, 0.015f, 0.16f, 0.16f, 0.20f, 0.25f);
            }
        }
        for (float x : new float[]{-SEPARACION_FAROS, SEPARACION_FAROS}) {
            if (jugador) {
                dibujarFaro(x); // Conserva los dos focos reales y su interruptor F en IluminacionCiudad.
            } else {
                piezaEn(origenX, origenZ, giro, x, ALTURA_FAROS, -AVANCE_FAROS, 0.38f, 0.2f, 0.07f, 1, 0.95f, 0.65f);
            }
            piezaEn(origenX, origenZ, giro, x, 0.68f, 1.32f, 0.35f, 0.17f, 0.07f, 0.85f, 0.05f, 0.05f);
        }
    }

    /** Usa el mismo giro y traslado que las cajas, con una malla cilíndrica de eje X. */
    protected void ruedaEn(float origenX, float origenZ, float giro, float x, float y, float z,
                           float anchoRueda, float diametro, float r, float g, float b) {
        if (ruedas == null) ruedas = new MallaRueda();
        float coseno = (float) Math.cos(giro);
        float seno = (float) Math.sin(giro);
        vector("uPos", origenX + coseno * x + seno * z, y, origenZ - seno * x + coseno * z);
        vector("uEscala", anchoRueda, diametro, diametro);
        vector("uColor", r, g, b);
        decimal("uGiro", giro);
        ruedas.dibujar();
    }

    @Override
    protected void liberarExtras() {
        if (bocina != null) {
            bocina.close();
            bocina = null;
        }
        if (ruedas != null) {
            ruedas.close();
            ruedas = null;
        }
        super.liberarExtras();
    }

    /** Dibuja el faro de la etapa basica; IluminacionCiudad agrega su encendido visual. */
    protected void dibujarFaro(float x) {
        pieza(x, ALTURA_FAROS, -AVANCE_FAROS, 0.38f, 0.2f, 0.07f, 1, 0.95f, 0.65f);
    }

    /** Transforma una pieza del espacio local del auto al espacio de la ciudad. */
    protected void pieza(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        piezaEn(autoX, autoZ, angulo, x, y, z, sx, sy, sz, r, g, b);
    }

    /** EXTRA 5: transforma una pieza sin modificar las variables del auto del jugador. */
    protected void piezaEn(float origenX, float origenZ, float giro, float x, float y, float z,
                           float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(giro);
        float seno = (float) Math.sin(giro);
        float mundoX = origenX + coseno * x + seno * z;
        float mundoZ = origenZ - seno * x + coseno * z;
        cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, giro);
    }

    /** Punto de entrada para ejecutar únicamente la segunda etapa. */
    public static void main(String[] args) {
        ConduccionAuto aplicacion = new ConduccionAuto(); // Crea la aplicación con ciudad y vehículo.
        aplicacion.run(); // Inicia la ventana y el ciclo de dibujo heredados.
    }
}
