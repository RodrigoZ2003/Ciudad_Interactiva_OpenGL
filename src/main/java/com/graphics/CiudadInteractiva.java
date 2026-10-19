package com.graphics; // Reúne la versión final con las otras tres lecciones.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar la tecla que controla el minimapa.
import static org.lwjgl.opengl.GL33.*; // Permite cambiar viewport, recorte y buffers de dibujo.

/**
 * CLASE 4: CIUDAD TERMINADA, ENTREGAS Y MINIMAPA.
 * Orden de lectura: estado, controles, entregas, decoración y segundo pase de dibujo.
 * Hereda IluminacionCiudad, que hereda ConduccionAuto, que a su vez hereda CiudadBase.
 * Los semaforos son visuales; el trafico autonomo y el HUD se delegan a sus propias clases.
 */
public class CiudadInteractiva extends IluminacionCiudad {

    // ==================== 1. ESTADO DEL JUEGO ====================
    private boolean mostrarMapa = true; // Muestra el minimapa desde el inicio.
    private int entregas = 0; // Cuenta las entregas completadas; también identifica el siguiente destino.
    private float tiempo = 0; // Cronometro de la partida: se detiene al ganar y vuelve a cero con R.
    private double tiempoEscena = 0; // FUNCIONES 3: reloj independiente, tambien avanza despues de ganar.
    // EXTRA 5: logica de trafico independiente del renderizador y recursos del panel.
    protected final TraficoAutonomo trafico = new TraficoAutonomo();
    private boolean mostrarAyuda = false;
    private InterfazHud interfaz;
    protected final ArmamentoAuto armas = new ArmamentoAuto(this::sonidoArma);
    private SonidosArmas sonidosArmas;
    private boolean gatillo;
    // AMPLIACION 1: destinos sobre las avenidas exteriores, expresados como centros de celdas.
    protected static final float[][] DESTINOS = {
        {centro(MAPA.length - 1), centro(0)}, // Noreste: (+50, -50).
        {centro(0), centro(0)}, // Noroeste: (-50, -50).
        {INICIO_X, INICIO_Z} // Suroeste: (-50, +50), regreso al inicio.
    };

    // ==================== 2. CONTROLES Y REINICIO ====================

    /** Añade el interruptor del minimapa a los controles anteriores. */
    @Override // Amplía las teclas definidas por IluminacionCiudad.
    protected void tecla(int key) {
        super.tecla(key); // Conserva salida, cámara, reinicio y luces.
        if (key == GLFW_KEY_K) {
            armas.alternar();
            gatillo = false;
        }
        if (key == GLFW_KEY_M) { // Comprueba si se pulsó la tecla del mapa.
            mostrarMapa = !mostrarMapa; // Alterna entre mostrar y ocultar la vista superior.
        }
        if (key == GLFW_KEY_H) { // EXTRA 5: abre o cierra la ayuda dentro de la ventana.
            mostrarAyuda = !mostrarAyuda;
        }
    }

    @Override
    protected void raton(int boton, int accion) {
        if (boton == GLFW_MOUSE_BUTTON_LEFT && accion == GLFW_RELEASE) gatillo = false;
        if (!armas.visible() || accion != GLFW_PRESS) return;
        if (boton == GLFW_MOUSE_BUTTON_LEFT) {
            gatillo = true;
            armas.dispararMetralletas(autoX, autoZ, angulo, velocidad);
        } else if (boton == GLFW_MOUSE_BUTTON_RIGHT) {
            armas.dispararMisil(autoX, autoZ, angulo, velocidad);
        }
    }

    @Override
    protected void cambiarFoco(boolean activo) {
        if (!activo) gatillo = false; // Cambiar de ventana nunca deja el disparo atascado.
    }

    protected void sonidoArma(SonidosArmas.Tipo tipo) {
        if (sonidosArmas == null) sonidosArmas = new SonidosArmas();
        sonidosArmas.reproducir(tipo);
    }

    /** Reinicia el vehículo y el progreso de las entregas. */
    @Override // Amplía el reinicio definido en ConduccionAuto.
    protected void reiniciar() {
        super.reiniciar(); // Restaura posición, velocidad y orientación del vehículo.
        entregas = 0; // Vuelve a seleccionar la primera parada.
        tiempo = 0; // Reinicia el cronómetro de la partida.
        trafico.reiniciar(); // EXTRA 5: devuelve cada auto autonomo a su propio punto inicial.
        armas.reiniciar();
        gatillo = false;
    }

