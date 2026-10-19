package com.graphics;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.opengl.GL33.*;

/** EXTRA 5: panel y ayuda 2D, dibujados al final sin borrar la escena ni el minimapa. */
public final class InterfazHud implements AutoCloseable {
    public record Estado(float velocidadKmh, boolean noche, boolean faros, int entregas, int total,
                         float tiempo, boolean mapa, boolean ayuda, int trafico, String sector, String destino,
                         int marcha, int limiteMarchaKmh, boolean armas, String chata) { }

    private static final int CELDA_TEXTO = 48;
    private static final int TAMANO_ATLAS = CELDA_TEXTO * 16;
    private static final float TAMANO_FUENTE = 28;
    private final float[] avances = new float[256];
    private final FloatBuffer vertices = MemoryUtil.memAllocFloat(131072);
    private int programa, vao, vbo, textura;
    private final int pantallaUniform;
    private static final float[] BLANCO = {0.94f, 0.97f, 1, 1};
    private static final float[] GRIS = {0.64f, 0.73f, 0.82f, 1};
    private static final float[] CIAN = {0.3f, 0.88f, 0.94f, 1};
    private static final float[] VERDE = {0.45f, 0.95f, 0.65f, 1};
    private static final float[] AMBAR = {1, 0.79f, 0.3f, 1};

