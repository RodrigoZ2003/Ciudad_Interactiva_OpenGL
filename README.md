# Ciudad y conducción con OpenGL: cuatro etapas y mejoras opcionales

Proyecto de enseñanza basado en `AppCamara.java` y `AppLaberinto.java` de tus clases: Java 17, Maven, LWJGL 3.3.3, GLFW, OpenGL 3.3 Core, shaders GLSL, VAO y VBO. Toda la geometría se genera con código; no requiere imágenes ni modelos externos.

## Ejecutar

En este equipo puedes ejecutar `.\ejecutar.cmd` desde PowerShell, en la carpeta del proyecto. Incluye Maven en `.tools` y usa el JDK instalado en `C:\Program Files\Java\jdk-21.0.10`. La primera ejecución descarga dependencias en `.tools\repository`.

Abre una terminal **en esta carpeta**, donde está `pom.xml`. Necesitas un JDK 17 o superior y Maven (`java -version`, `mvn -version`). La primera compilación descarga las dependencias.

```sh
mvn compile exec:exec -DmainClass=com.graphics.CiudadBase
mvn compile exec:exec -DmainClass=com.graphics.ConduccionAuto
mvn compile exec:exec -DmainClass=com.graphics.IluminacionCiudad
mvn compile exec:exec -DmainClass=com.graphics.CiudadInteractiva
```

Ejecuta un comando por vez; ESC cierra la ventana. `mvn compile exec:exec` abre la versión final por defecto. Esta copia está configurada para Windows x64.

Incluye bibliotecas nativas para Windows x64. Se necesita una sesión gráfica y un controlador compatible con OpenGL 3.3. No está diseñado para ejecutarse en un servidor sin pantalla.

## Ampliacion obligatoria: punto 1

La ciudad ahora contiene 11 x 11 celdas de 10 unidades: 110 x 110 unidades, 20 manzanas con edificios y 5 parques. Suelo, limites, colisiones, vista general, minimapa y entregas estan adaptados al mapa ampliado. Consulta `docs/Punto_1_Ampliacion.md` para ver las ubicaciones del codigo, formulas y guion de defensa. Iluminacion completada: 25 farolas distribuidas, faros con alcance de 24 unidades y lentes controladas con F. Consulta `docs/Punto_2_Iluminacion.md`.

## Funciones obligatorias: punto 3

Semaforos con reloj independiente de la partida: rojo 5 s, verde 5 s y amarillo 2 s, incluso despues de ganar. El titulo prioriza la informacion de la actividad y los controles N/F/M; R permite empezar otra partida completa. Pasos peatonales corregidos: 40 cruces completos entre aceras, con diez franjas cada uno y orientacion norte-sur o este-oeste. Decoracion y minimapa verificados al redimensionar la ventana. Consulta `docs/Punto_3_Funciones.md` para las ubicaciones del codigo, explicaciones, capturas y guion de demostracion.

## Mejoras opcionales: punto 5

Tres autos autonomos recorren circuitos con curvas y estado independiente. El panel dentro de la ventana muestra velocidad, marcha, límite de marcha, luces, entregas, tiempo, destino y sector; H abre/cierra la ayuda. Cinco senales de limite 30 complementan los parques. El panel se adapta al redimensionamiento y permanece visible al ocultar el minimapa. R tambien restaura el trafico. Consulta `docs/Punto_5_Mejoras_Opcionales.md` para el codigo, las formulas y la demostracion.

Las responsabilidades nuevas se separan en `TraficoAutonomo` (rutas y estado), `InterfazHud` (panel y ayuda OpenGL) y `SenalizacionUrbana` (senales de 30). Las cuatro clases originales siguen representando las etapas didacticas.

## Diseño visual de la ciudad

`DecoracionCiudad` reúne las mejoras de edificios y plazas de la versión final: fachadas con una paleta de piedra, ladrillo y tonos azulados, marcos de ventanas, cornisas, balcones, entradas con toldos, azoteas con paneles y jardines. De noche se iluminan distintas ventanas, mientras el cielo cambia entre azul diurno y azul oscuro nocturno.

Las cinco plazas incluyen senderos, árboles con copas escalonadas, jardineras con flores, bancos y balizas. Sus elementos centrales varían entre fuentes con chorros animados, pérgolas y una escultura. Las aceras tienen bordes, juntas de pavimento y papeleras; las farolas incorporan bases y cubiertas. Los detalles quedan dentro de las manzanas existentes y se omiten en el minimapa para conservar su legibilidad. No se añaden texturas ni modelos externos.

## Cómo se acumulan los avances

