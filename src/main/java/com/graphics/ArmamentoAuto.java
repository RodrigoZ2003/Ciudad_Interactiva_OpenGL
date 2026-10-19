package com.graphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** Armas de videojuego: estado independiente del renderizado, sin alterar la conducción. */
public final class ArmamentoAuto {
    private final Consumer<SonidosArmas.Tipo> sonido;
    private final List<Proyectil> proyectiles = new ArrayList<>();
    private final List<Explosion> explosiones = new ArrayList<>();
    private boolean visible;
    private float recargaBalas, recargaMisil, destello;

    public ArmamentoAuto(Consumer<SonidosArmas.Tipo> sonido) { this.sonido = sonido; }
    public boolean visible() { return visible; }
    public void alternar() { visible = !visible; destello = 0; }
    List<Proyectil> proyectiles() { return Collections.unmodifiableList(proyectiles); }
    int explosionesActivas() { return explosiones.size(); }

    public boolean dispararMetralletas(float x, float z, float giro, float velocidad) {
        if (!visible || recargaBalas > .00001f || proyectiles.size() > 126) return false;
        lanzar(x, z, giro, velocidad, -.91f, -1.28f, false);
        lanzar(x, z, giro, velocidad, .91f, -1.28f, false);
        recargaBalas = .12f;
        destello = .06f;
        sonido.accept(SonidosArmas.Tipo.METRALLETA);
        return true;
    }

    public boolean dispararMisil(float x, float z, float giro, float velocidad) {
        if (!visible || recargaMisil > .00001f || proyectiles.size() >= 128) return false;
        lanzar(x, z, giro, velocidad, 0, -.8f, true);
        recargaMisil = 1;
        sonido.accept(SonidosArmas.Tipo.MISIL);
        return true;
    }

    private void lanzar(float x, float z, float giro, float velocidad, float lateral, float frente, boolean misil) {
        float seno = (float) Math.sin(giro), coseno = (float) Math.cos(giro);
        proyectiles.add(new Proyectil(x + coseno * lateral + seno * frente,
            z - seno * lateral + coseno * frente, giro, (misil ? 45 : 80) + Math.max(0, velocidad), misil));
    }

    /** La cadencia usa segundos; los proyectiles existentes siguen volando al ocultar las armas. */
    public void actualizar(float dt, float x, float z, float giro, float velocidad, boolean gatillo, TraficoAutonomo trafico) {
        if (!Float.isFinite(dt) || dt < 0) throw new IllegalArgumentException("Tiempo de armas inválido");
        int pasos = Math.max(1, (int) Math.ceil(dt / .02));
        float paso = dt / pasos;
        for (int i = 0; i < pasos; i++) {
            recargaBalas = Math.max(0, recargaBalas - paso);
            recargaMisil = Math.max(0, recargaMisil - paso);
            destello = Math.max(0, destello - paso);
            for (Explosion explosion : explosiones) explosion.edad += paso;
            explosiones.removeIf(e -> e.edad >= .7f);
            if (gatillo && visible) dispararMetralletas(x, z, giro, velocidad);
            avanzarProyectiles(paso, trafico);
        }
    }

    private void avanzarProyectiles(float dt, TraficoAutonomo trafico) {
        var iterador = proyectiles.iterator();
        while (iterador.hasNext()) {
            Proyectil p = iterador.next();
            float avance = p.velocidad * dt;
            float nx = p.x - (float) Math.sin(p.giro) * avance;
            float nz = p.z - (float) Math.cos(p.giro) * avance;
            float radio = p.misil ? .15f : .045f;
            float impacto = Float.POSITIVE_INFINITY;
            TraficoAutonomo.Vehiculo objetivo = null;
            // Las aceras/manzanas bloquean el disparo antes de que alcance un auto detrás.
            for (int fila = 0; fila < CiudadBase.MAPA.length; fila++) {
                for (int columna = 0; columna < CiudadBase.MAPA[fila].length; columna++) {
                    if (CiudadBase.MAPA[fila][columna] == 0) continue;
                    var acera = new Colisiones.Rectangulo(CiudadBase.centro(columna), CiudadBase.centro(fila),
                        CiudadBase.CELDA / 2, CiudadBase.CELDA / 2, 0);
                    impacto = Math.min(impacto, Colisiones.interseccionSegmento(p.x, p.z, nx, nz, acera, radio));
                }
            }
            for (var auto : trafico.vehiculos()) {
                if (!auto.activo()) continue;
                float contacto = Colisiones.interseccionSegmento(p.x, p.z, nx, nz,
                    Colisiones.auto(auto.x(), auto.z(), auto.angulo()), radio);
                if (contacto < impacto) {
                    impacto = contacto;
                    objetivo = auto;
                }
            }
            if (Float.isFinite(impacto)) {
                float hitX = p.x + (nx - p.x) * impacto, hitZ = p.z + (nz - p.z) * impacto;
                if (objetivo != null) {
                    objetivo.destruir(); // Un impacto de cualquiera de las armas destruye el auto.
                    explotar(objetivo.x(), objetivo.z(), true);
                } else if (p.misil) {
                    explotar(hitX, hitZ, false);
                }
                iterador.remove();
                continue;
            }
            p.x = nx;
            p.z = nz;
            p.recorrido += avance;
            p.vida -= dt;
            if (p.vida <= 0 || Math.abs(nx) > CiudadBase.LIMITE || Math.abs(nz) > CiudadBase.LIMITE) iterador.remove();
        }
    }

