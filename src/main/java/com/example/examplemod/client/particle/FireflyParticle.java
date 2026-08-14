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
    private static final int MIN_LIFETIME = 240;
    private static final int EXTRA_LIFETIME = 361;
    private static final int MIN_FADE_IN_TICKS = 20;
    private static final int EXTRA_FADE_IN_TICKS = 31;
    private static final int MIN_FADE_OUT_TICKS = 40;
    private static final int EXTRA_FADE_OUT_TICKS = 41;
    private static final double STEERING_FACTOR = 0.045;
    private static final double MAX_ROAM_DISTANCE = 12.0;

    private final SpriteSet sprites;
    private final double originX;
    private final double originY;
    private final double originZ;
    private final int fadeInTicks;
    private final int fadeOutTicks;
    private final float maximumAlpha;
    private final float blinkSpeed;
    private final float blinkPhase;
    private final double motionFrequencyX;
    private final double motionFrequencyY;
    private final double motionFrequencyZ;
    private final double motionPhaseX;
    private final double motionPhaseY;
    private final double motionPhaseZ;

    private double targetVelocityX;
    private double targetVelocityY;
    private double targetVelocityZ;
    private int steeringTicks;

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
        this.fadeInTicks = MIN_FADE_IN_TICKS + this.random.nextInt(EXTRA_FADE_IN_TICKS);
        this.fadeOutTicks = MIN_FADE_OUT_TICKS + this.random.nextInt(EXTRA_FADE_OUT_TICKS);
        this.quadSize = 0.018F + this.random.nextFloat() * 0.042F;
        this.maximumAlpha = 0.82F + this.random.nextFloat() * 0.18F;
        this.blinkPhase = this.random.nextFloat() * Mth.TWO_PI;
        this.blinkSpeed = 0.045F + this.random.nextFloat() * 0.025F;
        this.motionFrequencyX = randomBetween(0.025, 0.055);
        this.motionFrequencyY = randomBetween(0.020, 0.045);
        this.motionFrequencyZ = randomBetween(0.030, 0.060);
        this.motionPhaseX = this.random.nextDouble() * Mth.TWO_PI;
        this.motionPhaseY = this.random.nextDouble() * Mth.TWO_PI;
        this.motionPhaseZ = this.random.nextDouble() * Mth.TWO_PI;
        this.steeringTicks = 0;
        this.gravity = 0.0F;
        this.friction = 0.985F;
        this.hasPhysics = false;
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

        double motionTime = this.age;
        double desiredVelocityX = this.targetVelocityX
                + Math.sin(motionTime * this.motionFrequencyX + this.motionPhaseX) * 0.006
                + Math.sin(motionTime * this.motionFrequencyY * 0.53 + this.motionPhaseZ) * 0.003;
        double desiredVelocityY = this.targetVelocityY
                + Math.sin(motionTime * this.motionFrequencyY + this.motionPhaseY) * 0.004
                + Math.cos(motionTime * this.motionFrequencyX * 0.61 + this.motionPhaseX) * 0.002;
        double desiredVelocityZ = this.targetVelocityZ
                + Math.cos(motionTime * this.motionFrequencyZ + this.motionPhaseZ) * 0.006
                + Math.sin(motionTime * this.motionFrequencyX * 0.47 + this.motionPhaseY) * 0.003;

        this.xd += (desiredVelocityX - this.xd) * STEERING_FACTOR;
        this.yd += (desiredVelocityY - this.yd) * STEERING_FACTOR;
        this.zd += (desiredVelocityZ - this.zd) * STEERING_FACTOR;
        this.move(this.xd, this.yd, this.zd);

        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;
        this.setSpriteFromAge(this.sprites);
    }

    private void updateGlow() {
        float wave = (Mth.sin(this.blinkPhase + this.age * this.blinkSpeed) + 1.0F) * 0.5F;
        float pulse = 0.24F + 0.76F * smoothStep(wave);
        float fadeIn = smoothStep(Mth.clamp((float) this.age / this.fadeInTicks, 0.0F, 1.0F));
        float fadeOut = smoothStep(Mth.clamp((float) (this.lifetime - this.age) / this.fadeOutTicks, 0.0F, 1.0F));
        this.alpha = this.maximumAlpha * pulse * fadeIn * fadeOut;
    }

    private void updateSteering() {
        if (this.steeringTicks-- > 0) {
            return;
        }

        double offsetX = this.x - this.originX;
        double offsetY = this.y - this.originY;
        double offsetZ = this.z - this.originZ;
        double distanceSquared = offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ;
        if (distanceSquared > MAX_ROAM_DISTANCE * MAX_ROAM_DISTANCE) {
            double inverseDistance = 1.0 / Math.sqrt(distanceSquared);
            this.targetVelocityX = -offsetX * inverseDistance * 0.020;
            this.targetVelocityY = -offsetY * inverseDistance * 0.014;
            this.targetVelocityZ = -offsetZ * inverseDistance * 0.020;
            this.steeringTicks = 30;
            return;
        }

        double returnX = Mth.clamp((this.originX - this.x) * 0.0015, -0.006, 0.006);
        double returnY = Mth.clamp((this.originY - this.y) * 0.0020, -0.005, 0.005);
        double returnZ = Mth.clamp((this.originZ - this.z) * 0.0015, -0.006, 0.006);
        this.targetVelocityX = randomBetween(-0.018, 0.018) + returnX;
        this.targetVelocityY = randomBetween(-0.010, 0.010) + returnY;
        this.targetVelocityZ = randomBetween(-0.018, 0.018) + returnZ;
        this.steeringTicks = 45 + this.random.nextInt(76);
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
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
