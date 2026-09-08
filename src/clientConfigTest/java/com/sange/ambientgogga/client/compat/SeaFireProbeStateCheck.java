package com.sange.ambientgogga.client.compat;

import org.lwjgl.opengl.*;

/** Exercises the real state guard around the test draw, including texture and sampler restoration. */
public final class SeaFireProbeStateCheck {
    public static void run(Runnable draw) {
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int framebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        boolean discard = GL11.glIsEnabled(GL30.GL_RASTERIZER_DISCARD);
        try (var state = new SeaFireProbeState()) {
            draw.run();
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + 15);
            GL11.glEnable(GL11.GL_BLEND);
        }
        if (program != GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM)
                || vao != GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING)
                || active != GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)
                || framebuffer != GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
                || discard != GL11.glIsEnabled(GL30.GL_RASTERIZER_DISCARD)
                || GL11.glGetError() != GL11.GL_NO_ERROR)
            throw new AssertionError("Probe did not restore OpenGL state");
    }
}
