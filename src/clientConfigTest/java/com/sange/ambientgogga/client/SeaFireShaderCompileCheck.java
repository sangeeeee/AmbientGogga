package com.sange.ambientgogga.client;

import java.nio.charset.StandardCharsets;
import java.util.zip.ZipFile;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;

/** Optional actual-driver shader compilation in an invisible context, without launching Minecraft. */
public final class SeaFireShaderCompileCheck {
    public static void main(String[] args) throws Exception {
        if (!GLFW.glfwInit()) throw new IllegalStateException("GLFW initialization failed");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_COMPAT_PROFILE);
        long window = GLFW.glfwCreateWindow(32, 32, "Sea Fire shader validation", 0, 0);
        if (window == 0) throw new IllegalStateException("Hidden OpenGL context unavailable");
        try {
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            try (ZipFile zip = new ZipFile(args[0])) {
                for (boolean waves : new boolean[]{false, true}) {
                    for (String dimension : new String[]{"world0", "world-1", "world1", "world0-dh"}) {
                        boolean distantHorizons = dimension.endsWith("-dh");
                        String folder = distantHorizons ? "world0" : dimension;
                        String source = expand(zip, "shaders/" + folder + "/gbuffers_particles_translucent.vsh", 0);
                        int line = source.indexOf('\n');
                        source = source.substring(0, line + 1) + "#define IS_IRIS\n#define MC_VERSION 12101\n"
                                + (waves ? "#define LARGE_WAVE_DISPLACEMENT\n" : "") + source.substring(line + 1);
                        if (distantHorizons) source = source.replace("#define IS_IRIS", "#define DISTANT_HORIZONS\n#define IS_IRIS");
                        int shader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
                        GL20.glShaderSource(shader, source);
                        GL20.glCompileShader(shader);
                        String log = GL20.glGetShaderInfoLog(shader);
                        boolean ok = GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) != 0;
                        GL20.glDeleteShader(shader);
                        if (!ok) throw new AssertionError(dimension + " waves=" + waves + "\n" + log);
                        System.out.println("PASS GPU vertex compile: " + dimension + " waves=" + waves);
                    }
                }
            }
        } finally {
            GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
        }
    }

    static String expand(ZipFile zip, String path, int depth) throws Exception {
        if (depth > 30) throw new IllegalArgumentException("Include nesting too deep");
        var entry = zip.getEntry(path);
        if (entry == null) throw new IllegalArgumentException("Missing shader include: " + path);
        String text = new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
        StringBuilder result = new StringBuilder();
        for (String line : text.split("\\R")) {
            String trimmed = line.strip();
            if (trimmed.startsWith("#include \"")) {
                String include = trimmed.substring(trimmed.indexOf('"') + 1, trimmed.lastIndexOf('"'));
                String resolved = include.startsWith("/") ? "shaders" + include : path.substring(0, path.lastIndexOf('/') + 1) + include;
                result.append(expand(zip, resolved, depth + 1));
            } else result.append(line).append('\n');
        }
        return result.toString();
    }
}
