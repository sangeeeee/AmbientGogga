package com.sange.ambientgogga.client.compat;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import com.sange.ambientgogga.AmbientGogga;
import com.sange.ambientgogga.config.ClientConfig;
import com.sange.ambientgogga.client.particle.SeaFireParticle;
import java.lang.reflect.Method;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL40;

/** Optional vegetation-tag displacement sampling. The visible particles retain their normal render pass. */
@EventBusSubscriber(modid = AmbientGogga.MODID, value = Dist.CLIENT)
public final class SeaFireVegetationWaves {
    private static final SeaFireWaveField FIELD = new SeaFireWaveField();
    private static final float[] POSITIONS = new float[SeaFireWaveField.SIDE * SeaFireWaveField.SIDE * 3];
    private static Api api;
    private static SeaFireVertexProbe probe;
    private static Object pipelineIdentity, failedPipeline;
    private static ShaderInstance shaderIdentity;
    private static boolean apiFailed;
    private static long nextSample, submitted;
    private static double pendingX, pendingZ, pendingStep;

    public static float height(double x, double z) { return FIELD.sample(x, z); }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        // This stage also fires over empty ocean where no cutout chunk geometry was submitted.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (!SeaFireShaderCompat.useVegetation() || !SeaFireParticle.hasLiveParticles()
                || ClientConfig.SEA_FIRE.VEGETATION_STRENGTH.get() <= 0
                || ClientConfig.SEA_FIRE.VEGETATION_MAX_HEIGHT.get() <= 0) {
            release();
            return;
        }
        long now = System.nanoTime();
        if (!Minecraft.getInstance().isPaused()) FIELD.frame(now);
        if (apiFailed || !GL.getCapabilities().OpenGL40) return;
        Object pipeline = null;
        try {
            if (api == null) api = new Api();
            if (Boolean.TRUE.equals(api.shadow.invoke(api.instance))) return;
            pipeline = api.pipeline.invoke(api.manager.invoke(null));
            if (pipeline == null || pipeline == failedPipeline) return;
            if (pipeline != pipelineIdentity) {
                release();
                pipelineIdentity = pipeline;
                failedPipeline = null;
            }
            if (probe != null) {
                float[] heights = probe.poll(ClientConfig.SEA_FIRE.VEGETATION_STRENGTH.get().floatValue(),
                        ClientConfig.SEA_FIRE.VEGETATION_MAX_HEIGHT.get().floatValue());
                if (heights != null) FIELD.update(pendingX, pendingZ, pendingStep, heights, now);
                if (probe.pending()) {
                    if (now - submitted > 2_000_000_000L) throw new IllegalStateException("Vertex probe result expired");
                    return;
                }
            }
            if (now < nextSample || Minecraft.getInstance().isPaused()) return;
            nextSample = now + 100_000_000L;
            // Never interrupt another mod's transform-feedback operation.
            if (GL11.glGetBoolean(GL40.GL_TRANSFORM_FEEDBACK_BUFFER_ACTIVE)) return;
            ShaderInstance shader = (ShaderInstance) api.getShader.invoke(api.shaderMap.invoke(pipeline), api.key);
            if (shader == null) return;
            int material = api.material();
            if (material < 0) return;
            if (shader != shaderIdentity || probe == null) {
                release();
                pipelineIdentity = pipeline;
                shaderIdentity = shader;
                probe = new SeaFireVertexProbe(shader.getId(), SeaFireWaveField.SIDE * SeaFireWaveField.SIDE);
                LogUtils.getLogger().info("Sea Fire: experimental Iris vegetation sampling enabled (17x17, at most 10 Hz).");
            }
            var mc = Minecraft.getInstance();
            if (mc.level == null) return;
            var eye = event.getCamera().getPosition();
            pendingStep = Math.max(4, Math.ceil(ClientConfig.SEA_FIRE.RADIUS.get() / 7));
            pendingX = Math.floor(eye.x / pendingStep) * pendingStep - 8 * pendingStep;
            pendingZ = Math.floor(eye.z / pendingStep) * pendingStep - 8 * pendingStep;
            for (int row = 0; row < SeaFireWaveField.SIDE; row++) {
                for (int col = 0; col < SeaFireWaveField.SIDE; col++) {
                    int i = (row * SeaFireWaveField.SIDE + col) * 3;
                    POSITIONS[i] = (float) (pendingX + col * pendingStep - eye.x);
                    POSITIONS[i + 1] = (float) (mc.level.getSeaLevel() - 1.0 / 9 - eye.y);
                    POSITIONS[i + 2] = (float) (pendingZ + row * pendingStep - eye.z);
                }
            }
            Matrix4f inverse = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix()).invert();
            try (var state = new SeaFireProbeState()) {
                // Initialize the vanilla terrain variant even when Sodium rendered all real terrain.
                shader.setDefaultUniforms(VertexFormat.Mode.QUADS, event.getModelViewMatrix(),
                        event.getProjectionMatrix(), mc.getWindow());
                try {
                    shader.apply();
                    probe.submit(POSITIONS, material, inverse);
                    submitted = now;
                } finally { shader.clear(); }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            release();
            if (pipeline == null) apiFailed = true; else failedPipeline = pipeline;
            LogUtils.getLogger().warn("Sea Fire vegetation sampling unavailable; retaining ordinary surface particles. Reload the shader pack to retry.", error);
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        // Client events are normally on the render thread; execute also handles a scheduled disconnect.
        Minecraft.getInstance().execute(SeaFireVegetationWaves::release);
    }

    private static void release() {
        if (probe != null) probe.close();
        probe = null; shaderIdentity = null; pipelineIdentity = null;
        FIELD.clear();
    }

    private static final class Api {
        final Object instance, key, settings;
        final Method shadow, manager, pipeline, shaderMap, getShader, stateIds;
        Api() throws ReflectiveOperationException {
            Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            instance = irisApi.getMethod("getInstance").invoke(null);
            shadow = irisApi.getMethod("isRenderingShadowPass");
            manager = Class.forName("net.irisshaders.iris.Iris").getMethod("getPipelineManager");
            pipeline = Class.forName("net.irisshaders.iris.pipeline.PipelineManager").getMethod("getPipelineNullable");
            shaderMap = Class.forName("net.irisshaders.iris.pipeline.IrisRenderingPipeline").getMethod("getShaderMap");
            Class<?> keys = Class.forName("net.irisshaders.iris.pipeline.programs.ShaderKey");
            key = keys.getField("TERRAIN_CUTOUT").get(null);
            getShader = Class.forName("net.irisshaders.iris.pipeline.programs.ShaderMap").getMethod("getShader", keys);
            Class<?> worldSettings = Class.forName("net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings");
            settings = worldSettings.getField("INSTANCE").get(null);
            stateIds = worldSettings.getMethod("getBlockStateIds");
        }
        int material() throws ReflectiveOperationException {
            Object map = stateIds.invoke(settings);
            if (!(map instanceof Map<?, ?> ids)) return -1;
            for (var block : new net.minecraft.world.level.block.Block[]{Blocks.WHEAT, Blocks.SUGAR_CANE, Blocks.OAK_LEAVES}) {
                Object value = ids.get(block.defaultBlockState());
                if (value instanceof Number id && id.intValue() > 0) return id.intValue();
            }
            return -1;
        }
    }

    private SeaFireVegetationWaves() { }
}
