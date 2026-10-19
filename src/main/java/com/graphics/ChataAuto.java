package com.graphics;

/** Remolque de un eje con articulación: el enganche arrastra el eje sin deslizamiento lateral. */
final class ChataAuto {
    static final float DISTANCIA = 3.6f; // Centro de la plataforma cuando está alineada.
    static final float SEMIANCHO = 1.2f;
    static final float SEMILARGO = 1.2f;
    static final float DISTANCIA_ENGANCHE = 1.5f; //Del centro del auto hasta la bola de enganche.
    static final float DESFASE_EJE = .2f; // Eje ligeramente detrás del centro de carga.
    static final float BRAZO_EJE = DISTANCIA - DISTANCIA_ENGANCHE + DESFASE_EJE; //De la bola enganche a eje del remolque.
    static final float MAX_ARTICULACION = (float) Math.toRadians(75); //Giro

    record Punto(float x, float z) { }

    private ChataAuto() { }

    static Punto puntoEnganche(float x, float z, float giroAuto) {
        return local(x, z, giroAuto, 0, DISTANCIA_ENGANCHE);
    }

    static Punto eje(float x, float z, float giroAuto, float giroChata) {
        var enganche = puntoEnganche(x, z, giroAuto);
        return local(enganche.x, enganche.z, giroChata, 0, BRAZO_EJE);
    }

    /** Consulta alineada para el acoplamiento inicial. */
    static Colisiones.Rectangulo contorno(float x, float z, float giro) {
        return contorno(x, z, giro, giro);
    }

    static Colisiones.Rectangulo contorno(float x, float z, float giroAuto, float giroChata) {
        var enganche = puntoEnganche(x, z, giroAuto);
        var centro = local(enganche.x, enganche.z, giroChata, 0, BRAZO_EJE - DESFASE_EJE);
        return new Colisiones.Rectangulo(centro.x, centro.z, SEMIANCHO, SEMILARGO, giroChata);
    }

    /** Plataforma, acople y las dos barras de la lanza en A, cada uno con su contorno real. */
    static Colisiones.Rectangulo[] contornos(float x, float z, float giroAuto, float giroChata) {
        var h = puntoEnganche(x, z, giroAuto);
        var acople = local(h.x, h.z, giroChata, 0, .1f);
        return new Colisiones.Rectangulo[]{
            contorno(x, z, giroAuto, giroChata),
            new Colisiones.Rectangulo(acople.x, acople.z, .12f, .17f, giroChata),
            barra(h, giroChata, -.82f), barra(h, giroChata, .82f)
        };
    }

    private static Colisiones.Rectangulo barra(Punto h, float giro, float lateral) {
        var inicio = local(h.x, h.z, giro, 0, .18f);
        var fin = local(h.x, h.z, giro, lateral, 1.0f);
        float dx = fin.x - inicio.x, dz = fin.z - inicio.z;
        return new Colisiones.Rectangulo((inicio.x + fin.x) / 2, (inicio.z + fin.z) / 2,
            .065f, (float) Math.hypot(dx, dz) / 2, (float) Math.atan2(dx, dz));
    }

    private static Punto local(float x, float z, float giro, float lateral, float atras) {
        double cos = Math.cos(giro), sin = Math.sin(giro);
        return new Punto(x + (float) (cos * lateral + sin * atras), z + (float) (-sin * lateral + cos * atras));
    }

    /**
     * Si H es el enganche y L el brazo al eje, d(phi) = (-dHx*cos(phi) + dHz*sin(phi))/L.
     * Integramos con punto medio. El signo del desplazamiento produce la respuesta en reversa;
     * no se copia el ángulo del auto ni se interpola por tiempo. Este cálculo no modifica estado.
     */
    static float seguirEnganche(float x0, float z0, float giro0, float phi,
                               float x1, float z1, float giro1) {
        // Usar double en las diferencias evita perder desplazamientos pequeños lejos del origen.
        double dx = (double) x1 - x0 + DISTANCIA_ENGANCHE * (Math.sin(giro1) - Math.sin(giro0));
        double dz = (double) z1 - z0 + DISTANCIA_ENGANCHE * (Math.cos(giro1) - Math.cos(giro0));
        double medio = phi + (-dx * Math.cos(phi) + dz * Math.sin(phi)) / (2 * BRAZO_EJE);
        return phi + (float) ((-dx * Math.cos(medio) + dz * Math.sin(medio)) / BRAZO_EJE);
    }

    static float articulacion(float giroAuto, float giroChata) {
        return (float) Math.atan2(Math.sin(giroChata - giroAuto), Math.cos(giroChata - giroAuto));
    }

    static void dibujar(ConduccionAuto ciudad) {
        float giro = ciudad.anguloChata;
        var contornos = contornos(ciudad.autoX, ciudad.autoZ, ciudad.angulo, giro);
        var caja = contornos[0];
        float x = caja.x(), z = caja.z();
        for (int i = 1; i < contornos.length; i++) {
            var barra = contornos[i];
            ciudad.piezaEn(barra.x(), barra.z(), (float) Math.atan2(barra.seno(), barra.coseno()),
                0, .48f, 0, barra.semiancho() * 2, .16f, barra.semilargo() * 2,
                i == 1 ? .64f : .56f, i == 1 ? .68f : .12f, i == 1 ? .71f : .10f);
        }
        // Bastidor rojo y tablones separados, tomando como referencia una chata de carga.
        ciudad.piezaEn(x, z, giro, 0, .55f, 0, 1.9f, .18f, 2.3f, .56f, .12f, .10f);
        for (int i = 0; i < 7; i++) {
            ciudad.piezaEn(x, z, giro, 0, .67f, -.96f + i * .32f,
                1.76f, .1f, .3f, .62f, .43f, .25f);
        }
        for (float lado : new float[]{-1, 1}) {
            for (int fila = 0; fila < 3; fila++) {
                ciudad.piezaEn(x, z, giro, lado * .9f, .84f + fila * .23f, 0,
                    .09f, .16f, 2.3f, .67f - fila * .035f, .46f - fila * .025f, .27f);
            }
            ciudad.piezaEn(x, z, giro, lado * .9f, 1.46f, 0, .14f, .08f, 2.3f, .56f, .12f, .10f);
            for (float extremo : new float[]{-1, 0, 1}) {
                ciudad.piezaEn(x, z, giro, lado * .9f, 1.06f, extremo * 1.1f,
                    .15f, .88f, .16f, .56f, .12f, .10f);
            }
            ciudad.ruedaEn(x, z, giro, lado * 1.03f, .43f, DESFASE_EJE, .28f, .76f, .045f, .055f, .07f);
            ciudad.ruedaEn(x, z, giro, lado * 1.18f, .43f, DESFASE_EJE, .025f, .4f, .67f, .72f, .76f);
            ciudad.piezaEn(x, z, giro, lado * 1.04f, .87f, DESFASE_EJE, .32f, .1f, .96f, .56f, .12f, .10f);
            ciudad.piezaEn(x, z, giro, lado * .69f, .56f, 1.16f, .24f, .15f, .06f, .92f, .07f, .045f);
        }
        for (float extremo : new float[]{-1, 1}) {
            for (int fila = 0; fila < 3; fila++) {
                ciudad.piezaEn(x, z, giro, 0, .84f + fila * .23f, extremo * 1.1f,
                    1.7f, .16f, .1f, .61f, .4f, .23f);
            }
            ciudad.piezaEn(x, z, giro, 0, 1.46f, extremo * 1.1f, 1.8f, .08f, .13f, .56f, .12f, .10f);
        }
    }
}