```text
CiudadBase: ciudad, mapa y renderizador
  └── ConduccionAuto extends CiudadBase: auto, conducción y colisiones
        └── IluminacionCiudad extends ConduccionAuto: iluminación
              └── CiudadInteractiva extends IluminacionCiudad: detalles urbanos, entregas y minimapa
```

Cada archivo tiene su propio `main` y puede ejecutarse como una etapa distinta dentro del proyecto. Las etapas posteriores necesitan los archivos anteriores: `extends` reutiliza el avance y `super.escena()` lo dibuja antes de añadir contenido. No son cuatro copias independientes. Así, en ConduccionAuto se explica el auto sin repetir la configuración de OpenGL. Los nombres describen la responsabilidad de cada etapa y siguen la convención PascalCase de Java.

## Cómo leer el código comentado

Los cuatro archivos están divididos en secciones numeradas. Las secciones y los metodos principales tienen comentarios en espanol que explican su proposito. Las llaves y líneas vacías solo separan bloques. Los shaders también están explicados línea por línea dentro de sus cadenas GLSL.

- **CiudadBase:** variables → inicio/ciclo/limpieza → teclado/cámara → ciudad → cajas/uniforms → shaders → vértices del cubo.
- **ConduccionAuto:** variables del auto → teclado → movimiento → colisiones → cámara → piezas del vehículo.
- **IluminacionCiudad:** luces → controles → envío de datos → farolas → shader de iluminación.
- **CiudadInteractiva:** partida → controles → entregas → destino → decoración → minimapa.

Se usa una instrucción por línea, condiciones con llaves y cálculos intermedios con nombres descriptivos. El cubo contiene sus 36 vértices explícitos, como en los ejemplos originales. En `caja()` y `pieza()`, los argumentos mantienen este orden: posición X/Y/Z, tamaño X/Y/Z y color rojo/verde/azul; `cajaGirada()` agrega el ángulo al final.

## Guion de enseñanza

### Clase 1 — Crear primero la ciudad

**Resultado:** una ciudad 3D con calles conectadas, marcas viales, edificios, aceras y parques. Las flechas izquierda/derecha orbitan la cámara.

1. Leer `MAPA`: 0 es calle, 1 edificio y 2 parque. Las filas corresponden a Z y las columnas a X; Y es altura.
2. Explicar `centro()`: convierte índices de matriz a coordenadas del mundo. Cada celda mide 10 unidades y el mapa ocupa 110 × 110.
3. Revisar `crearCubo()`: posiciones y normales en VBO, atributos en VAO y 36 vértices.
4. Seguir `caja()` y `cajaGirada()`: un cubo unitario se transforma en asfalto, edificio o línea.
5. Leer `vertexShader()`: modelo, cámara y perspectiva. `uMapa` deja preparado el segundo tipo de proyección que se usa en CiudadInteractiva.
6. Recorrer `run()`, `iniciar()`, `loop()`, `dibujarFrame()` y `limpiar()`: eventos, actualización, limpieza, dibujo y presentación.

**Ejercicio:** cambiar una manzana de edificio a parque y variar las alturas. Mantener calles conectadas. Las normales se preparan aquí, pero la iluminación se introduce en IluminacionCiudad.

### Clase 2 — Crear y mover el auto

**Resultado:** auto rojo con ventanas y marcos, espejos, parrilla, parachoques, placas, neumáticos y llantas cilíndricas; bocina con B, cámara de seguimiento y colisiones con manzanas y borde. `BocinaAuto` genera y reproduce el sonido en otro hilo; `MallaRueda` reutiliza una malla para las ruedas del jugador y del tráfico.

1. Leer `dibujarAuto()` y `pieza()`: las piezas usan coordenadas locales que giran y se trasladan juntas.
2. Estudiar `actualizar(deltaTime)`: aceleración, resistencia, freno y límites de velocidad.
3. Relacionar seno/coseno con la dirección del auto. Su frente local es -Z; el giro está en radianes.
4. Explicar el giro proporcional a la velocidad y el cambio de dirección al retroceder.
5. Revisar `puedeCircular()` y `Colisiones`: rectángulos orientados y separación sobre sus ejes (SAT).
6. Alternar cámaras con C para observar la conducción en el mapa.

**Ejercicio:** modificar aceleración y resistencia y comparar el manejo. La colisión usa un rectángulo de 2.09 × 2.75 unidades que gira con el auto e incluye espejos, llantas y parachoques. El movimiento se divide en pasos cortos y se ajusta hasta el contacto, evitando detenerse un cuadro entero antes de la acera. El choque de conducción detiene al auto; se puede retroceder para separarse. No hay rebotes ni daños por contacto; los proyectiles sí destruyen vehículos del tráfico.

