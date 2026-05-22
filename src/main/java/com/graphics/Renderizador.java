package com.graphics;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 *  Renderizador — Todo lo de OpenGL en un solo lugar
 *
 *  Responsabilidades:
 *    - Crear la ventana GLFW
 *    - Compilar los shaders GLSL
 *    - Subir el quad base a la GPU (VAO/VBO)
 *    - Ofrecer métodos para dibujar primitivas:
 *        dibujarRect()     → rectángulos
 *        dibujarCirculo()  → círculos aproximados
 *        dibujarTriangulo() → triángulos libres
 *
 *  Las otras clases (Pajaro, Tuberia, Fondo) llaman a estos métodos
 *  sin saber nada de OpenGL. Eso es encapsulamiento.
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class Renderizador {

    // ══════════════════════════════════════════════════════════════
    //  RECURSOS OPENGL
    // ══════════════════════════════════════════════════════════════

    private long ventana;   // handle de la ventana GLFW
    private int  programa;  // ID del programa GLSL (vertex + fragment linked)
    private int  vao;       // Vertex Array Object — recuerda la config de atributos
    private int  vbo;       // Vertex Buffer Object — guarda los vértices del quad base en GPU

    // "Direcciones" de los uniforms dentro del shader
    // (se consultan una sola vez al compilar y se reusan cada frame)
    private int uOffsetLocation; // uniform vec2 uOffset — desplaza el quad
    private int uScaleLocation;  // uniform vec2 uScale  — escala el quad
    private int uColorLocation;  // uniform vec3 uColor  — color RGB

    // ══════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════
    /**
     * Crea todo lo necesario para dibujar con OpenGL.
     *
     * @param ancho  ancho de la ventana en píxeles
     * @param alto   alto de la ventana en píxeles
     * @param titulo texto de la barra de título
     */
    public Renderizador(int ancho, int alto, String titulo) {
        crearVentana(ancho, alto, titulo);
        crearShaders();
        crearQuadBase();
    }

    // ──────────────────────────────────────────────────────────────
    //  CREACIÓN DE VENTANA
    // ──────────────────────────────────────────────────────────────
    /**
     * Inicializa GLFW y crea la ventana con un contexto OpenGL 3.3 Core.
     *
     * ¿Qué es Core Profile?
     *   Es la versión moderna de OpenGL. No tiene las funciones viejas
     *   (glBegin/glEnd, etc.). Obliga a usar shaders, lo cual es correcto.
     */
    private void crearVentana(int ancho, int alto, String titulo) {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("No se pudo iniciar GLFW");
        }

        // Configurar el tipo de contexto antes de crear la ventana
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,   GLFW.GLFW_FALSE); // ocultar hasta estar listo
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE,  GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);   // OpenGL 3.x
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);   // OpenGL x.3
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE); // necesario en macOS

        ventana = GLFW.glfwCreateWindow(ancho, alto, titulo, 0, 0);
        if (ventana == 0) {
            throw new RuntimeException("No se pudo crear la ventana GLFW");
        }

        GLFW.glfwMakeContextCurrent(ventana); // vincular contexto OpenGL a esta ventana
        GLFW.glfwSwapInterval(1);             // VSync: ~60 FPS
        GLFW.glfwShowWindow(ventana);
        GL.createCapabilities();              // habilita las funciones OpenGL en LWJGL
    }

    // ──────────────────────────────────────────────────────────────
    //  SHADERS GLSL
    // ──────────────────────────────────────────────────────────────
    /**
     * Compila y enlaza el par de shaders que usan todos los objetos del juego.
     *
     * ─────────────────────────────────────────────────────────────
     * VERTEX SHADER (se ejecuta una vez POR CADA VÉRTICE):
     *   Recibe aPos (coordenada local del vértice, entre -0.5 y +0.5).
     *   Aplica: posición_final = aPos * uScale + uOffset
     *   Así un solo quad genérico puede representar cualquier rectángulo:
     *     - uScale lo estira/achica
     *     - uOffset lo mueve a la posición correcta
     *
     * FRAGMENT SHADER (se ejecuta una vez POR CADA PÍXEL):
     *   Pinta el píxel con el color uniforme uColor.
     *   Todos los píxeles de un draw call tienen el mismo color (flat shading).
     * ─────────────────────────────────────────────────────────────
     */
    private void crearShaders() {
        // ── Vertex Shader ─────────────────────────────────────────
        String vertexSrc = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            uniform vec2 uOffset;
            uniform vec2 uScale;
            void main() {
                vec2 finalPos = aPos.xy * uScale + uOffset;
                gl_Position = vec4(finalPos, aPos.z, 1.0);
            }
            """;

        // ── Fragment Shader ───────────────────────────────────────
        String fragmentSrc = """
            #version 330 core
            uniform vec3 uColor;
            out vec4 fragColor;
            void main() {
                fragColor = vec4(uColor, 1.0);
            }
            """;

        // Compilar vertex shader
        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexSrc);
        GL20.glCompileShader(vertexShader);
        comprobarCompilacion(vertexShader, "Vertex");

        // Compilar fragment shader
        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSrc);
        GL20.glCompileShader(fragmentShader);
        comprobarCompilacion(fragmentShader, "Fragment");

        // Enlazar ambos en un programa
        programa = GL20.glCreateProgram();
        GL20.glAttachShader(programa, vertexShader);
        GL20.glAttachShader(programa, fragmentShader);
        GL20.glLinkProgram(programa);

        if (GL20.glGetProgrami(programa, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Error al enlazar shaders: " + GL20.glGetProgramInfoLog(programa));
        }

        // Obtener las "direcciones" de los uniforms (solo se hace una vez)
        uOffsetLocation = GL20.glGetUniformLocation(programa, "uOffset");
        uScaleLocation  = GL20.glGetUniformLocation(programa, "uScale");
        uColorLocation  = GL20.glGetUniformLocation(programa, "uColor");

        if (uOffsetLocation == -1 || uScaleLocation == -1 || uColorLocation == -1) {
            throw new RuntimeException("No se encontraron los uniforms en el shader");
        }

        // Los shaders individuales ya no se necesitan (el programa los contiene)
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
    }

    /** Verifica que un shader compiló sin errores. */
    private void comprobarCompilacion(int shader, String tipo) {
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(tipo + " shader no compiló: " + GL20.glGetShaderInfoLog(shader));
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  QUAD BASE EN GPU
    // ──────────────────────────────────────────────────────────────
    /**
     * Sube a la GPU un rectángulo unitario centrado en el origen.
     *
     * ¿Por qué un quad "base"?
     *   En lugar de subir geometría diferente para cada objeto del juego,
     *   subimos este quad UNA sola vez y lo reposicionamos con uniforms.
     *   Es eficiente: menos datos viajando CPU→GPU por frame.
     *
     * Coordenadas: x e y van de -0.5 a +0.5 (centrado en 0,0).
     * Se dibuja como 2 triángulos (6 vértices) que forman un cuadrado.
     *
     * VAO = "receta" que dice cómo leer el VBO
     * VBO = los datos de vértices guardados en la GPU
     */
    private void crearQuadBase() {
        float[] vertices = {
            // Triángulo 1 (inferior derecho)
            -0.5f, -0.5f, 0.0f,
             0.5f, -0.5f, 0.0f,
             0.5f,  0.5f, 0.0f,
            // Triángulo 2 (superior izquierdo)
            -0.5f, -0.5f, 0.0f,
             0.5f,  0.5f, 0.0f,
            -0.5f,  0.5f, 0.0f
        };

        // Crear y activar el VAO (guarda la configuración de atributos)
        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        // Crear el VBO y subir los vértices a la GPU
        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip(); // flip() = dejar el buffer listo para leer
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        // GL_STATIC_DRAW = los datos no van a cambiar

        // Decirle al shader cómo leer el VBO:
        // atributo 0 (aPos) = 3 floats, sin normalizar, paso=3 floats, inicio=0
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        // Desvincular para no modificar accidentalmente
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    // ══════════════════════════════════════════════════════════════
    //  PRIMITIVAS PÚBLICAS DE DIBUJO
    //  (estas son las únicas funciones que usan las otras clases)
    // ══════════════════════════════════════════════════════════════

    /**
     * Prepara el contexto OpenGL antes de dibujar el frame.
     * Llama a esto al inicio de cada render().
     *
     * @param r, g, b  color de fondo (cielo)
     */
    public void limpiarPantalla(float r, float g, float b) {
        GL11.glClearColor(r, g, b, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL20.glUseProgram(programa);   // activar nuestro programa GLSL
        GL30.glBindVertexArray(vao);   // activar el quad base
    }

    // ──────────────────────────────────────────────────────────────
    /**
     * Dibuja un rectángulo usando el quad base.
     *
     * Cómo funciona:
     *   El quad base es un cuadrado de lado 1 centrado en el origen.
     *   Con uScale lo estiramos al ancho/alto deseado.
     *   Con uOffset lo movemos al (x, y) deseado.
     *   El shader hace: posición_final = aPos * uScale + uOffset
     *
     * Todos los valores están en NDC (Normalized Device Coordinates):
     *   -1.0 = borde izquierdo/inferior, +1.0 = borde derecho/superior.
     *
     * @param x, y    centro del rectángulo en NDC
     * @param ancho   ancho en NDC  (2.0 = pantalla completa)
     * @param alto    alto en NDC   (2.0 = pantalla completa)
     * @param r, g, b color RGB (0.0 a 1.0)
     */
    public void dibujarRect(float x, float y, float ancho, float alto,
                             float r, float g, float b) {
        GL20.glUniform2f(uOffsetLocation, x, y);
        GL20.glUniform2f(uScaleLocation, ancho, alto);
        GL20.glUniform3f(uColorLocation, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    // ──────────────────────────────────────────────────────────────
    /**
     * Dibuja un círculo aproximado usando GL_TRIANGLE_FAN.
     *
     * ¿Qué es TRIANGLE_FAN?
     *   El primer vértice es el "abanico" (centro).
     *   Cada par de vértices siguientes forma un triángulo con el centro.
     *   Con 16 segmentos, se ve como un círculo suave.
     *
     * Nota: como el círculo tiene coordenadas propias (no usa el quad base),
     * ponemos uOffset=0 y uScale=1 para que el shader no lo mueva.
     *
     * @param cx, cy  centro en NDC
     * @param radio   radio en NDC
     * @param r, g, b color RGB
     */
    public void dibujarCirculo(float cx, float cy, float radio,
                                float r, float g, float b) {
        int segmentos = 16;

        // Centro + un vértice por segmento + cierre del círculo
        float[] v = new float[(segmentos + 2) * 3];

        // Primer punto: centro del abanico
        v[0] = cx;
        v[1] = cy;
        v[2] = 0.0f;

        // Calcular los vértices del borde usando trigonometría
        for (int i = 0; i <= segmentos; i++) {
            double angulo = 2.0 * Math.PI * i / segmentos;
            v[(i + 1) * 3    ] = cx + radio * (float) Math.cos(angulo);
            v[(i + 1) * 3 + 1] = cy + radio * (float) Math.sin(angulo);
            v[(i + 1) * 3 + 2] = 0.0f;
        }

        // Subir a VBO temporal y dibujar
        int tempVao = GL30.glGenVertexArrays();
        int tempVbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(tempVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tempVbo);

        FloatBuffer buf = BufferUtils.createFloatBuffer(v.length);
        buf.put(v).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STREAM_DRAW);
        // GL_STREAM_DRAW = datos que cambian cada frame (más eficiente para temporales)

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        // Shader no debe mover nada (el círculo ya tiene sus coordenadas finales)
        GL20.glUniform2f(uOffsetLocation, 0.0f, 0.0f);
        GL20.glUniform2f(uScaleLocation,  1.0f, 1.0f);
        GL20.glUniform3f(uColorLocation,  r, g, b);

        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, segmentos + 2);

        // Limpiar VBO/VAO temporales para no acumular memoria en GPU
        GL15.glDeleteBuffers(tempVbo);
        GL30.glDeleteVertexArrays(tempVao);

        // Restaurar el VAO del quad base para que dibujarRect() siga funcionando
        GL30.glBindVertexArray(vao);
    }

    // ──────────────────────────────────────────────────────────────
    /**
     * Dibuja un triángulo con 3 vértices explícitos en NDC.
     * Útil para el pico del pájaro, la cola y otras formas irregulares.
     *
     * Misma técnica que dibujarCirculo: VBO temporal por llamada.
     *
     * @param x1,y1  vértice 1 en NDC
     * @param x2,y2  vértice 2 en NDC
     * @param x3,y3  vértice 3 en NDC
     * @param r,g,b  color RGB
     */
    public void dibujarTriangulo(float x1, float y1,
                                  float x2, float y2,
                                  float x3, float y3,
                                  float r,  float g,  float b) {
        float[] v = {
            x1, y1, 0.0f,
            x2, y2, 0.0f,
            x3, y3, 0.0f
        };

        int tempVao = GL30.glGenVertexArrays();
        int tempVbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(tempVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tempVbo);

        FloatBuffer buf = BufferUtils.createFloatBuffer(v.length);
        buf.put(v).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STREAM_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL20.glUniform2f(uOffsetLocation, 0.0f, 0.0f);
        GL20.glUniform2f(uScaleLocation,  1.0f, 1.0f);
        GL20.glUniform3f(uColorLocation,  r, g, b);

        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);

        GL15.glDeleteBuffers(tempVbo);
        GL30.glDeleteVertexArrays(tempVao);

        // Restaurar quad base
        GL30.glBindVertexArray(vao);
    }

    // ══════════════════════════════════════════════════════════════
    //  GETTERS
    // ══════════════════════════════════════════════════════════════
    /** Devuelve el handle de la ventana GLFW (necesario para input y swap). */
    public long getVentana() { return ventana; }

    // ══════════════════════════════════════════════════════════════
    //  LIMPIEZA
    // ══════════════════════════════════════════════════════════════
    /**
     * Libera todos los recursos de GPU y cierra GLFW.
     * Siempre llamar esto al terminar el juego.
     */
    public void limpiar() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        GL20.glDeleteProgram(programa);
        GLFW.glfwDestroyWindow(ventana);
        GLFW.glfwTerminate();
    }
}
