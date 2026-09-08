package com.sange.ambientgogga.client;

import com.sange.ambientgogga.client.compat.SeaFireVertexProbe;
import com.sange.ambientgogga.client.compat.SeaFireWaveField;
import com.sange.ambientgogga.client.compat.SeaFireProbeStateCheck;
import java.util.Arrays;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

/** Driver-level feedback, matrix, interpolation and rejection checks in a hidden window. */
public final class SeaFireVegetationProbeTest {
    private static final String VERTEX = """
            #version 430 core
            in vec3 iris_Position;
            in vec2 mc_Entity;
            uniform mat4 iris_ModelViewMat;
            uniform mat4 iris_ProjMat;
            uniform float frameTimeCounter;
            void main() {
                vec3 p = iris_Position;
                if (mc_Entity.x == 42.0) p.y += sin(frameTimeCounter + p.x * 0.1) * 0.2;
                gl_Position = iris_ProjMat * iris_ModelViewMat * vec4(p, 1.0);
            }
            """;

    public static void main(String[] args) throws Exception {
        verifyGrid();
        if (!GLFW.glfwInit()) throw new AssertionError("GLFW unavailable");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_COMPAT_PROFILE);
        long window = GLFW.glfwCreateWindow(32, 32, "Sea Fire vegetation probe validation", 0, 0);
        if (window == 0) throw new AssertionError("Hidden OpenGL context unavailable");
        try {
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            com.mojang.blaze3d.systems.RenderSystem.initRenderThread();
            int source = program(VERTEX);
            var projection = new Matrix4f().perspective((float) Math.toRadians(70), 1.6F, 0.05F, 256);
            var view = new Matrix4f().rotateX(0.3F).rotateY(1.1F);
            var inverse = new Matrix4f(projection).mul(view).invert();
            GL20.glUseProgram(source);
            GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(source, "iris_ModelViewMat"), false, view.get(new float[16]));
            GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(source, "iris_ProjMat"), false, projection.get(new float[16]));
            GL20.glUniform1f(GL20.glGetUniformLocation(source, "frameTimeCounter"), 1);
            float[] positions = new float[SeaFireWaveField.SIDE * SeaFireWaveField.SIDE * 3];
            for (int i = 0; i < positions.length / 3; i++) {
                positions[i * 3] = (i % 17 - 8) * 4;
                positions[i * 3 + 1] = -1;
                positions[i * 3 + 2] = (i / 17 - 8) * 4 - 0.4F;
            }
            long total = 0;
            try (var probe = new SeaFireVertexProbe(source, positions.length / 3)) {
                for (int frame = 0; frame < 50; frame++) {
                    float time = 1 + frame * 0.13F;
                    GL20.glUseProgram(source);
                    GL20.glUniform1f(GL20.glGetUniformLocation(source, "frameTimeCounter"), time);
                    long start = System.nanoTime();
                    SeaFireProbeStateCheck.run(() -> probe.submit(positions, 42, inverse));
                    total += System.nanoTime() - start;
                    // A test-only finish makes assertions deterministic. Production only polls with timeout zero.
                    GL11.glFinish();
                    float[] result = probe.poll(0.5F, 0.12F);
                    require(result != null, "Completed feedback was not readable");
                    for (int i = 0; i < result.length; i++) {
                        float expected = (float) Math.abs(Math.sin(time + positions[i * 3] * 0.1) * 0.1);
                        near(result[i], expected, 0.002F, "Perspective reconstruction or material tagging");
                    }
                    require(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL probe error");
                }
            }
            require(GL20.glGetProgrami(source, GL20.GL_LINK_STATUS) != 0, "Source program was damaged");
            GL20.glDeleteProgram(source);
            int unsupported = program(VERTEX.replace("mc_Entity", "unknown_material"));
            boolean rejected = false;
            try (var unused = new SeaFireVertexProbe(unsupported, 1)) { }
            catch (IllegalStateException expected) { rejected = true; }
            require(rejected, "Unknown vertex layout must fall back");
            GL20.glDeleteProgram(unsupported);
            System.out.printf("PASS GPU vegetation-tag feedback, projection recovery, source isolation, fallback, GL state restoration and grid interpolation; mean guarded submit %.3f ms (synthetic shader, 578 vertices; excludes Iris activation).%n", total / 50e6);
            if (args.length != 0) verifyPackVertex(args[0]);
        } finally {
            GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
        }
    }

    private static void verifyPackVertex(String path) throws Exception {
        // This checks the pack's actual equations with an explicit vertex-input shim, not an Iris runtime launch.
        String source;
        try (var zip = new java.util.zip.ZipFile(path)) {
            source = SeaFireShaderCompileCheck.expand(zip, "shaders/world0/gbuffers_terrain_cutout.vsh", 0);
        }
        String[] legacy = {"gl_ModelViewProjectionMatrix", "gl_ModelViewMatrixInverse", "gl_ProjectionMatrixInverse",
                "gl_ModelViewMatrix", "gl_ProjectionMatrix", "gl_NormalMatrix", "gl_MultiTexCoord0", "gl_MultiTexCoord1",
                "gl_MultiTexCoord2", "gl_TextureMatrix[0]", "gl_TextureMatrix[1]", "gl_TextureMatrix[2]", "gl_Vertex", "gl_Normal", "gl_Color"};
        String[] modern = {"(iris_ProjMat * iris_ModelViewMat)", "gbufferModelViewInverse", "inverse(iris_ProjMat)",
                "iris_ModelViewMat", "iris_ProjMat", "mat3(iris_ModelViewMat)", "vec4(iris_UV0,0,1)", "vec4(iris_UV2,0,1)",
                "vec4(iris_UV2,0,1)", "mat4(1)", "mat4(0.00390625)", "mat4(0.00390625)", "vec4(iris_Position,1)", "iris_Normal", "iris_Color"};
        for (int i = 0; i < legacy.length; i++) source = source.replace(legacy[i], modern[i]);
        int line = source.indexOf('\n');
        source = source.substring(0, line + 1) + """
                #define IS_IRIS
                #define MC_VERSION 12101
                in vec3 iris_Position;
                in vec3 iris_Normal;
                in vec4 iris_Color;
                in vec2 iris_UV0;
                in vec2 iris_UV2;
                uniform mat4 iris_ModelViewMat;
                uniform mat4 iris_ProjMat;
                """ + source.substring(line + 1);
        int original = program(source);
        var projection = new Matrix4f().perspective(1.2F, 1.6F, 0.05F, 256);
        GL20.glUseProgram(original);
        for (String name : new String[]{"iris_ModelViewMat", "gbufferModelView", "gbufferModelViewInverse"})
            GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(original, name), false, new Matrix4f().get(new float[16]));
        GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(original, "iris_ProjMat"), false, projection.get(new float[16]));
        GL20.glUniform1f(GL20.glGetUniformLocation(original, "frameTimeCounter"), 10);
        GL20.glUniform1f(GL20.glGetUniformLocation(original, "far"), 256);
        float[] pos = {0, 0, -10, 1, 0, -10, 2, 0, -10};
        try (var probe = new SeaFireVertexProbe(original, 3)) {
            probe.submit(pos, 54, new Matrix4f(projection).invert());
            GL11.glFinish();
            float[] height = probe.poll(0.5F, 0.12F);
            require(height != null && Arrays.stream(new double[]{height[0], height[1], height[2]}).max().orElse(0) > 0.001,
                    "Actual pack vegetation must produce a nonzero lift");
            require(GL11.glGetError() == GL11.GL_NO_ERROR, "Actual pack probe GL error");
            System.out.println("PASS actual Eclipse terrain vertex equations with input shim: vegetation lift " + Arrays.toString(height));
        } finally { GL20.glDeleteProgram(original); }
    }

    private static void verifyGrid() {
        var grid = new SeaFireWaveField();
        float[] heights = new float[17 * 17];
        Arrays.fill(heights, 0.1F);
        grid.update(-32, -32, 4, heights, 0);
        near(grid.sample(0, 0), 0, 0, "Initial transition");
        grid.frame(50_000_000L);
        near(grid.sample(0, 0), 0.05F, 0.00001F, "Temporal interpolation");
        grid.frame(100_000_000L);
        grid.update(-28, -32, 4, heights, 100_000_000L);
        near(grid.sample(0, 0), 0.1F, 0.00001F, "Camera grid shift must stay continuous");
        near(grid.sample(500, 0), 0, 0, "Outside grid");
        grid.frame(2_000_000_000L);
        near(grid.sample(0, 0), 0, 0, "Stale grid must expire");
    }

    private static int program(String source) {
        int vertex = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertex, source); GL20.glCompileShader(vertex);
        require(GL20.glGetShaderi(vertex, GL20.GL_COMPILE_STATUS) != 0, GL20.glGetShaderInfoLog(vertex));
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertex); GL20.glLinkProgram(program);
        GL20.glDeleteShader(vertex);
        require(GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) != 0, GL20.glGetProgramInfoLog(program));
        return program;
    }
    private static void near(float actual, float expected, float tolerance, String message) {
        require(Math.abs(actual - expected) <= tolerance, message + ": " + actual + " != " + expected);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