En `CiudadInteractiva` también hay colisiones con el tráfico: el jugador no atraviesa a los otros vehículos y estos se detienen cuando el jugador les bloquea el paso. Retoman su ruta al quedar libre. Las comprobaciones incluyen el giro en las curvas y se comparten mediante `Colisiones`.

### Clase 3 — Focos e iluminación

**Resultado:** ambiente nocturno, 25 farolas que iluminan superficies, faros del vehículo y cambio día/noche.

1. Comparar el shader de color plano de CiudadBase con `fragmentShader()` de IluminacionCiudad.
2. Estudiar normales y Lambert: `max(dot(normal, direccionLuz), 0)`.
3. Revisar ambiente y luz direccional del sol.
4. Relacionar las coordenadas de `LUCES` con los postes dibujados en `escena()`.
5. Analizar cómo disminuye la luz con la distancia.
6. Explicar el cono de los faros con producto escalar y `smoothstep`.
7. Comparar emisión de la bombilla con la luz calculada sobre el suelo. Son fenómenos distintos.

**Ejercicio:** variar el color y la atenuación de una farola. La iluminación es local, sin sombras ni oclusión: una luz puede atravesar un edificio. Implementar shadow maps queda como ampliación. El cielo también cambia de color entre día y noche.

### Clase 4 — Ciudad final y minimapa

**Resultado:** plazas con fuentes, pérgolas, árboles y jardines, edificios detallados y ventanas iluminadas, pasos peatonales, semáforos animados, minimapa y juego de tres entregas.

1. Estudiar `decorarCiudad()` y los métodos de `DecoracionCiudad`, `dibujarPasoPeatonal()` y `dibujarSemaforo()`: composición de objetos reutilizando cajas y la matriz.
2. Leer `DESTINOS` y `actualizarActividad()`: acercarse a menos de 3 unidades y frenar a menos de 1 unidad/segundo completa una entrega.
3. Seguir `dibujarFrame()`: primero la cámara principal, después una vista ortográfica en otro viewport.
4. Explicar `glScissor`: permite borrar solo el recuadro del minimapa y su profundidad.
5. Mostrar por qué se restauran viewport, scissor y `uMapa` al terminar.
6. Seguir el indicador cian del auto y su punta blanca; la marca dorada indica el destino activo.

**Ejercicio:** agregar una cuarta entrega sobre una calle o modificar el tamaño del minimapa. El minimapa mantiene el norte (-Z) arriba. El destino se muestra también como baliza dorada en el mundo. Al completar tres entregas aparece GANASTE en el título; R reinicia.

## Controles

| Tecla | Acción | Desde |
|---|---|---|
| Izquierda / derecha | Orbitar la ciudad | Clase 1 únicamente |
| W / S o arriba / abajo | Acelerar / frenar y retroceder | Clase 2 |
| A / D o izquierda / derecha | Girar mientras el auto se mueve | Clase 2 |
| Espacio | Freno | Clase 2 |
| B | Tocar bocina (un toque por pulsación) | Clase 2 |
| T | Poner / quitar la chata trasera | Clase 2 |
| 1 / 2 / 3 / 4 / 5 | Seleccionar marcha (también con teclado numérico) | Clase 2 |
| C | Cámara de seguimiento / aérea oblicua | Clase 2 |
| R | Reiniciar auto y, en CiudadInteractiva, las entregas | Clase 2 |
| N | Día / noche | Clase 3 |
| F | Encender / apagar faros | Clase 3 |
| M | Mostrar / ocultar minimapa | Clase 4 |
| H | Abrir / cerrar ayuda dentro de la ventana | Clase 4 |
| K | Desplegar / ocultar lanzamisiles y dos metralletas | Clase 4 |
| Clic izquierdo | Disparar las dos metralletas; mantener para ráfagas | Clase 4, armas desplegadas |
| Clic derecho | Lanzar un misil; recarga de 1 segundo | Clase 4, armas desplegadas |
| ESC | Salir | Todas |

El auto tiene cinco marchas manuales: **1 → 20 km/h, 2 → 40 km/h, 3 → 60 km/h, 4 → 100 km/h y 5 → 150 km/h**. W o arriba sigue siendo el acelerador. Subir de marcha permite acelerar hasta el nuevo máximo; bajar aplica el límite menor inmediatamente. El panel muestra la marcha y su máximo. R reinicia en primera. S conserva el frenado y la reversa, limitada a 21.6 km/h (20 km/h en primera).

