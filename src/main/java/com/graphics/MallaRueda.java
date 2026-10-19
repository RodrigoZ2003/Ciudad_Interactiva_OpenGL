package com.graphics;

import static org.lwjgl.opengl.GL33.*;

/** Cilindro de eje X: una sola malla reutilizada para neumáticos y llantas. */
final class MallaRueda implements AutoCloseable {
    private static final int SEGMENTOS = 20;
    private final int vao;
    private final int vbo;

    MallaRueda() {
        int anteriorVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int anteriorBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING);
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        try {
            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, crearVertices(), GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0L);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3L * Float.BYTES);
            glEnableVertexAttribArray(0);
            glEnableVertexAttribArray(1);
        } finally {
            glBindVertexArray(anteriorVao);
            glBindBuffer(GL_ARRAY_BUFFER, anteriorBuffer);
        }
    }

    static float[] crearVertices() {
        float[] datos = new float[SEGMENTOS * 12 * 6];
        int offset = 0;
        for (int i = 0; i < SEGMENTOS; i++) {
            double a = 2 * Math.PI * i / SEGMENTOS;
            double b = 2 * Math.PI * (i + 1) / SEGMENTOS;
            float ya = (float) Math.cos(a), za = (float) Math.sin(a);
            float yb = (float) Math.cos(b), zb = (float) Math.sin(b);
            // Dos triángulos laterales con normales radiales para una superficie suave.
            offset = vertice(datos, offset, -.5f, ya / 2, za / 2, 0, ya, za);
            offset = vertice(datos, offset, -.5f, yb / 2, zb / 2, 0, yb, zb);
            offset = vertice(datos, offset, .5f, yb / 2, zb / 2, 0, yb, zb);
            offset = vertice(datos, offset, -.5f, ya / 2, za / 2, 0, ya, za);
            offset = vertice(datos, offset, .5f, yb / 2, zb / 2, 0, yb, zb);
            offset = vertice(datos, offset, .5f, ya / 2, za / 2, 0, ya, za);
            // Tapas con normales hacia afuera del eje.
            offset = vertice(datos, offset, -.5f, 0, 0, -1, 0, 0);
            offset = vertice(datos, offset, -.5f, yb / 2, zb / 2, -1, 0, 0);
            offset = vertice(datos, offset, -.5f, ya / 2, za / 2, -1, 0, 0);
            offset = vertice(datos, offset, .5f, 0, 0, 1, 0, 0);
            offset = vertice(datos, offset, .5f, ya / 2, za / 2, 1, 0, 0);
            offset = vertice(datos, offset, .5f, yb / 2, zb / 2, 1, 0, 0);
        }
        return datos;
    }

    private static int vertice(float[] datos, int offset, float x, float y, float z, float nx, float ny, float nz) {
        datos[offset++] = x;
        datos[offset++] = y;
        datos[offset++] = z;
        datos[offset++] = nx;
        datos[offset++] = ny;
        datos[offset++] = nz;
        return offset;
    }

    void dibujar() {
        int anteriorVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, SEGMENTOS * 12);
        glBindVertexArray(anteriorVao); // Las demás piezas siguen usando el cubo original.
    }

    @Override
    public void close() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}
