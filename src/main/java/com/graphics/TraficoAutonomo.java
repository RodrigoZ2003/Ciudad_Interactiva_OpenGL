package com.graphics;

import java.util.ArrayList;
import java.util.List;

/** EXTRA 5: movimiento por distancia sobre rectas y arcos, sin depender de OpenGL. */
public final class TraficoAutonomo {
    private final List<Vehiculo> vehiculos;

    public TraficoAutonomo() {
        float oeste = CiudadBase.centro(0);
        float este = CiudadBase.centro(10);
        float norte = CiudadBase.centro(0);
        float sur = CiudadBase.centro(10);
        // Los extremos son cruces de calles (filas y columnas pares).
        Ruta noroeste = new Ruta(oeste, norte, CiudadBase.centro(4), CiudadBase.centro(4));
        Ruta noreste = new Ruta(CiudadBase.centro(6), norte, este, CiudadBase.centro(4));
        Ruta surCiudad = new Ruta(oeste, CiudadBase.centro(6), este, sur);
        vehiculos = List.of(
            new Vehiculo(noroeste, 5.5f, 16, 0.12f, 0.65f, 0.95f),
            new Vehiculo(noreste, 7, 65, 1, 0.72f, 0.12f),
            new Vehiculo(surCiudad, 6, 192, 0.65f, 0.35f, 0.95f)
        );
    }

    public List<Vehiculo> vehiculos() {
        return vehiculos; // Lista inmodificable: cada auto conserva su estado independiente.
    }

    public void actualizar(float deltaTime) {
        actualizar(deltaTime, (x, z, giro) -> true);
    }

    @FunctionalInterface
    public interface PasoLibre {
        boolean permite(float x, float z, float giro);
    }

    /** Subpasos cortos: el tráfico tampoco puede atravesar al jugador entre cuadros. */
    public void actualizar(float deltaTime, PasoLibre entorno) {
        if (!Float.isFinite(deltaTime) || deltaTime < 0) {
            throw new IllegalArgumentException("El paso temporal debe ser finito y positivo");
        }
        int pasos = Math.max(1, (int) Math.ceil(deltaTime / 0.01));
        float paso = deltaTime / pasos;
        for (int i = 0; i < pasos; i++) {
            for (Vehiculo vehiculo : vehiculos) {
                if (!vehiculo.activo) continue;
                vehiculo.avanzar(paso, (x, z, giro) -> entorno.permite(x, z, giro)
                    && !ocupa(x, z, giro, vehiculo));
            }
        }
    }

    /** El parámetro excluir evita que cada vehículo choque consigo mismo. */
    public boolean ocupa(float x, float z, float giro, Vehiculo excluir) {
        return ocupa(Colisiones.auto(x, z, giro), excluir);
    }

    /** Acepta también el contorno de una chata o de su enganche. */
    boolean ocupa(Colisiones.Rectangulo contorno, Vehiculo excluir) {
        for (Vehiculo otro : vehiculos) {
            if (otro.activo && otro != excluir && Colisiones.chocan(contorno, Colisiones.auto(otro.x, otro.z, otro.angulo))) return true;
        }
        return false;
    }

    public void reiniciar() {
        for (Vehiculo vehiculo : vehiculos) {
            vehiculo.reiniciar();
        }
    }

    public int activos() {
        return (int) vehiculos.stream().filter(Vehiculo::activo).count();
    }

    /** Estado propio de un auto: progreso, posicion, orientacion y velocidad. */
    public static final class Vehiculo {
        private final Ruta ruta;
        private final float velocidad;
        private final double inicio;
        private final float rojo, verde, azul;
        private double distancia;
        private float x, z, angulo;
        private boolean activo = true;

        private Vehiculo(Ruta ruta, float velocidad, double inicio, float rojo, float verde, float azul) {
            this.ruta = ruta;
            this.velocidad = velocidad;
            this.inicio = inicio;
            this.rojo = rojo;
            this.verde = verde;
            this.azul = azul;
            reiniciar();
        }

        private void avanzar(float deltaTime, PasoLibre libre) {
            double avance = velocidad * (double) deltaTime;
            if (!permiteAvance(avance, libre)) {
                double desde = 0;
                double hasta = avance;
                for (int intento = 0; intento < 12; intento++) {
                    double medio = (desde + hasta) / 2;
                    if (permiteAvance(medio, libre)) desde = medio;
                    else hasta = medio;
                }
                avance = desde;
            }
            distancia = (distancia + avance) % ruta.longitud;
            colocar();
        }

