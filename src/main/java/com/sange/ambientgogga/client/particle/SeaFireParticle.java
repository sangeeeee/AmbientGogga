package com.sange.ambientgogga.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sange.ambientgogga.client.FireflyTiming;
import com.sange.ambientgogga.client.SeaFireClientConfig;
import com.sange.ambientgogga.client.SeaFireSpawner;
import com.sange.ambientgogga.client.SeaFireSurface;
import com.sange.ambientgogga.client.compat.SeaFireShaderCompat;
import com.sange.ambientgogga.client.compat.SeaFireVegetationWaves;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** Upright squares anchored to source water, facing the camera horizontally without pitching. */
public final class SeaFireParticle extends TextureSheetParticle {
    private static final WeakHashMap<SeaFireParticle, Boolean> LIVE = new WeakHashMap<>();
    private static WeakReference<ClientLevel> trackedLevel = new WeakReference<>(null);
    private final int waterY, minimumLight;
    private final double speed, phase, blinkHz, peakAlpha, minimumGlow;
    private double heading;
    private float previousAlpha;
    public static boolean hasLiveParticles() { return !LIVE.isEmpty(); }

    public static boolean hasCapacity(ClientLevel level) {
        if (trackedLevel.get() != level) {
            LIVE.clear();
            trackedLevel = new WeakReference<>(level);
        }
        return LIVE.size() < SeaFireClientConfig.MAX_PARTICLES.get();
    }

    private SeaFireParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        waterY = Mth.floor(y - 0.01);
        int a = SeaFireClientConfig.MIN_LIFETIME.get(), b = SeaFireClientConfig.MAX_LIFETIME.get();
        lifetime = Math.min(a, b) + random.nextInt(Math.abs(b - a) + 1);
        double small = Math.min(SeaFireClientConfig.MIN_SIZE.get(), SeaFireClientConfig.MAX_SIZE.get());
        double large = Math.max(SeaFireClientConfig.MIN_SIZE.get(), SeaFireClientConfig.MAX_SIZE.get());
        double sizeSample = random.nextDouble();
        quadSize = (float) (small + sizeSample * sizeSample * (large - small));
        peakAlpha = between(SeaFireClientConfig.MIN_ALPHA.get(), SeaFireClientConfig.MAX_ALPHA.get());
        blinkHz = between(SeaFireClientConfig.MIN_BLINK_HZ.get(), SeaFireClientConfig.MAX_BLINK_HZ.get());
        minimumGlow = SeaFireClientConfig.MIN_GLOW.get();
        minimumLight = SeaFireClientConfig.LIGHT.get();
        speed = SeaFireClientConfig.SPEED.get() * between(0.4, 1);
        phase = random.nextDouble() * Math.PI * 2;
        heading = random.nextDouble() * Math.PI * 2;
        // Deep blue, electric blue and cyan; every color remains cooler than land fireflies.
        float tint = random.nextFloat();
        setColor(0.06F + tint * 0.10F, 0.27F + tint * 0.47F, 1.0F);
        hasPhysics = false;
        gravity = 0;
        alpha = 0;
        pickSprite(sprites);
        LIVE.put(this, Boolean.TRUE);
    }

    private double between(double a, double b) {
        return Math.min(a, b) + random.nextDouble() * Math.abs(b - a);
    }

    @Override
    public void tick() {
        xo = x; yo = y; zo = z;
        previousAlpha = alpha;
        if (age++ >= lifetime || !fits(level, x, waterY, z, quadSize)) {
            remove();
            return;
        }
        heading += Math.sin(phase + age * 0.035) * 0.045;
        double nextX = x + Math.cos(heading) * speed;
        double nextZ = z + Math.sin(heading) * speed;
        // Keep all four corners over water, including at diagonals and shore edges.
        if (fits(level, nextX, waterY, nextZ, quadSize)) {
            setPos(nextX, SeaFireSurface.height(level, Mth.floor(nextX), waterY, Mth.floor(nextZ)), nextZ);
        } else {
            heading += Math.PI * 0.65;
        }
        double night = SeaFireSpawner.activity(level);
        alpha = (float) (FireflyTiming.glow(age, lifetime, Math.min(20, lifetime), Math.min(40, lifetime),
                peakAlpha, minimumGlow, phase, blinkHz) * night);
    }

    private static boolean fits(ClientLevel level, double x, int y, double z, double size) {
        int minX = Mth.floor(x - size), maxX = Mth.floor(x + size);
        int minZ = Mth.floor(z - size), maxZ = Mth.floor(z + size);
        if (minX == maxX && minZ == maxZ) {
            return SeaFireSurface.isSurface(level, minX, y, minZ);
        }
        return SeaFireSurface.isSurface(level, minX, y, minZ)
                && SeaFireSurface.isSurface(level, maxX, y, minZ)
                && SeaFireSurface.isSurface(level, maxX, y, maxZ)
                && SeaFireSurface.isSurface(level, minX, y, maxZ);
    }

    @Override
    public void remove() {
        super.remove();
        LIVE.remove(this);
    }

    @Override
    protected int getLightColor(float partialTick) {
        int light = SeaFireSurface.light(level, Mth.floor(x), waterY, Mth.floor(z));
        return LightTexture.pack(Math.max(minimumLight, LightTexture.block(light)), LightTexture.sky(light));
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        var eye = camera.getPosition();
        float px = (float) (Mth.lerp(partialTick, xo, x) - eye.x);
        float py = (float) (Mth.lerp(partialTick, yo, y) - eye.y);
        if (SeaFireShaderCompat.useVegetation()) {
            py += SeaFireVegetationWaves.height(Mth.lerp(partialTick, xo, x), Mth.lerp(partialTick, zo, z));
        }
        float pz = (float) (Mth.lerp(partialTick, zo, z) - eye.z);
        int light = SeaFireShaderCompat.markLight(getLightColor(partialTick));
        float opacity = Mth.lerp(partialTick, previousAlpha, alpha);
        // Rotate only around world Y. The lower edge stays at the water surface.
        double horizontalDistance = Math.hypot(px, pz);
        float rightX = horizontalDistance > 1e-6 ? (float) (-pz / horizontalDistance) : 1;
        float rightZ = horizontalDistance > 1e-6 ? (float) (px / horizontalDistance) : 0;
        float dx = rightX * quadSize, dz = rightZ * quadSize;
        float top = py + quadSize * 2;
        buffer.addVertex(px - dx, py, pz - dz).setUv(getU0(), getV1()).setColor(rCol, gCol, bCol, opacity).setLight(light);
        buffer.addVertex(px + dx, py, pz + dz).setUv(getU1(), getV1()).setColor(rCol, gCol, bCol, opacity).setLight(light);
        buffer.addVertex(px + dx, top, pz + dz).setUv(getU1(), getV0()).setColor(rCol, gCol, bCol, opacity).setLight(light);
        buffer.addVertex(px - dx, top, pz - dz).setUv(getU0(), getV0()).setColor(rCol, gCol, bCol, opacity).setLight(light);
    }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            if (!hasCapacity(level)) return null;
            var pos = SeaFireSurface.find(level, Mth.floor(x), Mth.floor(z));
            double size = Math.max(SeaFireClientConfig.MIN_SIZE.get(), SeaFireClientConfig.MAX_SIZE.get());
            if (pos == null || Math.abs(y - SeaFireSurface.height(level, pos)) > 1 || !fits(level, x, pos.getY(), z, size)) return null;
            return new SeaFireParticle(level, x, SeaFireSurface.height(level, pos), z, sprites);
        }
    }
}
