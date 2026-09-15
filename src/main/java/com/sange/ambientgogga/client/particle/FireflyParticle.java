package com.sange.ambientgogga.client.particle;

import com.sange.ambientgogga.config.ClientConfig;
import com.sange.ambientgogga.client.FireflyTiming;
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
    private final double motionSpeed, motionFrequency, steeringFactor, maxRoamDistance;
    private final int minSteeringTicks, maxSteeringTicks;
    private final float minimumGlow;
    private final int minimumLight;

    private final SpriteSet sprites;
    private final double originX;
    private final double originY;
    private final double originZ;
    private final int fadeInTicks;
    private final int fadeOutTicks;
    private final float maximumAlpha;
    private final float blinkFrequencyHz;
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
        this.lifetime = randomTicks(ClientConfig.FIREFLIES.MIN_LIFETIME.get(), ClientConfig.FIREFLIES.MAX_LIFETIME.get());
        this.motionSpeed = ClientConfig.FIREFLIES.MOTION_SPEED.get();
        this.motionFrequency = ClientConfig.FIREFLIES.MOTION_FREQUENCY.get();
        this.steeringFactor = ClientConfig.FIREFLIES.STEERING_FACTOR.get();
        this.maxRoamDistance = ClientConfig.FIREFLIES.ROAM_DISTANCE.get();
        this.minSteeringTicks = ClientConfig.FIREFLIES.MIN_STEERING_TICKS.get();
        this.maxSteeringTicks = ClientConfig.FIREFLIES.MAX_STEERING_TICKS.get();
        this.minimumGlow = ClientConfig.FIREFLIES.MIN_GLOW.get().floatValue();
        this.minimumLight = ClientConfig.FIREFLIES.MINIMUM_LIGHT.get();
        // Particle's velocity constructor randomizes input; start with the supplied velocity.
        this.xd = velocityX * this.motionSpeed;
        this.yd = velocityY * this.motionSpeed;
        this.zd = velocityZ * this.motionSpeed;
        this.fadeInTicks = Math.min(this.lifetime, randomTicks(ClientConfig.FIREFLIES.MIN_FADE_IN.get(), ClientConfig.FIREFLIES.MAX_FADE_IN.get()));
        this.fadeOutTicks = Math.min(this.lifetime, randomTicks(ClientConfig.FIREFLIES.MIN_FADE_OUT.get(), ClientConfig.FIREFLIES.MAX_FADE_OUT.get()));
        this.quadSize = (float) randomBetween(ClientConfig.FIREFLIES.MIN_SIZE.get(), ClientConfig.FIREFLIES.MAX_SIZE.get());
        this.maximumAlpha = (float) randomBetween(ClientConfig.FIREFLIES.MIN_PEAK_ALPHA.get(), ClientConfig.FIREFLIES.MAX_PEAK_ALPHA.get());
        this.blinkPhase = this.random.nextFloat() * Mth.TWO_PI;
        this.blinkFrequencyHz = (float) randomBetween(ClientConfig.FIREFLIES.MIN_BLINK_HZ.get(), ClientConfig.FIREFLIES.MAX_BLINK_HZ.get());
        this.motionFrequencyX = randomBetween(0.025, 0.055);
        this.motionFrequencyY = randomBetween(0.020, 0.045);
        this.motionFrequencyZ = randomBetween(0.030, 0.060);
        this.motionPhaseX = this.random.nextDouble() * Mth.TWO_PI;
        this.motionPhaseY = this.random.nextDouble() * Mth.TWO_PI;
        this.motionPhaseZ = this.random.nextDouble() * Mth.TWO_PI;
        this.steeringTicks = 0;
        this.gravity = 0.0F;
        this.friction = 0.985F;
        this.hasPhysics = true;
        this.alpha = 0.0F;

        float warmth = this.random.nextFloat();
        this.setColor(1.0F, 0.91F + warmth * 0.055F, 0.68F + warmth * 0.10F);
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

        double motionTime = this.age * this.motionFrequency;
        double desiredVelocityX = this.targetVelocityX
                + Math.sin(motionTime * this.motionFrequencyX + this.motionPhaseX) * 0.008
                + Math.sin(motionTime * this.motionFrequencyY * 0.53 + this.motionPhaseZ) * 0.004;
        double desiredVelocityY = this.targetVelocityY
                + Math.sin(motionTime * this.motionFrequencyY + this.motionPhaseY) * 0.0055
                + Math.cos(motionTime * this.motionFrequencyX * 0.61 + this.motionPhaseX) * 0.0028;
        double desiredVelocityZ = this.targetVelocityZ
                + Math.cos(motionTime * this.motionFrequencyZ + this.motionPhaseZ) * 0.008
                + Math.sin(motionTime * this.motionFrequencyX * 0.47 + this.motionPhaseY) * 0.004;

        this.xd += (desiredVelocityX * this.motionSpeed - this.xd) * this.steeringFactor;
        this.yd += (desiredVelocityY * this.motionSpeed - this.yd) * this.steeringFactor;
        this.zd += (desiredVelocityZ * this.motionSpeed - this.zd) * this.steeringFactor;

        double intendedX = this.xd;
        double intendedY = this.yd;
        double intendedZ = this.zd;
        double previousX = this.x;
        double previousY = this.y;
        double previousZ = this.z;
        this.move(intendedX, intendedY, intendedZ);

        boolean collidedX = Math.abs((this.x - previousX) - intendedX) > 1.0E-5;
        boolean collidedY = Math.abs((this.y - previousY) - intendedY) > 1.0E-5;
        boolean collidedZ = Math.abs((this.z - previousZ) - intendedZ) > 1.0E-5;
        if (collidedX || collidedY || collidedZ) {
            redirectAfterCollision(collidedX, collidedY, collidedZ);
        }

        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;
        this.setSpriteFromAge(this.sprites);
    }

    private void redirectAfterCollision(boolean collidedX, boolean collidedY, boolean collidedZ) {
        if (collidedX) {
            this.xd = -this.xd * 0.35;
            this.targetVelocityX = -this.targetVelocityX;
        }
        if (collidedY) {
            this.yd = -this.yd * 0.30;
            this.targetVelocityY = this.onGround
                    ? randomBetween(0.006, 0.016)
                    : -this.targetVelocityY;
        }
        if (collidedZ) {
            this.zd = -this.zd * 0.35;
            this.targetVelocityZ = -this.targetVelocityZ;
        }

        this.steeringTicks = 4 + this.random.nextInt(8);
    }

    private void updateGlow() {
        this.alpha = (float) FireflyTiming.glow(this.age, this.lifetime, this.fadeInTicks, this.fadeOutTicks,
                this.maximumAlpha, this.minimumGlow, this.blinkPhase, this.blinkFrequencyHz);
    }

    private void updateSteering() {
        if (this.steeringTicks-- > 0) {
            return;
        }

        double offsetX = this.x - this.originX;
        double offsetY = this.y - this.originY;
        double offsetZ = this.z - this.originZ;
        double distanceSquared = offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ;
        if (distanceSquared > this.maxRoamDistance * this.maxRoamDistance) {
            double inverseDistance = 1.0 / Math.sqrt(distanceSquared);
            this.targetVelocityX = -offsetX * inverseDistance * 0.028;
            this.targetVelocityY = -offsetY * inverseDistance * 0.020;
            this.targetVelocityZ = -offsetZ * inverseDistance * 0.028;
            this.steeringTicks = 30;
            return;
        }

        double returnX = Mth.clamp((this.originX - this.x) * 0.0015, -0.006, 0.006);
        double returnY = Mth.clamp((this.originY - this.y) * 0.0020, -0.005, 0.005);
        double returnZ = Mth.clamp((this.originZ - this.z) * 0.0015, -0.006, 0.006);
        this.targetVelocityX = randomBetween(-0.024, 0.024) + returnX;
        this.targetVelocityY = randomBetween(-0.014, 0.014) + returnY;
        this.targetVelocityZ = randomBetween(-0.024, 0.024) + returnZ;
        this.steeringTicks = randomTicks(this.minSteeringTicks, this.maxSteeringTicks);
    }

    private int randomTicks(int a, int b) {
        return Math.min(a, b) + this.random.nextInt(Math.abs(b - a) + 1);
    }

    private double randomBetween(double a, double b) {
        return Math.min(a, b) + this.random.nextDouble() * Math.abs(b - a);
    }

    @Override
    protected int getLightColor(float partialTick) {
        int light = super.getLightColor(partialTick);
        return LightTexture.pack(Math.max(LightTexture.block(light), this.minimumLight), LightTexture.sky(light));
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
