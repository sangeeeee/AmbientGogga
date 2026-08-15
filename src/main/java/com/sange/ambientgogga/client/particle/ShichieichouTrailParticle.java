package com.sange.ambientgogga.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** A small, non-light-producing blue-white mote left behind by Shichieichou. */
public final class ShichieichouTrailParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float initialSize;
    private final float maximumAlpha;

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
        this.sprites = sprites;
        this.lifetime = 16 + this.random.nextInt(17);
        this.initialSize = 0.025F + this.random.nextFloat() * 0.03F;
        this.quadSize = this.initialSize;
        this.maximumAlpha = 0.45F + this.random.nextFloat() * 0.25F;
        this.alpha = 0.0F;
        this.gravity = 0.0F;
        this.friction = 0.92F;
        this.hasPhysics = false;

        float whiteness = this.random.nextFloat();
        this.setColor(
                0.58F + whiteness * 0.20F,
                0.82F + whiteness * 0.14F,
                1.0F
        );
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isAlive()) {
            return;
        }

        float progress = Mth.clamp((float) this.age / this.lifetime, 0.0F, 1.0F);
        float fadeIn = Mth.clamp(this.age / 3.0F, 0.0F, 1.0F);
        float fadeOut = 1.0F - progress;
        this.alpha = this.maximumAlpha * fadeIn * fadeOut * fadeOut;
        this.quadSize = this.initialSize * (1.0F - progress * 0.45F);
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

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