    public InterfazHud() {
        int anteriorPrograma = glGetInteger(GL_CURRENT_PROGRAM);
        int anteriorVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int anteriorBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING);
        int anteriorTextura = glGetInteger(GL_TEXTURE_BINDING_2D);
        try {
            programa = crearPrograma();
            pantallaUniform = glGetUniformLocation(programa, "uPantalla");
            glUseProgram(programa);
            glUniform1i(glGetUniformLocation(programa, "uAtlas"), 0);
            vao = glGenVertexArrays();
            vbo = glGenBuffers();
            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            int[] componentes = {2, 2, 4, 1}; // Posicion, UV, color RGBA y mascara de texto.
            int desplazamiento = 0;
            for (int i = 0; i < componentes.length; i++) {
                glEnableVertexAttribArray(i);
                glVertexAttribPointer(i, componentes[i], GL_FLOAT, false, 9 * Float.BYTES, (long) desplazamiento * Float.BYTES);
                desplazamiento += componentes[i];
            }
            crearAtlas(); // Java genera la textura de letras; no se descargan fuentes ni imagenes.
        } catch (RuntimeException error) {
            close();
            throw error;
        } finally {
            glUseProgram(anteriorPrograma);
            glBindVertexArray(anteriorVao);
            glBindBuffer(GL_ARRAY_BUFFER, anteriorBuffer);
            glBindTexture(GL_TEXTURE_2D, anteriorTextura);
        }
    }

    public void dibujar(int ancho, int alto, Estado estado) {
        if (ancho < 1 || alto < 1) return;
        vertices.clear();
        float escala = Math.min(1, Math.min(ancho / 560f, alto / 420f));
        float margen = 14 * escala;
        float panelAncho = 306 * escala;
        float panelAlto = 284 * escala;
        // En ventanas estrechas el panel baja, dejando libre el minimapa de arriba.
        float y = ancho < 740 ? alto - panelAlto - margen : margen;
        panel(margen, y, panelAncho, panelAlto);
        rectangulo(margen, y, 3 * escala, panelAlto, 0.3f, 0.88f, 0.94f, 1);
        texto(margen + 16 * escala, y + 11 * escala, "CIUDAD INTERACTIVA", 12 * escala, CIAN);
        texto(margen + 218 * escala, y + 11 * escala, "TRÁFICO " + estado.trafico, 10 * escala, GRIS);
        texto(margen + 16 * escala, y + 31 * escala, String.format(java.util.Locale.ROOT, "%02d", Math.round(estado.velocidadKmh)), 38 * escala, BLANCO);
        texto(margen + 106 * escala, y + 52 * escala, "km/h", 15 * escala, GRIS);
        texto(margen + 169 * escala, y + 36 * escala, "MARCHA " + estado.marcha, 13 * escala, AMBAR);
        texto(margen + 169 * escala, y + 57 * escala, "MÁX " + estado.limiteMarchaKmh + " km/h", 11 * escala, GRIS);
        etiqueta(margen + 16 * escala, y + 85 * escala, 128 * escala, 25 * escala,
            estado.noche ? "N  NOCHE" : "N  DÍA", escala, CIAN);
        etiqueta(margen + 152 * escala, y + 85 * escala, 138 * escala, 25 * escala,
            estado.faros ? "F  FAROS ON" : "F  FAROS OFF", escala, estado.faros ? AMBAR : GRIS);
        boolean victoria = estado.entregas >= estado.total;
        texto(margen + 16 * escala, y + 121 * escala, "ENTREGAS  " + estado.entregas + "/" + estado.total, 14 * escala, victoria ? VERDE : BLANCO);
        texto(margen + 224 * escala, y + 121 * escala, (int) estado.tiempo + " s", 14 * escala, GRIS);
        float anchoBarra = (274f - (estado.total - 1) * 6) / estado.total;
        for (int i = 0; i < estado.total; i++) {
            boolean completada = i < estado.entregas;
            rectangulo(margen + (16 + i * (anchoBarra + 6)) * escala, y + 147 * escala,
                anchoBarra * escala, 5 * escala, completada ? 0.45f : 0.18f, completada ? 0.95f : 0.26f, completada ? 0.65f : 0.34f, 1);
        }
        texto(margen + 16 * escala, y + 163 * escala, victoria ? "GANASTE · R PARA REINICIAR" : "DESTINO: " + estado.destino, 12 * escala, victoria ? VERDE : AMBAR);
        texto(margen + 16 * escala, y + 183 * escala, "SECTOR: " + estado.sector, 12 * escala, GRIS);
        texto(margen + 16 * escala, y + 207 * escala, estado.armas ? "K  ARMAS ACTIVAS · CLIC IZQ / DER" : "K  DESPLEGAR ARMAS", 11 * escala, estado.armas ? AMBAR : GRIS);
        texto(margen + 16 * escala, y + 230 * escala, estado.chata, 11 * escala, AMBAR);
        rectangulo(margen + 16 * escala, y + 253 * escala, 274 * escala, 1, 0.2f, 0.3f, 0.4f, 1);
        texto(margen + 16 * escala, y + 261 * escala, estado.ayuda ? "H  CERRAR AYUDA" : "H  VER CONTROLES", 11 * escala, CIAN);
        texto(margen + 202 * escala, y + 261 * escala, estado.mapa ? "M  MAPA ON" : "M  MAPA OFF", 11 * escala, GRIS);
        if (estado.ayuda) dibujarAyuda(ancho, alto);
        presentar(ancho, alto);
    }

    private void dibujarAyuda(int ancho, int alto) {
        float escala = Math.min(1, Math.min((ancho - 24) / 480f, (alto - 24) / 450f));
        if (escala <= 0) return;
        float x = (ancho - 480 * escala) / 2;
        float y = (alto - 450 * escala) / 2;
        panel(x, y, 480 * escala, 450 * escala);
        texto(x + 24 * escala, y + 17 * escala, "CONTROLES", 23 * escala, CIAN);
        texto(x + 357 * escala, y + 25 * escala, "H  CERRAR", 12 * escala, GRIS);
        String[][] filas = {
            {"W / S", "Acelerar / retroceder"}, {"A / D", "Girar mientras avanzas"},
            {"ESPACIO", "Frenar"}, {"C", "Cambiar cámara"},
            {"B", "Tocar bocina"},
            {"T", "Poner / quitar chata"},
            {"1 - 5", "Seleccionar marcha"},
            {"K", "Desplegar / ocultar armas"},
            {"CLIC IZQ.", "Metralletas (mantener: ráfaga)"},
            {"CLIC DER.", "Misil (recarga: 1 segundo)"},
            {"N / F", "Día-noche / faros"}, {"M", "Mostrar / ocultar minimapa"},
            {"R", "Reiniciar recorrido"}, {"ESC", "Salir"}
        };
        for (int i = 0; i < filas.length; i++) {
            texto(x + 24 * escala, y + (61 + i * 25) * escala, filas[i][0], 14 * escala, AMBAR);
            texto(x + 146 * escala, y + (61 + i * 25) * escala, filas[i][1], 14 * escala, BLANCO);
        }
        texto(x + 24 * escala, y + 424 * escala, "La ciudad sigue en movimiento mientras lees la ayuda.", 11 * escala, GRIS);
    }

    private void panel(float x, float y, float ancho, float alto) {
        rectangulo(x, y, ancho, alto, 0.04f, 0.085f, 0.14f, 0.95f);
        rectangulo(x, y, ancho, 1, 0.23f, 0.38f, 0.48f, 1);
        rectangulo(x, y + alto - 1, ancho, 1, 0.23f, 0.38f, 0.48f, 1);
    }

    private void etiqueta(float x, float y, float ancho, float alto, String titulo, float escala, float[] color) {
        rectangulo(x, y, ancho, alto, 0.12f, 0.2f, 0.28f, 1);
        texto(x + 10 * escala, y + 5 * escala, titulo, 12 * escala, color);
    }

    private void texto(float x, float y, String texto, float tamano, float[] color) {
        float escala = tamano / TAMANO_FUENTE;
        for (char letra : texto.toCharArray()) {
            int codigo = letra < 256 ? letra : '?';
            float u = (codigo % 16) / 16f;
            float v = (codigo / 16) / 16f;
            quad(x, y, CELDA_TEXTO * escala, CELDA_TEXTO * escala, u, v, u + 1 / 16f, v + 1 / 16f, color, 1);
            x += avances[codigo] * escala;
        }
    }

    private void rectangulo(float x, float y, float ancho, float alto, float r, float g, float b, float a) {
        quad(x, y, ancho, alto, 0, 0, 0, 0, new float[]{r, g, b, a}, 0);
    }

    private void quad(float x, float y, float ancho, float alto, float u0, float v0, float u1, float v1, float[] color, float mascara) {
        vertice(x, y, u0, v0, color, mascara);
        vertice(x + ancho, y, u1, v0, color, mascara);
        vertice(x + ancho, y + alto, u1, v1, color, mascara);
        vertice(x, y, u0, v0, color, mascara);
        vertice(x + ancho, y + alto, u1, v1, color, mascara);
        vertice(x, y + alto, u0, v1, color, mascara);
    }

    private void vertice(float x, float y, float u, float v, float[] color, float mascara) {
        vertices.put(x).put(y).put(u).put(v).put(color).put(mascara);
    }

    /** Un solo lote de triangulos; guarda/restaura los estados que modifica. */
    private void presentar(int ancho, int alto) {
        int anteriorPrograma = glGetInteger(GL_CURRENT_PROGRAM);
        int anteriorVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int anteriorBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING);
        int anteriorUnidad = glGetInteger(GL_ACTIVE_TEXTURE);
        glActiveTexture(GL_TEXTURE0);
        int anteriorTextura = glGetInteger(GL_TEXTURE_BINDING_2D);
        boolean profundidad = glIsEnabled(GL_DEPTH_TEST);
        boolean mezcla = glIsEnabled(GL_BLEND);
        int srcRgb = glGetInteger(GL_BLEND_SRC_RGB), dstRgb = glGetInteger(GL_BLEND_DST_RGB);
        int srcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA), dstAlpha = glGetInteger(GL_BLEND_DST_ALPHA);
        int ecuacionRgb = glGetInteger(GL_BLEND_EQUATION_RGB), ecuacionAlpha = glGetInteger(GL_BLEND_EQUATION_ALPHA);
        try {
            glDisable(GL_DEPTH_TEST);
            glEnable(GL_BLEND);
            glBlendEquation(GL_FUNC_ADD);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glUseProgram(programa);
            glUniform2f(pantallaUniform, ancho, alto);
            glBindTexture(GL_TEXTURE_2D, textura);
            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            vertices.flip();
            glBufferData(GL_ARRAY_BUFFER, vertices, GL_STREAM_DRAW);
            glDrawArrays(GL_TRIANGLES, 0, vertices.remaining() / 9);
        } finally {
            if (profundidad) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST);
            if (mezcla) glEnable(GL_BLEND); else glDisable(GL_BLEND);
            glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            glBlendEquationSeparate(ecuacionRgb, ecuacionAlpha);
            glUseProgram(anteriorPrograma);
            glBindVertexArray(anteriorVao);
            glBindBuffer(GL_ARRAY_BUFFER, anteriorBuffer);
            glBindTexture(GL_TEXTURE_2D, anteriorTextura);
            glActiveTexture(anteriorUnidad);
        }
    }

    private void crearAtlas() {
        BufferedImage imagen = new BufferedImage(TAMANO_ATLAS, TAMANO_ATLAS, BufferedImage.TYPE_INT_ARGB);
        Graphics2D dibujo = imagen.createGraphics();
        dibujo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) TAMANO_FUENTE));
        dibujo.setColor(Color.WHITE);
        dibujo.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontMetrics medidas = dibujo.getFontMetrics();
        for (int i = 0; i < 256; i++) {
            avances[i] = medidas.charWidth((char) i);
            if (i >= 32 && i != 127) {
                dibujo.drawString(String.valueOf((char) i), (i % 16) * CELDA_TEXTO + 2,
                    (i / 16) * CELDA_TEXTO + medidas.getAscent() + 2);
            }
        }
        dibujo.dispose();
        ByteBuffer pixeles = MemoryUtil.memAlloc(TAMANO_ATLAS * TAMANO_ATLAS);
        int anteriorAlineacion = glGetInteger(GL_UNPACK_ALIGNMENT);
        try {
            for (int y = 0; y < TAMANO_ATLAS; y++) {
                for (int x = 0; x < TAMANO_ATLAS; x++) pixeles.put((byte) (imagen.getRGB(x, y) >>> 24));
            }
            pixeles.flip();
            textura = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textura);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, TAMANO_ATLAS, TAMANO_ATLAS, 0, GL_RED, GL_UNSIGNED_BYTE, pixeles);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        } finally {
            glPixelStorei(GL_UNPACK_ALIGNMENT, anteriorAlineacion);
            MemoryUtil.memFree(pixeles);
        }
    }

    private static int crearPrograma() {
        String vertex = """
            #version 330 core
            layout(location=0) in vec2 aPos;
            layout(location=1) in vec2 aUv;
            layout(location=2) in vec4 aColor;
            layout(location=3) in float aMascara;
            uniform vec2 uPantalla;
            out vec2 uv; out vec4 tinte; out float mascara;
            void main() {
                gl_Position = vec4(aPos.x / uPantalla.x * 2.0 - 1.0, 1.0 - aPos.y / uPantalla.y * 2.0, 0, 1);
                uv=aUv; tinte=aColor; mascara=aMascara;
            }
            """;
        String fragment = """
            #version 330 core
            in vec2 uv; in vec4 tinte; in float mascara;
            uniform sampler2D uAtlas;
            out vec4 color;
            void main() {
                float cobertura = mascara > 0.5 ? texture(uAtlas, uv).r : 1.0;
                color = vec4(tinte.rgb, tinte.a * cobertura);
            }
            """;
        int vs = compilar(GL_VERTEX_SHADER, vertex);
        int fs;
        try { fs = compilar(GL_FRAGMENT_SHADER, fragment); }
        catch (RuntimeException error) { glDeleteShader(vs); throw error; }
        int programa = glCreateProgram();
        try {
            glAttachShader(programa, vs);
            glAttachShader(programa, fs);
            glLinkProgram(programa);
            if (glGetProgrami(programa, GL_LINK_STATUS) == GL_FALSE) throw new IllegalStateException(glGetProgramInfoLog(programa));
            return programa;
        } catch (RuntimeException error) {
            glDeleteProgram(programa);
            throw error;
        } finally {
            glDeleteShader(vs);
            glDeleteShader(fs);
        }
    }

    private static int compilar(int tipo, String fuente) {
        int shader = glCreateShader(tipo);
        glShaderSource(shader, fuente);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String error = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException("Shader de interfaz: " + error);
        }
        return shader;
    }

    @Override
    public void close() {
        if (textura != 0) glDeleteTextures(textura);
        if (vbo != 0) glDeleteBuffers(vbo);
        if (vao != 0) glDeleteVertexArrays(vao);
        if (programa != 0) glDeleteProgram(programa);
        MemoryUtil.memFree(vertices);
        textura = vbo = vao = programa = 0;
    }
}