**Chata con T:** empieza desacoplada y se coloca detrás del auto con plataforma de madera, bastidor rojo, barandas de tablones, ruedas, guardabarros y lanza en A. Tiene un ángulo propio: el movimiento del enganche arrastra su eje, sigue la curva con retraso y se endereza al avanzar. En reversa se empuja el remolque y aumenta el plegado si no se corrige el volante. La articulación mantiene la distancia al eje; detenido, no se alinea automáticamente. El conjunto se detiene si alguna parte toca una manzana, el borde, otro auto o la propia carrocería; también tiene un tope de articulación de 75 grados. Se puede maniobrar en una dirección libre para separarse. El tráfico respeta la posición articulada real. Si no cabe al colocarla, T muestra un aviso. Otra T la quita; R reinicia sin chata. Se dibuja también en el minimapa. Consulta `docs/Chata_Auto.md` para conocer la física, los archivos y el motivo de cada cambio.

En la versión final, las armas empiezan ocultas. K muestra un lanzamisiles sobre el techo y una metralleta a cada lado; otra pulsación las oculta y corta las ráfagas. Disparan hacia el frente del auto. Cada impacto de bala o misil destruye el vehículo del tráfico alcanzado, con explosión visual y sonora, y lo retira del escenario, minimapa y colisiones. Las manzanas bloquean los proyectiles. Los disparos ya lanzados siguen su recorrido aunque se oculten las armas; tienen duración limitada. R restaura los tres autos, limpia proyectiles y explosiones y vuelve a ocultar las armas. Cambiar de ventana detiene la ráfaga.

`ArmamentoAuto` gestiona armas, proyectiles y explosiones; `SonidosArmas` genera los sonidos de metralleta, misil y explosión con Java Sound, sin archivos externos. Los efectos se reproducen en otro hilo con un número limitado de canales. La colisión de proyectiles recorre el segmento entre posiciones y considera la orientación del objetivo para evitar saltarlo a alta velocidad. Es un sistema arcade en el plano X/Z, sin daño al jugador ni destrucción de edificios.

El titulo de la ventana muestra primero velocidad en km/h (se supone una unidad = un metro), entregas/resultado, tiempo y estado de N/F/M. Desde 1000 pixeles de ancho agrega las instrucciones de conduccion. Una ventana extremadamente estrecha puede recortar el titulo. El motor permite alcanzar el máximo de cada marcha mientras se mantiene el acelerador y no hay obstáculos. Los semáforos son decorativos: cambian de color pero no bloquean al vehículo. Incluye tres autos autonomos, panel y ayuda H. Los autos siguen rutas predefinidas y no negocian prioridad entre vehiculos. No hay peatones, sombras, modelos importados ni ruedas animadas. La bocina genera su sonido con Java Sound; no requiere archivos de audio. Si no hay dispositivo de sonido disponible, se informa en consola y el juego continúa.

## Verificación

```sh
mvn test
```

Las 69 pruebas incluyen dieciocho verificaciones de chata: tecla T y reinicio, acoplamiento sin solapamientos, choques contra aceras y bordes, giro junto a esquinas, tráfico en ambos sentidos, liberación al quitarla, lanza en A y geometría. También comprueban seguimiento de curvas a izquierda y derecha, reversa, enderezamiento, eje sin deslizamiento lateral, longitud constante del enganche, estabilidad entre pasos, autocolisión y ausencia de giro acumulado contra un obstáculo. Las otras 51 verifican armas, marchas, bocina, auto, colisiones, iluminación, entregas, tráfico y HUD, sin abrir ventanas. Las llegadas se simulan en los destinos; la conducción manual completa forma parte de la demostración. Para un arranque gráfico breve puede pasarse `-Ddemo.frames=6` a la **JVM del juego**; al llegar a ese número de cuadros la ventana se cierra. Esta prueba necesita pantalla y comprueba también compilación/enlace de shaders y errores OpenGL.

Práctica manual: ejecutar cada etapa; conducir hasta una acera, comprobar el contacto frontal y lateral con otro auto; bloquear el tráfico y liberar su paso; retroceder; cambiar cámara; alternar N/F; pulsar B para oír la bocina; redimensionar la ventana; alternar M; completar las tres entregas y reiniciar con R.

Verificado en este equipo: las cuatro etapas abrieron un contexto gráfico, dibujaron seis cuadros y cerraron sin errores OpenGL. Con Java 25 y LWJGL 3.3.3 aparecen advertencias de acceso nativo, Unsafe y versión JNI; para impartir las clases se recomienda usar JDK 17, la versión objetivo de los ejemplos originales. El arranque breve no sustituye completar manualmente el recorrido.

## Documento breve de entrega

Consulta `docs/Documento_Breve_Ciudad_Interactiva_OpenGL.pdf`: documento de la entrega anterior. Para los nombres de clase y controles actuales, incluida la bocina B, utiliza este README.
