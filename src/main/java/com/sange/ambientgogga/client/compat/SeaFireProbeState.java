package com.sange.ambientgogga.client.compat;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.*;

/** Restores the states touched by Iris shader activation and the private rasterizer-discard draw. */
final class SeaFireProbeState implements AutoCloseable {
    private final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
    private final int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
    private final int array = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
    private final int feedback = GL11.glGetInteger(GL40.GL_TRANSFORM_FEEDBACK_BINDING);
    private final int feedbackBuffer = GL11.glGetInteger(GL30.GL_TRANSFORM_FEEDBACK_BUFFER_BINDING);
    private final int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
    private final int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
    private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final boolean discard = GL11.glIsEnabled(GL30.GL_RASTERIZER_DISCARD);
    private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
    private final int src = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dst = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
    private final int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private static final int[] TARGETS = {GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_3D,
            GL13.GL_TEXTURE_CUBE_MAP, GL30.GL_TEXTURE_2D_ARRAY, GL31.GL_TEXTURE_BUFFER};
    private static final int[] BINDINGS = {GL11.GL_TEXTURE_BINDING_1D, GL11.GL_TEXTURE_BINDING_2D, GL12.GL_TEXTURE_BINDING_3D,
            GL13.GL_TEXTURE_BINDING_CUBE_MAP, GL30.GL_TEXTURE_BINDING_2D_ARRAY, GL31.GL_TEXTURE_BINDING_BUFFER};
    private final int[][] textures;
    private final int[] samplers;
    private final int[][] images;
    private final int[][] blends;

    SeaFireProbeState() {
        // Iris 1.21.1's SamplerLimits uses the fragment-stage unit limit for its allocator.
        textures = new int[GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS)][TARGETS.length];
        samplers = new int[textures.length];
        for (int i = 0; i < textures.length; i++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
            for (int t = 0; t < TARGETS.length; t++) textures[i][t] = GL11.glGetInteger(BINDINGS[t]);
            samplers[i] = GL30.glGetIntegeri(GL33.GL_SAMPLER_BINDING, i);
        }
        GL13.glActiveTexture(activeTexture);
        images = new int[GL.getCapabilities().OpenGL42 ? GL11.glGetInteger(GL42.GL_MAX_IMAGE_UNITS) : 0][6];
        for (int i = 0; i < images.length; i++) {
            images[i] = new int[]{GL30.glGetIntegeri(GL42.GL_IMAGE_BINDING_NAME, i),
                    GL30.glGetIntegeri(GL42.GL_IMAGE_BINDING_LEVEL, i), GL30.glGetIntegeri(GL42.GL_IMAGE_BINDING_LAYERED, i),
                    GL30.glGetIntegeri(GL42.GL_IMAGE_BINDING_LAYER, i), GL30.glGetIntegeri(GL42.GL_IMAGE_BINDING_ACCESS, i),
                    GL30.glGetIntegeri(GL42.GL_IMAGE_BINDING_FORMAT, i)};
        }
        blends = new int[GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS)][7];
        for (int i = 0; i < blends.length; i++) {
            blends[i] = new int[]{GL30.glIsEnabledi(GL11.GL_BLEND, i) ? 1 : 0,
                    GL30.glGetIntegeri(GL14.GL_BLEND_SRC_RGB, i), GL30.glGetIntegeri(GL14.GL_BLEND_DST_RGB, i),
                    GL30.glGetIntegeri(GL14.GL_BLEND_SRC_ALPHA, i), GL30.glGetIntegeri(GL14.GL_BLEND_DST_ALPHA, i),
                    GL30.glGetIntegeri(GL20.GL_BLEND_EQUATION_RGB, i), GL30.glGetIntegeri(GL20.GL_BLEND_EQUATION_ALPHA, i)};
        }
    }

    @Override public void close() {
        GL20.glUseProgram(program);
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, array);
        GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, feedback);
        GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, feedbackBuffer);
        if (discard) GL11.glEnable(GL30.GL_RASTERIZER_DISCARD); else GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
        // Iris updates Minecraft's texture cache. Restore through that cache for vanilla units.
        for (int i = 0; i < textures.length; i++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
            if (i < 12) {
                GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
                GlStateManager._bindTexture(textures[i][1]);
            }
            for (int t = 0; t < TARGETS.length; t++) GL11.glBindTexture(TARGETS[t], textures[i][t]);
            GL33.glBindSampler(i, samplers[i]);
        }
        // Raw GL changes above do not update the cached active unit.
        GL13.glActiveTexture(activeTexture);
        GlStateManager._activeTexture(activeTexture);
        for (int i = 0; i < images.length; i++) {
            int[] binding = images[i];
            GL42.glBindImageTexture(i, binding[0], binding[1], binding[2] != 0, binding[3], binding[4], binding[5]);
        }
        if (blend) GlStateManager._enableBlend(); else GlStateManager._disableBlend();
        GlStateManager._blendFuncSeparate(src, dst, srcAlpha, dstAlpha);
        for (int i = 0; i < blends.length; i++) {
            int[] b = blends[i];
            if (b[0] != 0) GL30.glEnablei(GL11.GL_BLEND, i); else GL30.glDisablei(GL11.GL_BLEND, i);
            GL40.glBlendFuncSeparatei(i, b[1], b[2], b[3], b[4]);
            GL40.glBlendEquationSeparatei(i, b[5], b[6]);
        }
    }
}
