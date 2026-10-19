package com.graphics;

/** Detalles visuales contenidos en las manzanas: no modifican calles ni colisiones. */
public final class DecoracionCiudad {
    private static final float[][] FACHADAS = {
        {.70f, .61f, .48f}, {.32f, .48f, .55f}, {.70f, .43f, .32f},
        {.58f, .65f, .63f}, {.49f, .54f, .64f}, {.78f, .72f, .59f}
    };
    private DecoracionCiudad() { }

    public static float[] colorEdificio(int fila, int columna) {
        return FACHADAS[(fila / 2 * 3 + columna / 2) % FACHADAS.length];
    }

    public static void acera(CiudadBase c, float x, float z) {
        // Bordes claros y juntas del pavimento sobre la acera existente.
        for (int lado : new int[]{-1, 1}) {
            c.caja(x + lado * 4.87f, .32f, z, .18f, .06f, 9.8f, .78f, .78f, .72f);
            c.caja(x, .32f, z + lado * 4.87f, 9.8f, .06f, .18f, .78f, .78f, .72f);
            for (int i = -4; i <= 4; i += 2) {
                c.caja(x + i, .307f, z + lado * 4.2f, .025f, .012f, 1.1f, .42f, .46f, .47f);
                c.caja(x + lado * 4.2f, .307f, z + i, 1.1f, .012f, .025f, .42f, .46f, .47f);
            }
        }
        // Papelera en la esquina libre, sin ocupar el cruce peatonal.
        c.caja(x - 4.15f, .64f, z + 4.15f, .38f, .65f, .38f, .15f, .25f, .23f);
        c.caja(x - 4.15f, .99f, z + 4.15f, .44f, .08f, .44f, .10f, .16f, .17f);
    }

    public static void edificio(CiudadBase c, int fila, int columna, float altura, boolean noche) {
        float x = CiudadBase.centro(columna), z = CiudadBase.centro(fila);
        int estilo = (fila / 2 + columna / 2) % 3;
        float[] color = colorEdificio(fila, columna);
        // Zócalo, cornisa y barandilla de azotea.
        c.caja(x, .52f, z, 7.08f, .42f, 7.08f, .24f, .28f, .29f);
        for (int lado : new int[]{-1, 1}) {
            c.caja(x + lado * 3.51f, altura + .72f, z, .12f, .4f, 7.14f, .62f, .65f, .63f);
            c.caja(x, altura + .72f, z + lado * 3.51f, 7.14f, .4f, .12f, .62f, .65f, .63f);
        }
        // Caseta y paneles en unas azoteas; jardín en otras.
        c.caja(x + 1.4f, altura + .88f, z + 1.2f, 1.6f, .8f, 1.5f, .35f, .40f, .41f);
        c.caja(x + 1.4f, altura + 1.33f, z + 1.2f, 1.75f, .12f, 1.65f, .20f, .24f, .25f);
        if (estilo == 1) {
            c.caja(x - 1.6f, altura + .62f, z, 1.6f, .2f, 3.3f, .13f, .30f, .23f);
            for (int i = -1; i <= 1; i++) c.caja(x - 1.6f, altura + .9f, z + i, .9f, .6f, .8f, .27f, .43f, .23f);
        } else {
            for (int i = 0; i < 2; i++) {
                c.caja(x - 1.5f, altura + .65f, z - 1.3f + i * 1.6f, 2.1f, .12f, 1.3f, .12f, .25f, .36f);
                c.caja(x - 1.5f, altura + .72f, z - 1.3f + i * 1.6f, .035f, .02f, 1.3f, .58f, .70f, .76f);
            }
        }
        // Una transformación local permite decorar las cuatro fachadas de forma consistente.
        for (int cara = 0; cara < 4; cara++) {
            float giro = (float) (cara * Math.PI / 2);
            for (float lateral : new float[]{-3.28f, 3.28f}) {
                fachada(c, x, z, giro, lateral, altura / 2 + .3f, -3.55f, .19f, altura, .13f,
                    .83f, .80f, .70f);
            }
            int piso = 0;
            for (float y = 1.7f; y < altura; y += 2, piso++) {
                if (estilo != 1) fachada(c, x, z, giro, 0, y + .70f, -3.57f, 6.5f, .13f, .17f, .75f, .73f, .66f);
                for (int ventana = -1; ventana <= 1; ventana++) {
                    float lateral = ventana * 2;
                    fachada(c, x, z, giro, lateral, y, -3.535f, 1.32f, 1.22f, .06f, .14f, .20f, .23f);
                    boolean encendida = noche && Math.floorMod(fila * 7 + columna * 3 + piso * 5 + ventana + cara * 2, 5) != 0;
                    c.entero("uEmision", encendida ? 1 : 0);
                    fachada(c, x, z, giro, lateral, y, -3.58f, 1.12f, 1.02f, .025f,
                        encendida ? .98f : .23f, encendida ? .76f : .43f, encendida ? .43f : .52f);
                    c.entero("uEmision", 0);
                    fachada(c, x, z, giro, lateral, y, -3.60f, .055f, 1.04f, .025f, .52f, .61f, .62f);
                    if (estilo == 2 && piso > 0 && ventana != 0) {
                        fachada(c, x, z, giro, lateral, y - .58f, -3.8f, 1.55f, .13f, .65f, .60f, .59f, .53f);
                        fachada(c, x, z, giro, lateral, y - .26f, -4.08f, 1.55f, .52f, .08f, .22f, .31f, .33f);
                    }
                }
            }
            // Entrada y toldo comercial en dos fachadas opuestas.
            if (cara % 2 == 0) {
                fachada(c, x, z, giro, 0, 1.02f, -3.64f, 1.32f, 1.4f, .10f, .12f, .25f, .30f);
                fachada(c, x, z, giro, 0, 1.02f, -3.71f, .06f, 1.4f, .035f, .74f, .76f, .72f);
                fachada(c, x, z, giro, 0, 1.85f, -3.98f, 2.3f, .15f, .85f, color[0] * .6f, .25f, .23f);
                for (int i = -2; i <= 2; i++) fachada(c, x, z, giro, i * .4f, 1.94f, -3.98f, .18f, .025f, .85f, .89f, .84f, .68f);
                fachada(c, x, z, giro, 0, 2.15f, -3.64f, 2.3f, .32f, .09f, .20f, .32f, .34f);
            }
        }
    }

