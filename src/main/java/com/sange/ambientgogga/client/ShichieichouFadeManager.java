package com.sange.ambientgogga.client;

import com.sange.ambientgogga.AmbientGogga;
import com.sange.ambientgogga.entity.Shichieichou;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/** Renders the non-interactive afterimage left by a naturally despawning shichieichou. */
@EventBusSubscriber(modid = AmbientGogga.MODID, value = Dist.CLIENT)
public final class ShichieichouFadeManager {
    private static final int FADE_DURATION_TICKS = 30;
    private static final double VELOCITY_DAMPING = 0.88D;
    private static final List<FadeGhost> GHOSTS = new ArrayList<>();

    private static ClientLevel activeLevel;

    private ShichieichouFadeManager() {
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ClientLevel clientLevel)
                || !(event.getEntity() instanceof Shichieichou shichieichou)
                || !shichieichou.shouldPlayNaturalDespawnFade()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != clientLevel) {
            return;
        }

        ensureLevel(clientLevel);
        shichieichou.setClientFadeProgress(0.001F);
        GHOSTS.add(new FadeGhost(shichieichou));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clear();
            return;
        }

        ensureLevel(minecraft.level);
        if (minecraft.isPaused()) {
            return;
        }

        Iterator<FadeGhost> iterator = GHOSTS.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().tick()) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || GHOSTS.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.level != activeLevel) {
            clear();
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        Vec3 cameraPosition = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        for (FadeGhost ghost : GHOSTS) {
            Shichieichou shichieichou = ghost.entity();
            double x = Mth.lerp(partialTick, shichieichou.xOld, shichieichou.getX());
            double y = Mth.lerp(partialTick, shichieichou.yOld, shichieichou.getY());
            double z = Mth.lerp(partialTick, shichieichou.zOld, shichieichou.getZ());
            float yaw = Mth.lerp(partialTick, shichieichou.yRotO, shichieichou.getYRot());
            dispatcher.render(
                    shichieichou,
                    x - cameraPosition.x,
                    y - cameraPosition.y,
                    z - cameraPosition.z,
                    yaw,
                    partialTick,
                    poseStack,
                    bufferSource,
                    dispatcher.getPackedLightCoords(shichieichou, partialTick)
            );
        }

        bufferSource.endBatch();
    }

    private static void ensureLevel(ClientLevel level) {
        if (activeLevel != level) {
            GHOSTS.clear();
            activeLevel = level;
        }
    }

    private static void clear() {
        GHOSTS.clear();
        activeLevel = null;
    }

    private static final class FadeGhost {
        private final Shichieichou entity;
        private int age;

        private FadeGhost(Shichieichou entity) {
            this.entity = entity;
        }

        private Shichieichou entity() {
            return this.entity;
        }

        private boolean tick() {
            this.age++;
            if (this.age >= FADE_DURATION_TICKS) {
                return true;
            }

            this.entity.xOld = this.entity.getX();
            this.entity.yOld = this.entity.getY();
            this.entity.zOld = this.entity.getZ();
            this.entity.xo = this.entity.getX();
            this.entity.yo = this.entity.getY();
            this.entity.zo = this.entity.getZ();
            this.entity.xRotO = this.entity.getXRot();
            this.entity.yRotO = this.entity.getYRot();
            this.entity.yBodyRotO = this.entity.yBodyRot;
            this.entity.yHeadRotO = this.entity.yHeadRot;

            Vec3 movement = this.entity.getDeltaMovement();
            this.entity.setPos(this.entity.position().add(movement));
            this.entity.setDeltaMovement(movement.scale(VELOCITY_DAMPING));
            this.entity.tickCount++;
            this.entity.setClientFadeProgress((float) this.age / FADE_DURATION_TICKS);
            return false;
        }
    }
}