    // ==================== 3. REGLAS DE LAS ENTREGAS ====================

    /** Actualiza la conduccion heredada y despues las reglas de esta etapa. */
    @Override
    protected void actualizar(float deltaTime) {
        super.actualizar(deltaTime);
        actualizarActividad(deltaTime); // FUNCIONES 3: logica comprobable sin abrir una ventana.
        armas.actualizar(deltaTime, autoX, autoZ, angulo, velocidad, gatillo, trafico);
    }

    /** Comprueba cada parte del conjunto (auto, chata y enganche) frente al tráfico. */
    @Override
    protected boolean espacioLibre(Colisiones.Rectangulo contorno) {
        return super.espacioLibre(contorno) && !trafico.ocupa(contorno, null);
    }

    /** Mantiene viva la ciudad y comprueba si el auto llego y freno en el destino activo. */
    protected void actualizarActividad(float deltaTime) {
        trafico.actualizar(deltaTime, (x, z, giro) -> {
            var contorno = Colisiones.auto(x, z, giro);
            return super.espacioLibre(contorno) && !jugadorOcupa(contorno);
        });
        for (TraficoAutonomo.Vehiculo vehiculo : trafico.vehiculos()) {
            if (!vehiculo.activo()) continue;
            if (!super.espacioLibre(Colisiones.auto(vehiculo.x(), vehiculo.z(), vehiculo.angulo()))) {
                throw new IllegalStateException("Una ruta de trafico invade una manzana o el limite");
            }
        }
        tiempoEscena += deltaTime; // Siempre avanza: los semaforos no dependen del resultado de la partida.
        if (entregas >= DESTINOS.length) {
            return; // Conserva el tiempo de victoria y evita consultar un destino inexistente.
        }
        tiempo += deltaTime;
        float distanciaX = autoX - DESTINOS[entregas][0];
        float distanciaZ = autoZ - DESTINOS[entregas][1];
        float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ;
        boolean estaCerca = distanciaCuadrada < 3 * 3;
        boolean estaFrenando = Math.abs(velocidad) < 1;
        if (estaCerca && estaFrenando) {
            entregas++;
        }
    }

    /** FUNCIONES 3: consultas de estado para verificar reinicio, progreso y controles. */
    protected int entregasCompletadas() {
        return entregas;
    }

    protected float tiempoPartida() {
        return tiempo;
    }

    protected boolean mapaVisible() {
        return mostrarMapa;
    }

    /** Devuelve el indice de la bombilla: 0 rojo, 2 verde, 1 amarillo. */
    protected int luzSemaforoActiva() {
        double fase = tiempoEscena % 12; // FUNCIONES 3: ciclo continuo de doce segundos.
        if (fase < 5) {
            return 0; // Rojo: segundos 0 a 5.
        }
        if (fase < 10) {
            return 2; // Verde: segundos 5 a 10.
        }
        return 1; // Amarillo: segundos 10 a 12.
    }

    /** Estado breve, situado antes de las instrucciones del titulo. */
    @Override
    protected String estadoExtra() {
        String mensaje;
        if (entregas == DESTINOS.length) {
            mensaje = " | GANASTE " + (int) tiempo + "s | R: reinicio";
        } else {
            mensaje = " | Entregas " + entregas + "/" + DESTINOS.length + " | " + (int) tiempo + "s";
        }
        mensaje += super.estadoExtra(); // Conserva el estado dia/noche y los faros.
        mensaje += " M:" + (mostrarMapa ? "ON" : "OFF"); // Muestra el estado real del minimapa.
        return mensaje;
    }

    // ==================== 4. ESCENA FINAL Y DESTINO ====================