        private boolean permiteAvance(double avance, PasoLibre libre) {
            Muestra muestra = ruta.muestrear((distancia + avance) % ruta.longitud);
            float giro = (float) Math.atan2(-muestra.dx, -muestra.dz);
            return libre.permite(muestra.x, muestra.z, giro);
        }

        private void reiniciar() {
            activo = true;
            distancia = inicio % ruta.longitud;
            colocar();
        }

        private void colocar() {
            Muestra muestra = ruta.muestrear(distancia);
            x = muestra.x;
            z = muestra.z;
            // El modelo apunta a -Z: atan2(-dx, -dz) orienta su frente segun la tangente.
            angulo = (float) Math.atan2(-muestra.dx, -muestra.dz);
        }

        public float x() { return x; }
        public boolean activo() { return activo; }
        public void destruir() { activo = false; }
        public float z() { return z; }
        public float angulo() { return angulo; }
        public float velocidad() { return velocidad; }
        public float rojo() { return rojo; }
        public float verde() { return verde; }
        public float azul() { return azul; }
        double longitudRuta() { return ruta.longitud; }
    }

    private record Muestra(float x, float z, float dx, float dz) { }

    /** Circuito rectangular redondeado: cuatro rectas y cuatro giros de 90 grados. */
    private static final class Ruta {
        private final List<Tramo> tramos = new ArrayList<>();
        private double longitud;

        Ruta(float x0, float z0, float x1, float z1) {
            double radio = CiudadBase.CELDA * 0.4; // 4 unidades: el giro cabe dentro de la interseccion.
            double[][] esquinas = {{x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}};
            for (int i = 0; i < 4; i++) {
                double[] anterior = esquinas[i];
                double[] esquina = esquinas[(i + 1) % 4];
                double[] siguiente = esquinas[(i + 2) % 4];
                double dx = Math.signum(esquina[0] - anterior[0]);
                double dz = Math.signum(esquina[1] - anterior[1]);
                double sx = Math.signum(siguiente[0] - esquina[0]);
                double sz = Math.signum(siguiente[1] - esquina[1]);
                // Recta desde la salida de una curva hasta la entrada de la siguiente.
                agregar(Tramo.recta(anterior[0] + dx * radio, anterior[1] + dz * radio,
                    esquina[0] - dx * radio, esquina[1] - dz * radio));
                // Centro del arco desplazado hacia el interior del circuito.
                double cx = esquina[0] - dx * radio + sx * radio;
                double cz = esquina[1] - dz * radio + sz * radio;
                double inicio = Math.atan2(-sz, -sx);
                double giro = Math.signum(dx * sz - dz * sx) * Math.PI / 2;
                agregar(Tramo.arco(cx, cz, radio, inicio, giro));
            }
        }

        private void agregar(Tramo tramo) {
            tramos.add(tramo);
            longitud += tramo.longitud;
        }

        private Muestra muestrear(double distancia) {
            for (Tramo tramo : tramos) {
                if (distancia <= tramo.longitud) {
                    return tramo.muestrear(distancia);
                }
                distancia -= tramo.longitud;
            }
            return tramos.get(0).muestrear(0); // Protege el cierre ante redondeo de punto flotante.
        }
    }

    private static final class Tramo {
        private final double x, z, dx, dz, radio, inicio, giro, longitud;

        private Tramo(double x, double z, double dx, double dz, double radio, double inicio, double giro, double longitud) {
            this.x = x; this.z = z; this.dx = dx; this.dz = dz;
            this.radio = radio; this.inicio = inicio; this.giro = giro; this.longitud = longitud;
        }

        static Tramo recta(double x, double z, double finX, double finZ) {
            double longitud = Math.hypot(finX - x, finZ - z);
            return new Tramo(x, z, (finX - x) / longitud, (finZ - z) / longitud, 0, 0, 0, longitud);
        }

        static Tramo arco(double cx, double cz, double radio, double inicio, double giro) {
            return new Tramo(cx, cz, 0, 0, radio, inicio, giro, Math.abs(giro) * radio);
        }

        Muestra muestrear(double distancia) {
            if (radio == 0) {
                return new Muestra((float) (x + dx * distancia), (float) (z + dz * distancia), (float) dx, (float) dz);
            }
            double signo = Math.signum(giro);
            double theta = inicio + signo * distancia / radio;
            return new Muestra((float) (x + radio * Math.cos(theta)), (float) (z + radio * Math.sin(theta)),
                (float) (-Math.sin(theta) * signo), (float) (Math.cos(theta) * signo));
        }
    }
}
