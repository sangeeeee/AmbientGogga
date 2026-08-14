package com.example.examplemod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class FireflyParticle extends TextureSheetParticle {
    private static final int MIN_LIFETIME = 160;
    private static final int EXTRA_LIFETIME = 121;
    private static final int FADE_IN_TICKS = 10;
    private static final int FADE_OUT_TICKS = 20;
    private static final double STEERING_FACTOR = 0.08;

    private final SpriteSet sprites;
    private final double originX;
    private final double originY;
    private final double originZ;
    private final float blinkSpeed;

    private double targetVelocityX;
    private double targetVelocityY;
    private double targetVelocityZ;
    private int steeringTicks;
    private float blinkPhase;

    private FireflyParticle(
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
        this.originX = x;
        this.originY = y;
        this.originZ = z;
        this.lifetime = MIN_LIFETIME + this.random.nextInt(EXTRA_LIFETIME);
        this.quadSize = 0.06F + this.random.nextFloat() * 0.035F;
        this.blinkPhase = this.random.nextFloat() * Mth.TWO_PI;
        this.blinkSpeed = 0.16F + this.random.nextFloat() * 0.12F;
        this.steeringTicks = 0;
        this.gravity = 0.0F;
        this.friction = 0.96F;
        this.hasPhysics = true;
        this.alpha = 0.0F;

        float warmth = this.random.nextFloat();
        this.setColor(1.0F, 0.9F + warmth * 0.1F, 0.62F + warmth * 0.18F);
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        updateGlow();
        updateSteering();

        this.xd += (this.targetVelocityX - this.xd) * STEERING_FACTOR;
        this.yd += (this.targetVelocityY - this.yd) * STEERING_FACTOR;
        this.zd += (this.targetVelocityZ - this.zd) * STEERING_FACTOR;
        this.move(this.xd, this.yd, this.zd);

        if (this.onGround) {
            this.yd = Math.max(this.yd, 0.008);
            this.steeringTicks = 0;
        }

        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;
        this.setSpriteFromAge(this.sprites);
    }

    private void updateGlow() {
        this.blinkPhase += this.blinkSpeed;
        float wave = (Mth.sin(this.blinkPhase) + 1.0F) * 0.5F;
        float pulse = 0.18F + 0.82F * wave * wave;
        float fadeIn = Mth.clamp((float)this.age / FADE_IN_TICKS, 0.0F, 1.0F);
        float fadeOut = Mth.clamp((float)(this.lifetime - this.age) / FADE_OUT_TICKS, 0.0F, 1.0F);
        this.alpha = pulse * Math.min(fadeIn, fadeOut);
    }

    private void updateSteering() {
        if (this.steeringTicks-- > 0) {
            return;
        }

        double returnX = Mth.clamp((this.originX - this.x) * 0.004, -0.012, 0.012);
        double returnY = Mth.clamp((this.originY - this.y) * 0.006, -0.008, 0.008);
        double returnZ = Mth.clamp((this.originZ - this.z) * 0.004, -0.012, 0.012);
        this.targetVelocityX = randomBetween(-0.010, 0.010) + returnX;
        this.targetVelocityY = randomBetween(-0.004, 0.008) + returnY;
        this.targetVelocityZ = randomBetween(-0.010, 0.010) + returnZ;
        this.steeringTicks = 12 + this.random.nextInt(29);
    }

    private double randomBetween(double minimum, double maximum) {
        return minimum + this.random.nextDouble() * (maximum - minimum);
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
            return new FireflyParticle(level, x, y, z, velocityX, velocityY, velocityZ, this.sprites);
        }
    }
}
