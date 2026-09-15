package com.sange.ambientgogga.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import com.sange.ambientgogga.config.ClientConfig;
import com.sange.ambientgogga.client.compat.SeaFireShaderCompat;
import java.lang.reflect.Method;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;

/** One batch using Iris's compact fullbright SpiderEyes variant, or the ordinary particle format. */
public final class SeaFireRenderType implements ParticleRenderType {
    public static final SeaFireRenderType INSTANCE = new SeaFireRenderType();
    private Api api;
    private boolean unavailable, emissiveBatch;
    private float strength = 1;

    @Override public BufferBuilder begin(Tesselator tesselator, TextureManager textures) {
        emissiveBatch = false;
        ShaderInstance shader = resolveShader();
        if (shader == null) return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT.begin(tesselator, textures);
        emissiveBatch = true;
        strength = ClientConfig.SEA_FIRE.SHADER_EMISSIVE_STRENGTH.get().floatValue();
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
        RenderSystem.enableBlend();
        if (SeaFireShaderCompat.deferredEmissive()) {
            // Packed material data must never be blended. Deferred lighting also requires the particle depth.
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
        } else {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.depthMask(false);
        }
        // SPS uses this compact format, avoiding terrain/entity metadata and extra per-quad tangent work.
        return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
    }

    public boolean emissiveBatch() { return emissiveBatch; }
    public float strength() { return strength; }

    @Override public boolean isTranslucent() {
        // NeoForge/Iris render opaque particles before deferred lighting and the translucent water pass.
        return !SeaFireShaderCompat.deferredEmissive();
    }

    private ShaderInstance resolveShader() {
        // The exact water protocol lives in the particle shader and retains priority when selected.
        if (unavailable || !SeaFireShaderCompat.useEmissive()) return null;
        try {
            if (api == null) api = new Api();
            if (Boolean.TRUE.equals(api.shadow.invoke(api.instance))) return null;
            Object pipeline = api.pipeline.invoke(api.manager.invoke(null));
            if (pipeline == null || !api.pipelineType.isInstance(pipeline)) return null;
            ShaderInstance shader = (ShaderInstance) api.shader.invoke(api.map.invoke(pipeline), api.key);
            if (shader != null && shader.getVertexFormat() != DefaultVertexFormat.POSITION_TEX_COLOR)
                throw new IllegalStateException("Unexpected Iris SPS vertex format");
            return shader;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            unavailable = true;
            LogUtils.getLogger().warn("Sea Fire Iris emissive pass unavailable; retaining ordinary particle rendering.", error);
            return null;
        }
    }

    private static final class Api {
        final Class<?> pipelineType;
        final Object instance, key;
        final Method shadow, manager, pipeline, map, shader;
        Api() throws ReflectiveOperationException {
            Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            instance = irisApi.getMethod("getInstance").invoke(null);
            shadow = irisApi.getMethod("isRenderingShadowPass");
            manager = Class.forName("net.irisshaders.iris.Iris").getMethod("getPipelineManager");
            pipeline = Class.forName("net.irisshaders.iris.pipeline.PipelineManager").getMethod("getPipelineNullable");
            pipelineType = Class.forName("net.irisshaders.iris.pipeline.IrisRenderingPipeline");
            map = pipelineType.getMethod("getShaderMap");
            Class<?> keys = Class.forName("net.irisshaders.iris.pipeline.programs.ShaderKey");
            key = keys.getField("SPS").get(null);
            shader = Class.forName("net.irisshaders.iris.pipeline.programs.ShaderMap").getMethod("getShader", keys);
        }
    }

    @Override public String toString() { return "AMBIENTGOGGA_SEA_FIRE"; }
    private SeaFireRenderType() { }
}