    private void explotar(float x, float z, boolean auto) {
        if (explosiones.size() >= 32) explosiones.remove(0);
        explosiones.add(new Explosion(x, z, auto ? 1 : .65f));
        sonido.accept(SonidosArmas.Tipo.EXPLOSION);
    }

    public void reiniciar() {
        visible = false;
        proyectiles.clear();
        explosiones.clear();
        recargaBalas = recargaMisil = destello = 0;
    }

    /** Composición con las mismas primitivas del auto; K no cambia sus colisiones. */
    public void dibujar(ConduccionAuto ciudad) {
        if (ciudad.vistaMapa) return;
        if (visible) {
            ciudad.pieza(0, 1.47f, .05f, .65f, .1f, .65f, .16f, .19f, .21f);
            ciudad.pieza(0, 1.68f, 0, .42f, .32f, 1.5f, .28f, .34f, .20f);
            ciudad.pieza(0, 1.68f, -.76f, .35f, .25f, .035f, .025f, .03f, .035f);
            ciudad.pieza(0, 1.68f, -.79f, .19f, .16f, .12f, .73f, .76f, .67f);
            for (float lado : new float[]{-.91f, .91f}) {
                ciudad.pieza(lado, .87f, -.15f, .22f, .26f, .5f, .15f, .19f, .22f);
                ciudad.pieza(lado, .87f, -.79f, .11f, .11f, .82f, .08f, .10f, .12f);
                ciudad.pieza(lado, .87f, -1.22f, .17f, .17f, .12f, .25f, .28f, .31f);
                if (destello > 0) {
                    ciudad.entero("uEmision", 1);
                    ciudad.pieza(lado, .87f, -1.36f, .22f, .22f, .21f, 1, .76f, .13f);
                    ciudad.entero("uEmision", 0);
                }
            }
        }
        ciudad.entero("uEmision", 1);
        for (Proyectil p : proyectiles) {
            float altura = p.misil ? .87f + .81f * Math.max(0, 1 - p.recorrido / 5) : .87f;
            if (p.misil) {
                ciudad.cajaGirada(p.x, altura, p.z, .2f, .2f, .65f, .73f, .77f, .70f, p.giro);
                float colaX = p.x + (float) Math.sin(p.giro) * .42f;
                float colaZ = p.z + (float) Math.cos(p.giro) * .42f;
                ciudad.cajaGirada(colaX, altura, colaZ, .15f, .15f, .32f, 1, .48f, .06f, p.giro);
                ciudad.cajaGirada(p.x, altura, p.z, .4f, .06f, .18f, .35f, .39f, .36f, p.giro);
            } else {
                ciudad.cajaGirada(p.x, altura, p.z, .065f, .065f, .45f, 1, .84f, .25f, p.giro);
            }
        }
        for (Explosion e : explosiones) {
            for (int i = 0; i < 12; i++) {
                double direccion = i * Math.PI * 2 / 12;
                float radio = e.edad * 5 * e.escala;
                float tamano = (.7f - e.edad) * e.escala;
                float x = e.x + (float) Math.cos(direccion) * radio;
                float z = e.z + (float) Math.sin(direccion) * radio;
                ciudad.cajaGirada(x, .6f + e.edad * (2 + i % 3), z, tamano, tamano, tamano,
                    1 - e.edad, .65f * (1 - e.edad), .08f, i + e.edad * 4);
            }
        }
        ciudad.entero("uEmision", 0);
    }

    static final class Proyectil {
        float x, z, recorrido, vida;
        final float giro, velocidad;
        final boolean misil;
        Proyectil(float x, float z, float giro, float velocidad, boolean misil) {
            this.x = x; this.z = z; this.giro = giro; this.velocidad = velocidad; this.misil = misil;
            vida = misil ? 3 : 1.5f;
        }
    }

    private static final class Explosion {
        final float x, z, escala;
        float edad;
        Explosion(float x, float z, float escala) { this.x = x; this.z = z; this.escala = escala; }
    }
}