    private static void fachada(CiudadBase c, float x, float z, float giro, float lx, float y, float lz,
                                  float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(giro), seno = (float) Math.sin(giro);
        c.cajaGirada(x + coseno * lx + seno * lz, y, z - seno * lx + coseno * lz, sx, sy, sz, r, g, b, giro);
    }

    public static void parque(CiudadBase c, int fila, int columna, double tiempo, boolean noche) {
        float x = CiudadBase.centro(columna), z = CiudadBase.centro(fila);
        int variante = fila == 5 && columna == 5 ? 0 : (fila / 2 + columna / 2) % 3;
        c.caja(x, .39f, z, 1.35f, .06f, 9.3f, .76f, .71f, .59f);
        c.caja(x, .395f, z, 9.3f, .06f, 1.35f, .76f, .71f, .59f);
        c.caja(x, .43f, z, 3.2f, .09f, 3.2f, .69f, .66f, .57f);
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                arbol(c, x + sx * 2.85f, z + sz * 2.85f, (fila + columna + sx + sz) % 3);
                jardinera(c, x + sx * 1.65f, z + sz * 3.65f, sx == sz);
            }
        }
        banco(c, x - 2.5f, z, (float) Math.PI / 2);
        banco(c, x + 2.5f, z, -(float) Math.PI / 2);
        if (variante == 0) fuente(c, x, z, tiempo, noche);
        else if (variante == 1) pergola(c, x, z);
        else {
            c.caja(x, .73f, z, 1.35f, .5f, 1.35f, .70f, .71f, .66f);
            c.cajaGirada(x, 1.55f, z, .65f, 1.15f, .65f, .28f, .52f, .54f, .6f);
            c.cajaGirada(x, 2.36f, z, 1.15f, .48f, 1.15f, .69f, .47f, .22f, -.4f);
        }
        // Balizas cálidas a ambos lados del sendero; emisión visual, sin sumar focos al shader.
        for (int lado : new int[]{-1, 1}) {
            c.caja(x + lado * .95f, .7f, z + 3.7f, .16f, .65f, .16f, .17f, .23f, .23f);
            c.entero("uEmision", noche ? 1 : 0);
            c.caja(x + lado * .95f, 1.03f, z + 3.7f, .22f, .17f, .22f, .98f, .80f, .46f);
            c.entero("uEmision", 0);
        }
    }

    private static void arbol(CiudadBase c, float x, float z, int variante) {
        c.caja(x, .48f, z, 1.65f, .2f, 1.65f, .40f, .44f, .32f);
        c.caja(x, 1.25f, z, .28f, 1.6f, .28f, .37f, .25f, .15f);
        c.cajaGirada(x, 2.4f, z, 1.8f, 1.25f, 1.8f, .16f, .39f + variante * .025f, .22f, .2f);
        c.cajaGirada(x + .13f, 3.05f, z - .12f, 1.35f, .85f, 1.35f, .25f, .49f, .27f, -.25f);
        c.caja(x, 3.53f, z, .7f, .4f, .7f, .36f, .55f, .29f);
    }

    private static void jardinera(CiudadBase c, float x, float z, boolean calida) {
        c.caja(x, .52f, z, .85f, .28f, 1.15f, .59f, .51f, .40f);
        c.caja(x, .73f, z, .72f, .22f, 1, .20f, .38f, .20f);
        for (int i = -1; i <= 1; i++) c.caja(x + i * .2f, .89f, z + i * .24f, .18f, .16f, .18f,
            calida ? .96f : .70f, calida ? .66f : .32f, calida ? .20f : .55f);
    }

    private static void banco(CiudadBase c, float x, float z, float giro) {
        for (int i = -1; i <= 1; i++) fachada(c, x, z, giro, 0, .74f, i * .2f, 1.6f, .12f, .16f, .60f, .38f, .19f);
        for (float lado : new float[]{-.6f, .6f}) fachada(c, x, z, giro, lado, .54f, 0, .12f, .42f, .55f, .18f, .23f, .22f);
        fachada(c, x, z, giro, 0, 1.08f, .27f, 1.6f, .46f, .12f, .60f, .38f, .19f);
    }

    private static void fuente(CiudadBase c, float x, float z, double tiempo, boolean noche) {
        c.caja(x, .57f, z, 2.65f, .28f, 2.65f, .48f, .57f, .59f);
        for (int lado : new int[]{-1, 1}) {
            c.caja(x + lado * 1.21f, .79f, z, .23f, .27f, 2.65f, .75f, .77f, .71f);
            c.caja(x, .79f, z + lado * 1.21f, 2.65f, .27f, .23f, .75f, .77f, .71f);
        }
        c.entero("uEmision", noche ? 1 : 0);
        c.caja(x, .74f, z, 2.16f, .05f, 2.16f, .16f, .55f, .66f);
        for (int i = 0; i < 4; i++) {
            double fase = tiempo * 2.5 + i * Math.PI / 2;
            float altura = .65f + .16f * (float) Math.sin(fase);
            float dx = i % 2 == 0 ? .4f : -.4f, dz = i < 2 ? .4f : -.4f;
            c.caja(x + dx, .78f + altura / 2, z + dz, .065f, altura, .065f, .53f, .84f, .90f);
            c.caja(x + dx, .83f + altura, z + dz, .13f, .10f, .13f, .74f, .94f, .97f);
        }
        c.entero("uEmision", 0);
    }

    private static void pergola(CiudadBase c, float x, float z) {
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1})
            c.caja(x + sx * 1.16f, 1.58f, z + sz * 1.16f, .17f, 2.3f, .17f, .47f, .32f, .19f);
        for (int i = -3; i <= 3; i++)
            c.caja(x + i * .39f, 2.83f, z, .16f, .16f, 2.8f, .61f, .43f, .25f);
        c.caja(x, 2.72f, z - 1.15f, 2.8f, .18f, .17f, .42f, .30f, .20f);
        c.caja(x, 2.72f, z + 1.15f, 2.8f, .18f, .17f, .42f, .30f, .20f);
        c.caja(x + 1.1f, 2.97f, z, .7f, .22f, 2.3f, .21f, .39f, .23f);
    }
}
