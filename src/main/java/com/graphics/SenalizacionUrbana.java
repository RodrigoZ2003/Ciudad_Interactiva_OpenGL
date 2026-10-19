package com.graphics;

/** EXTRA 5: senales de limite 30 junto a los cinco parques, construidas con cajas. */
public final class SenalizacionUrbana {
    private SenalizacionUrbana() { }

    public static void dibujar(CiudadBase ciudad) {
        for (int fila = 0; fila < CiudadBase.MAPA.length; fila++) {
            for (int columna = 0; columna < CiudadBase.MAPA[fila].length; columna++) {
                if (CiudadBase.MAPA[fila][columna] == 2) {
                    // Esquina sureste: la farola y el semaforo ocupan otras esquinas.
                    limite30(ciudad, CiudadBase.centro(columna) + 4, CiudadBase.centro(fila) + 4);
                }
            }
        }
    }

    private static void limite30(CiudadBase ciudad, float x, float z) {
        ciudad.caja(x, 1.45f, z, 0.12f, 2.5f, 0.12f, 0.42f, 0.46f, 0.5f);
        ciudad.caja(x, 2.85f, z, 1.55f, 1.35f, 0.13f, 0.9f, 0.12f, 0.12f);
        for (int cara : new int[]{-1, 1}) {
            ciudad.caja(x, 2.85f, z + cara * 0.071f, 1.32f, 1.12f, 0.012f, 0.98f, 0.98f, 0.94f);
            digito(ciudad, x - cara * 0.32f, 2.85f, z + cara * 0.086f, cara, 0b1001111); // 3.
            digito(ciudad, x + cara * 0.32f, 2.85f, z + cara * 0.086f, cara, 0b0111111); // 0.
        }
    }

    /** Digitos de siete segmentos: a,b,c,d,e,f,g; las dos caras se leen en el mismo orden. */
    private static void digito(CiudadBase ciudad, float x, float y, float z, int cara, int mascara) {
        float[][] segmentos = {
            {0, 0.37f, 0.37f, 0.06f}, {0.19f, 0.19f, 0.06f, 0.34f},
            {0.19f, -0.19f, 0.06f, 0.34f}, {0, -0.37f, 0.37f, 0.06f},
            {-0.19f, -0.19f, 0.06f, 0.34f}, {-0.19f, 0.19f, 0.06f, 0.34f},
            {0, 0, 0.37f, 0.06f}
        };
        for (int i = 0; i < segmentos.length; i++) {
            if ((mascara & (1 << i)) != 0) {
                float[] s = segmentos[i];
                ciudad.caja(x + cara * s[0], y + s[1], z, s[2], s[3], 0.012f, 0.06f, 0.08f, 0.1f);
            }
        }
    }
}
