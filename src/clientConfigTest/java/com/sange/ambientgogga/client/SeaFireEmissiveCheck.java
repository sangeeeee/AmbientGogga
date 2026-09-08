package com.sange.ambientgogga.client;

import java.util.zip.ZipFile;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

/** Renders the actual pack's emissive shader into a hidden floating-point target and checks fading. */
public final class SeaFireEmissiveCheck {
    public static void main(String[] args) throws Exception {
        if (!GLFW.glfwInit()) throw new AssertionError("GLFW unavailable");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_COMPAT_PROFILE);
        long window = GLFW.glfwCreateWindow(16, 16, "Sea Fire native emission check", 0, 0);
        if (window == 0) throw new AssertionError("Hidden GL context unavailable");
        try (var zip = new ZipFile(args[0])) {
            boolean deferred = com.sange.ambientgogga.client.compat.SeaFireShaderCompat.usesDeferredEyes(java.nio.file.Path.of(args[0]));
            GLFW.glfwMakeContextCurrent(window); GL.createCapabilities();
            int program = GL20.glCreateProgram();
            for (String extension : new String[]{"vsh", "fsh"}) {
                String source = SeaFireShaderCompileCheck.expand(zip, "shaders/world0/gbuffers_spidereyes." + extension, 0);
                int line = source.indexOf('\n');
                source = source.substring(0, line + 1) + "#define IS_IRIS\n#define MC_VERSION 12101\n#define MC_HAND_DEPTH 0.125\n" + source.substring(line + 1);
                // Iris preprocesses legal macro overrides before the driver sees GLSL; this test expands includes directly.
                source = source.replace("#define ATMOSPHERE_RAIN_DESATURATION_INTENSITY", "#undef ATMOSPHERE_RAIN_DESATURATION_INTENSITY\n#define ATMOSPHERE_RAIN_DESATURATION_INTENSITY");
                int shader = GL20.glCreateShader(extension.equals("vsh") ? GL20.GL_VERTEX_SHADER : GL20.GL_FRAGMENT_SHADER);
                GL20.glShaderSource(shader, source); GL20.glCompileShader(shader);
                require(GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) != 0, GL20.glGetShaderInfoLog(shader));
                GL20.glAttachShader(program, shader); GL20.glDeleteShader(shader);
            }
            GL20.glLinkProgram(program);
            require(GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) != 0, GL20.glGetProgramInfoLog(program));
            GL20.glUseProgram(program);
            float[] identity = new org.joml.Matrix4f().get(new float[16]);
            for (String name : new String[]{"gbufferModelView", "gbufferModelViewInverse", "gbufferProjection", "gbufferProjectionInverse"})
                GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(program, name), false, identity);
            int target = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, target);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, 16, 16, 0, GL11.GL_RGBA, GL11.GL_FLOAT, (java.nio.ByteBuffer) null);
            int framebuffer = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, target, 0);
            int depth = GL30.glGenRenderbuffers();
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depth);
            GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL14.GL_DEPTH_COMPONENT24, 16, 16);
            GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, depth);
            require(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE, "Incomplete test target");
            int white = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, white);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, new int[]{-1});
            GL11.glViewport(0, 0, 16, 16);
            GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_BLEND);
            GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity();
            GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity();
            if (deferred) {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthMask(true); GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
                GL11.glDepthMask(false);
                draw(1);
                require(readDepth() == 1, "Old emissive path should reproduce missing particle depth");
                GL11.glDepthMask(true);
                draw(1);
                require(readDepth() < 1, "Deferred particle depth was not written");
                var material = BufferUtils.createFloatBuffer(4);
                GL11.glReadPixels(8, 8, 1, 1, GL11.GL_RGBA, GL11.GL_FLOAT, material);
                int packed = Math.round(material.get(1) * 65535);
                require((packed >> 8) == 32 && (packed & 255) > 0, "Photon full-emissive material was not encoded");
                require(GL11.glGetError() == GL11.GL_NO_ERROR, "Photon material pass GL error");
                System.out.println("PASS actual Photon vertex/fragment draw: reproduced missing depth, verified corrected depth and native full-emissive material 32.");
            } else {
            float peak = draw(1), trough = draw(0.75F), faded = draw(0);
            require(peak > trough && trough > 0 && faded == 0, "Emissive RGB must blink and fade to zero");
            require(Math.abs(trough / peak - 0.75F) < 0.02F, "Emissive conversion changed the blink contrast");
            require(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL emissive render error");
            System.out.printf("PASS actual Eclipse emissive vertex/fragment link and floating-point draw: peak %.6f, trough %.6f, faded %.6f.%n", peak, trough, faded);
            }
            GL20.glUseProgram(0); GL20.glDeleteProgram(program);
            GL30.glDeleteFramebuffers(framebuffer); GL11.glDeleteTextures(target); GL11.glDeleteTextures(white);
            GL30.glDeleteRenderbuffers(depth);
            verifyWaterDepth();
        } finally {
            GLFW.glfwDestroyWindow(window); GLFW.glfwTerminate();
        }
    }

    private static float readDepth() {
        var pixel = BufferUtils.createFloatBuffer(1);
        GL11.glReadPixels(8, 8, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, pixel);
        return pixel.get(0);
    }

    private static void verifyWaterDepth() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL20.glUseProgram(0);
        GL11.glViewport(0, 0, 16, 16);
        GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glDepthFunc(GL11.GL_LESS);
        GL11.glDisable(GL11.GL_CULL_FACE); GL11.glDisable(GL11.GL_BLEND);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadMatrixf(new org.joml.Matrix4f().ortho(-0.05F, 0.05F, -0.05F, 0.05F, 0.01F, 2).get(new float[16]));
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadMatrixf(new org.joml.Matrix4f().lookAt(0, -0.12F, 0.3F, 0, 0, 0, 0, 1, 0).get(new float[16]));
        GL11.glDepthMask(true); GL11.glClearDepth(1); GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(-1, 0, -1); GL11.glVertex3f(1, 0, -1);
        GL11.glVertex3f(1, 0, 1); GL11.glVertex3f(-1, 0, 1);
        GL11.glEnd();
        GL11.glDepthMask(false);
        int oldSamples = quadSamples(0.002);
        int fixedSamples = quadSamples(SeaFireGeometry.bottom(0.002, -0.12, 0.018F, 0));
        require(oldSamples == 0 && fixedSamples > 0, "Underwater water-depth occlusion was not repaired");
        GL11.glDepthMask(true); GL11.glClearDepth(0); GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        require(quadSamples(SeaFireGeometry.bottom(0.002, -0.12, 0.018F, 0)) == 0, "Foreground depth must still occlude particles");
        GL11.glClearDepth(1);
        require(GL11.glGetError() == GL11.GL_NO_ERROR, "Underwater GL check failed");
        System.out.println("PASS underwater depth draw: old quad hidden, corrected quad visible, foreground occlusion retained.");
    }

    private static int quadSamples(double bottom) {
        int query = GL15.glGenQueries();
        GL15.glBeginQuery(GL15.GL_SAMPLES_PASSED, query);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(-0.018, bottom, 0); GL11.glVertex3d(0.018, bottom, 0);
        GL11.glVertex3d(0.018, bottom + 0.036, 0); GL11.glVertex3d(-0.018, bottom + 0.036, 0);
        GL11.glEnd();
        GL15.glEndQuery(GL15.GL_SAMPLES_PASSED);
        int samples = GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT);
        GL15.glDeleteQueries(query);
        return samples;
    }

    private static float draw(float intensity) {
        GL11.glClearColor(0, 0, 0, 0); GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glColor4f(SeaFireEmission.scale(0.1F, intensity), SeaFireEmission.scale(0.5F, intensity),
                SeaFireEmission.scale(1, intensity), 1);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(-1, -1, -1);
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(1, -1, -1);
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(1, 1, -1);
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(-1, 1, -1);
        GL11.glEnd();
        var pixel = BufferUtils.createFloatBuffer(4);
        GL11.glReadPixels(8, 8, 1, 1, GL11.GL_RGBA, GL11.GL_FLOAT, pixel);
        return pixel.get(2);
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
