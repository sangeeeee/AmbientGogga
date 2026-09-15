package com.sange.ambientgogga.client.particle;

import com.sange.ambientgogga.config.ClientConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** A softly luminous lavender, ice-white or blue mote left along the flight path. */
public final class ShichieichouTrailParticle extends TextureSheetParticle {
    private final float initialSize;
    private final float maximumAlpha;
    private final float twinklePhase;
    private final float twinkleSpeed;

    private ShichieichouTrailParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            SpriteSet sprites
    ) {
        super(level, x, y, z, velocityX, velocityY, velocityZ);
        // Particle's velocity constructor adds a large random kick. Restore the
        // emitter's small, directional velocity so the dust follows the wake.
        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;
        this.lifetime = 40 + this.random.nextInt(21);
        this.initialSize = (0.010F + this.random.nextFloat() * 0.006F)
                * ClientConfig.SHICHIEICHOU.DUST_SIZE.get().floatValue();
        this.quadSize = this.initialSize;
        this.maximumAlpha = 0.78F + this.random.nextFloat() * 0.17F;
        this.twinklePhase = this.random.nextFloat() * Mth.TWO_PI;
        this.twinkleSpeed = 0.45F + this.random.nextFloat() * 0.35F;
        this.alpha = 0.20F;
        this.gravity = 0.002F;
        this.friction = 0.985F;
        this.hasPhysics = true;
        this.setSize(0.01F, 0.01F);
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;

        int color = switch (this.random.nextInt(3)) {
            case 0 -> 0xE1CCFF; // Pale lavender.
            case 1 -> 0xEAF7FF; // Ice blue, almost white.
            default -> 0x70ACFF; // Blue accent among the paler dust.
        };
        this.setColor(((color >> 16) & 255) / 255.0F, ((color >> 8) & 255) / 255.0F,
                (color & 255) / 255.0F);
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isAlive()) {
            return;
        }

        float progress = Mth.clamp((float) this.age / this.lifetime, 0.0F, 1.0F);
        float fadeIn = Mth.clamp((this.age + 1) / 2.0F, 0.0F, 1.0F);
        float fadeOut = (1.0F - progress) * (1.0F - progress);
        float shimmer = 0.5F + 0.5F * Mth.sin(this.age * this.twinkleSpeed + this.twinklePhase);
        this.alpha = this.maximumAlpha * fadeIn * fadeOut * (0.55F + 0.45F * shimmer * shimmer);
        this.quadSize = this.initialSize * (1.0F - progress * 0.20F);
    }

    @Override
    protected int getLightColor(float partialTick) {
        int light = super.getLightColor(partialTick);
        // Readable at night without marking every mote as maximum brightness.
        return (light & 0xFFFF0000) | Math.max(light & 0xFFFF, 11 << 4);
    }

    // Trim transparent padding. Previously the visible dot occupied only 1/8
    // of an already tiny quad, making it subpixel at normal viewing distances.
    @Override
    protected float getU0() { return this.sprite.getU(0.25F); }

    @Override
    protected float getU1() { return this.sprite.getU(0.875F); }

    @Override
    protected float getV0() { return this.sprite.getV(0.125F); }

    @Override
    protected float getV1() { return this.sprite.getV(0.875F); }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double velocityX,
                double velocityY,
                double velocityZ
        ) {
            return new ShichieichouTrailParticle(
                    level,
                    x,
                    y,
                    z,
                    velocityX,
                    velocityY,
                    velocityZ,
                    this.sprites
            );
        }
    }
}