    /** Añade decoración y señal del destino a la escena iluminada. */
    @Override // Amplía el dibujo acumulado de las tres etapas anteriores.
    protected void escena() {
        super.escena(); // Dibuja la ciudad, el auto y las farolas.
        for (TraficoAutonomo.Vehiculo vehiculo : trafico.vehiculos()) {
            if (!vehiculo.activo()) continue;
            dibujarVehiculo(vehiculo.x(), vehiculo.z(), vehiculo.angulo(), vehiculo.rojo(), vehiculo.verde(), vehiculo.azul(), false);
        }
        armas.dibujar(this);
        if (!vistaMapa) { // Los detalles pequeños solo son necesarios en la vista principal.
            decorarCiudad(); // Añade árboles, bancos, ventanas y señalización urbana.
            SenalizacionUrbana.dibujar(this); // EXTRA 5: cinco senales de limite 30 en las aceras.
        }
        if (entregas < DESTINOS.length) { // Dibuja un objetivo únicamente mientras queden entregas.
            dibujarDestino(); // Coloca la marca dorada en la parada activa.
        }
    }

    /** Marca la próxima parada con una plataforma y una baliza flotante. */
    private void dibujarDestino() {
        float x = DESTINOS[entregas][0]; // Lee el X de la próxima entrega.
        float z = DESTINOS[entregas][1]; // Lee el Z de la próxima entrega.
        entero("uEmision", 1); // Hace que el objetivo sea visible incluso de noche.
        caja(x, 0.06f, z, 5, 0.08f, 5, 1, 0.72f, 0.12f); // Dibuja una marca dorada sobre el asfalto.
        if (!vistaMapa) { // Evita añadir una baliza tridimensional al mapa pequeño.
            float alturaBaliza = 3.5f + (float) Math.sin(tiempoEscena * 2) * 0.3f; // Hace oscilar la baliza suavemente.
            cajaGirada(x, alturaBaliza, z, 0.8f, 0.8f, 0.8f, 1, 0.8f, 0.15f, (float) (tiempoEscena % (Math.PI * 2))); // Dibuja el cubo giratorio del objetivo.
        }
        entero("uEmision", 0); // Devuelve a los siguientes objetos su iluminación normal.
    }

    // ==================== 5. DECORACIÓN DE LAS MANZANAS ====================

