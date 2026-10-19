package com.graphics;

/** Colisiones en el plano X/Z, compartidas por el jugador y el tráfico. */
final class Colisiones {
    // Extremos del modelo: espejos/llantas en X y placas/parachoques en Z.
    static final float SEMIANCHO_AUTO = 1.045f;
    static final float SEMILARGO_AUTO = 1.375f;

    private Colisiones() { }

    record Rectangulo(float x, float z, float semiancho, float semilargo, double coseno, double seno) {
        Rectangulo(float x, float z, float semiancho, float semilargo, float giro) {
            this(x, z, semiancho, semilargo, Math.cos(giro), Math.sin(giro));
        }

        double radioEn(double ejeX, double ejeZ) {
            return semiancho * Math.abs(ejeX * coseno - ejeZ * seno)
                + semilargo * Math.abs(ejeX * seno + ejeZ * coseno);
        }
    }

    static Rectangulo auto(float x, float z, float giro) {
        return new Rectangulo(x, z, SEMIANCHO_AUTO, SEMILARGO_AUTO, giro);
    }

    /** SAT: basta un eje separado para descartar el choque entre dos rectángulos. */
    static boolean chocan(Rectangulo a, Rectangulo b) {
        return solapanEn(a, b, a.coseno, -a.seno)
            && solapanEn(a, b, a.seno, a.coseno)
            && solapanEn(a, b, b.coseno, -b.seno)
            && solapanEn(a, b, b.seno, b.coseno);
    }

    private static boolean solapanEn(Rectangulo a, Rectangulo b, double ejeX, double ejeZ) {
        double distancia = Math.abs((b.x - a.x) * ejeX + (b.z - a.z) * ejeZ);
        // El contacto exacto se permite; únicamente la penetración bloquea el avance.
        return distancia < a.radioEn(ejeX, ejeZ) + b.radioEn(ejeX, ejeZ);
    }

    static boolean chocanAutos(float x, float z, float giro, float otroX, float otroZ, float otroGiro) {
        return chocan(auto(x, z, giro), auto(otroX, otroZ, otroGiro));
    }

    static boolean dentroDelMapa(Rectangulo auto, float limite) {
        return Math.abs(auto.x) + auto.radioEn(1, 0) <= limite
            && Math.abs(auto.z) + auto.radioEn(0, 1) <= limite;
    }

    /** Primer contacto de un segmento con el rectángulo; evita que un proyectil rápido lo salte. */
    static float interseccionSegmento(float x0, float z0, float x1, float z1, Rectangulo caja, float radio) {
        double dx = x0 - caja.x, dz = z0 - caja.z;
        double[] inicio = {dx * caja.coseno - dz * caja.seno, dx * caja.seno + dz * caja.coseno};
        double[] avance = {(x1 - x0) * caja.coseno - (z1 - z0) * caja.seno,
            (x1 - x0) * caja.seno + (z1 - z0) * caja.coseno};
        double[] mitad = {caja.semiancho + radio, caja.semilargo + radio};
        double entrada = 0, salida = 1;
        for (int eje = 0; eje < 2; eje++) {
            if (Math.abs(avance[eje]) < 1e-10) {
                if (Math.abs(inicio[eje]) > mitad[eje]) return Float.POSITIVE_INFINITY;
            } else {
                double a = (-mitad[eje] - inicio[eje]) / avance[eje];
                double b = (mitad[eje] - inicio[eje]) / avance[eje];
                entrada = Math.max(entrada, Math.min(a, b));
                salida = Math.min(salida, Math.max(a, b));
                if (entrada > salida) return Float.POSITIVE_INFINITY;
            }
        }
        return (float) entrada;
    }
}