    /** Decide qué decoración corresponde a cada tipo de parcela. */
    private void decorarCiudad() {
        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las columnas de esa fila.
                float x = centro(columna); // Obtiene el centro horizontal de la parcela.
                float z = centro(fila); // Obtiene el centro de la parcela en profundidad.
                int tipo = MAPA[fila][columna]; // Lee el contenido de la celda.
                if (tipo == 2) { // Detecta una parcela de parque.
                    DecoracionCiudad.parque(this, fila, columna, tiempoEscena, noche);
                }
                if (tipo == 1) { // Detecta una parcela con edificio.
                    float altura = 5 + (fila * 3 + columna * 7) % 9; // Recupera la misma altura calculada en CiudadBase.
                    DecoracionCiudad.edificio(this, fila, columna, altura, noche);
                }
                if (tipo != 0) { // La señalización se coloca junto a las manzanas, no en celdas de calle.
                    DecoracionCiudad.acera(this, x, z);
                    dibujarPasoPeatonal(fila, columna, x, z); // CEBRAS: cruces completos entre aceras, sin duplicar tramos.
                    dibujarSemaforo(x + 4, z - 4); // Coloca el semáforo dentro de la acera.
                }
            }
        }
    }

    /** CEBRAS: enlaza aceras opuestas en calles interiores, cerca de las esquinas. */
    private void dibujarPasoPeatonal(int fila, int columna, float x, float z) {
        if (fila > 1) { // Norte: existe otra manzana al otro lado de la calle.
            pintarPaso(x - CELDA * 0.25f, z - CELDA, true);
        }
        if (columna > 1) { // Oeste: cada calle se pinta desde una sola manzana.
            pintarPaso(x - CELDA, z + CELDA * 0.25f, false);
        }
    }

    /** Las franjas son transversales al recorrido del peaton y abarcan los 10 de calle. */
    private void pintarPaso(float x, float z, boolean cruzaZ) {
        float anchoPaso = CELDA * 0.26f; // Franja peatonal de 2.6 unidades de ancho.
        float separacion = CELDA / 10; // Diez marcas uniformemente espaciadas de acera a acera.
        // Interrumpe la linea amarilla bajo el paso; mismo material que el asfalto.
        caja(x, 0.045f, z, cruzaZ ? anchoPaso + 0.2f : CELDA, 0.01f,
            cruzaZ ? CELDA : anchoPaso + 0.2f, 0.16f, 0.19f, 0.23f);
        for (int indice = 0; indice < 10; indice++) {
            float desplazamiento = -CELDA / 2 + (indice + 0.5f) * separacion;
            float franjaX = cruzaZ ? x : x + desplazamiento;
            float franjaZ = cruzaZ ? z + desplazamiento : z;
            caja(franjaX, 0.065f, franjaZ, cruzaZ ? anchoPaso : separacion * 0.55f,
                0.015f, cruzaZ ? separacion * 0.55f : anchoPaso, 0.92f, 0.94f, 0.9f);
        }
    }

    /** Construye un semáforo decorativo que alterna rojo, verde y amarillo cada 12 segundos. */
    private void dibujarSemaforo(float x, float z) {
        caja(x, 1.7f, z, 0.18f, 2.8f, 0.18f, 0.18f, 0.20f, 0.22f); // Dibuja el poste sobre la acera.
        caja(x, 3.1f, z, 0.55f, 1.2f, 0.45f, 0.08f, 0.10f, 0.12f); // Dibuja la carcasa de las tres luces.
        int activa = luzSemaforoActiva(); // FUNCIONES 3: utiliza el reloj de escena, no el cronometro de entregas.
        for (int indice = 0; indice < 3; indice++) { // Rojo arriba, amarillo al centro y verde abajo.
            boolean encendida = indice == activa;
            float brillo = 0.15f; // Conserva un color tenue cuando la bombilla está apagada.
            entero("uEmision", 0); // Configura inicialmente una superficie sin emisión.
            if (encendida) { // Comprueba si esta bombilla corresponde a la fase activa.
                brillo = 1; // Usa intensidad completa para su color.
                entero("uEmision", 1); // Hace que la bombilla se vea encendida.
            }
            float rojo = 0; // Componente roja inicialmente ausente.
            float verde = 0; // Componente verde inicialmente ausente.
            if (indice < 2) { // Rojo y amarillo necesitan componente roja.
                rojo = brillo; // Añade rojo con la intensidad elegida.
            }
            if (indice > 0) { // Amarillo y verde necesitan componente verde.
                verde = brillo; // Añade verde; rojo más verde produce amarillo.
            }
            float altura = 3.45f - indice * 0.35f; // Separa verticalmente las tres bombillas.
            caja(x, altura, z - 0.24f, 0.28f, 0.25f, 0.06f, rojo, verde, 0.02f); // Dibuja la bombilla frente a la carcasa.
        }
        entero("uEmision", 0); // Evita que el siguiente objeto herede la emisión del semáforo.
    }

    // ==================== 6. MINIMAPA: SEGUNDO PASE DE DIBUJO ====================

    /** EXTRA 5: tres capas: vista principal, minimapa y panel, en ese orden. */
    @Override
    protected void dibujarFrame() {
        super.dibujarFrame();
        dibujarMinimapa();
        if (interfaz == null) interfaz = new InterfazHud(); // Requiere que ya exista un contexto OpenGL.
        interfaz.dibujar(ancho, alto, datosHud());
        if (glGetError() != GL_NO_ERROR) throw new IllegalStateException("Error OpenGL en el HUD");
    }

    /** Dibuja la misma ciudad desde arriba en un recuadro; M no oculta el panel. */
    private void dibujarMinimapa() {
        if (!mostrarMapa) { // Comprueba si el usuario ocultó el minimapa con M.
            return; // Conserva únicamente la imagen principal.
        }
        int dimensionMenor = Math.min(ancho, alto); // Busca la dimensión que limita el espacio disponible.
        int lado = Math.min(260, dimensionMenor / 3); // Limita el mapa a 260 píxeles y a un tercio de la ventana.
        if (lado < 1) { // FUNCIONES 3: evita un viewport de cero en una ventana extremadamente pequena.
            return;
        }
        int margen = Math.min(18, dimensionMenor / 20); // Calcula una separación adaptable respecto a los bordes.
        int x = ancho - lado - margen; // Ubica el recuadro cerca del borde derecho.
        int y = alto - lado - margen; // Ubica el recuadro arriba; OpenGL mide Y desde abajo.
        glEnable(GL_SCISSOR_TEST); // Activa el recorte para no borrar el resto de la escena.
        glScissor(x - 3, y - 3, lado + 6, lado + 6); // Selecciona el mapa más un borde de tres píxeles.
        glClearColor(0.8f, 0.87f, 0.94f, 1); // Define un color claro para el marco.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Borra solo el recuadro exterior gracias al scissor.
        glScissor(x, y, lado, lado); // Reduce el recorte al interior del minimapa.
        glClearColor(0.06f, 0.10f, 0.15f, 1); // Define el fondo oscuro del mapa.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Limpia color y profundidad dentro del mapa.
        glViewport(x, y, lado, lado); // Redirige la proyección al recuadro cuadrado.
        // AMPLIACION 1: uSemiextensionMapa ya contiene LIMITE + margen; el viewport cuadrado evita deformacion.
        entero("uMapa", 1); // Selecciona la proyección ortográfica del shader de CiudadBase.
        vistaMapa = true; // Indica a escena() que omita decoración pequeña y baliza flotante.
        try { // Asegura que el estado de dibujo se restaure incluso si el segundo pase falla.
            escena(); // Dibuja otra vez la misma ciudad, ahora vista desde arriba.
            dibujarIndicadorAuto(); // Resalta la posición y el frente del jugador en el mapa.
        } finally { // El siguiente cuadro debe volver a la configuración de pantalla completa.
            vistaMapa = false; // Reactiva los detalles de la escena principal.
            entero("uMapa", 0); // Recupera la proyección en perspectiva.
            glDisable(GL_SCISSOR_TEST); // Permite que la próxima limpieza abarque toda la pantalla.
            glViewport(0, 0, ancho, alto); // Recupera el área de dibujo de la ventana completa.
        }
        if (glGetError() != GL_NO_ERROR) { // Comprueba que el pase del mapa no haya generado errores OpenGL.
            throw new IllegalStateException("Error OpenGL en minimapa"); // Expone el error en la consola.
        }
    }

    /** Dibuja una marca cian y una punta blanca por encima de los edificios del minimapa. */
    private void dibujarIndicadorAuto() {
        cajaGirada(autoX, 25, autoZ, 2.2f, 0.1f, 3.2f, 0.1f, 1, 1, angulo); // Marca la posición con un rectángulo cian orientado.
        float frenteX = -(float) Math.sin(angulo); // Calcula la dirección frontal en el eje X.
        float frenteZ = -(float) Math.cos(angulo); // Calcula la dirección frontal en el eje Z.
        float puntaX = autoX + frenteX * 2; // Desplaza la punta dos unidades hacia delante en X.
        float puntaZ = autoZ + frenteZ * 2; // Desplaza la punta dos unidades hacia delante en Z.
        caja(puntaX, 26, puntaZ, 0.9f, 0.1f, 0.9f, 1, 1, 1); // Dibuja la punta blanca encima del indicador cian.
    }

    /** EXTRA 5: datos actuales para el HUD, consultables tambien sin abrir OpenGL. */
    protected InterfazHud.Estado datosHud() {
        String[] nombres = {"NORESTE", "NOROESTE", "SUROESTE"};
        String destino = entregas < DESTINOS.length ? nombres[entregas] : "COMPLETO";
        return new InterfazHud.Estado(Math.abs(velocidad) * 3.6f, noche, faros, entregas, DESTINOS.length,
            tiempo, mostrarMapa, mostrarAyuda, trafico.activos(), sectorActual(), destino, marcha, limiteMarchaKmh(), armas.visible(), estadoChata());
    }

    /** Nombres de sectores segun la posicion del jugador, usando X y Z del mundo. */
    protected String sectorActual() {
        String horizontal = autoX < -CELDA ? "OESTE" : autoX > CELDA ? "ESTE" : "";
        if (autoZ < -CELDA) return horizontal.isEmpty() ? "NORTE" : "NOR" + horizontal;
        if (autoZ > CELDA) return horizontal.isEmpty() ? "SUR" : "SUR" + horizontal;
        return horizontal.isEmpty() ? "CENTRO" : horizontal;
    }

    /** Libera textura de letras, vertices y shaders con el contexto aun activo. */
    @Override
    protected void liberarExtras() {
        if (sonidosArmas != null) {
            sonidosArmas.close();
            sonidosArmas = null;
        }
        if (interfaz != null) {
            interfaz.close();
            interfaz = null;
        }
        super.liberarExtras();
    }

    /** Punto de entrada del proyecto final. */
    public static void main(String[] args) {
        CiudadInteractiva aplicacion = new CiudadInteractiva(); // Crea la versión con todas las etapas acumuladas.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicación.
    }
}
