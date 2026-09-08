package com.sange.ambientgogga.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sange.ambientgogga.client.SeaFireCurves;
import com.sange.ambientgogga.client.SeaFireClientConfig;
import com.sange.ambientgogga.client.SeaFireEmission;
import com.sange.ambientgogga.client.SeaFireGeometry;
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
    private final double blinkStep, peakAlpha, minimumGlow;
    private final int track;
    private int pathStep;
    private double driftCos, driftSin, blinkPhase;
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
        blinkStep = between(SeaFireClientConfig.MIN_BLINK_HZ.get(), SeaFireClientConfig.MAX_BLINK_HZ.get())
                * SeaFireCurves.STEPS / 20;
        minimumGlow = SeaFireClientConfig.MIN_GLOW.get();
        minimumLight = SeaFireClientConfig.LIGHT.get();
        double speed = SeaFireClientConfig.SPEED.get() * between(0.4, 1);
        blinkPhase = random.nextDouble() * SeaFireCurves.STEPS;
        double rotation = random.nextDouble() * Math.PI * 2;
        driftCos = Math.cos(rotation) * speed;
        driftSin = Math.sin(rotation) * speed;
        track = random.nextInt(SeaFireCurves.TRACKS);
        pathStep = random.nextInt(SeaFireCurves.STEPS);
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
        float vx = SeaFireCurves.x(track, pathStep), vz = SeaFireCurves.z(track, pathStep);
        pathStep = (pathStep + 1) & (SeaFireCurves.STEPS - 1);
        double nextX = x + vx * driftCos - vz * driftSin;
        double nextZ = z + vx * driftSin + vz * driftCos;
        // Keep all four corners over water, including at diagonals and shore edges.
        if (SeaFireCurves.sameFootprint(x, z, nextX, nextZ, quadSize)
                || fits(level, nextX, waterY, nextZ, quadSize)) {
            setPos(nextX, SeaFireSurface.height(level, Mth.floor(nextX), waterY, Mth.floor(nextZ)), nextZ);
        } else {
            // Rotate the precomputed track at a shore collision without runtime trigonometry.
            double oldCos = driftCos;
            driftCos = -0.453990499739547 * oldCos - 0.891006524188368 * driftSin;
            driftSin = 0.891006524188368 * oldCos - 0.453990499739547 * driftSin;
        }
        double night = SeaFireSpawner.activity(level);
        blinkPhase += blinkStep;
        if (blinkPhase >= SeaFireCurves.STEPS) blinkPhase -= SeaFireCurves.STEPS;
        alpha = (float) (peakAlpha * (minimumGlow + (1 - minimumGlow) * SeaFireCurves.blink(blinkPhase))
                * SeaFireCurves.fade(age, lifetime) * night);
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
        float opacity = Mth.lerp(partialTick, previousAlpha, alpha);
        boolean emissive = SeaFireRenderType.INSTANCE.emissiveBatch();
        float brightness = emissive ? opacity * SeaFireRenderType.INSTANCE.strength() : 1;
        if (opacity <= 0 || (emissive && brightness < 1 / 255F)) return;
        var eye = camera.getPosition();
        float px = (float) (Mth.lerp(partialTick, xo, x) - eye.x);
        double surfaceHeight = Mth.lerp(partialTick, yo, y);
        float lift = SeaFireShaderCompat.useVegetation()
                ? SeaFireVegetationWaves.height(Mth.lerp(partialTick, xo, x), Mth.lerp(partialTick, zo, z)) : 0;
        float py = (float) (SeaFireGeometry.bottom(surfaceHeight, eye.y, quadSize, lift) - eye.y);
        float pz = (float) (Mth.lerp(partialTick, zo, z) - eye.z);
        int light = emissive ? 0 : SeaFireShaderCompat.markLight(getLightColor(partialTick));
        // Native emissive passes may use additive blending and ignore vertex alpha.
        float vertexAlpha = emissive ? 1 : opacity;
        float red = emissive ? SeaFireEmission.scale(rCol, brightness) : rCol;
        float green = emissive ? SeaFireEmission.scale(gCol, brightness) : gCol;
        float blue = emissive ? SeaFireEmission.scale(bCol, brightness) : bCol;
        // Rotate only around world Y. The lower edge stays at the water surface.
        double horizontalDistance = Math.hypot(px, pz);
        float rightX = horizontalDistance > 1e-6 ? (float) (-pz / horizontalDistance) : 1;
        float rightZ = horizontalDistance > 1e-6 ? (float) (px / horizontalDistance) : 0;
        float dx = rightX * quadSize, dz = rightZ * quadSize;
        float top = py + quadSize * 2;
        vertex(buffer, px - dx, py, pz - dz, getU0(), getV1(), red, green, blue, vertexAlpha, light, emissive);
        vertex(buffer, px + dx, py, pz + dz, getU1(), getV1(), red, green, blue, vertexAlpha, light, emissive);
        vertex(buffer, px + dx, top, pz + dz, getU1(), getV0(), red, green, blue, vertexAlpha, light, emissive);
        vertex(buffer, px - dx, top, pz - dz, getU0(), getV0(), red, green, blue, vertexAlpha, light, emissive);
    }

    private void vertex(VertexConsumer buffer, float x, float y, float z, float u, float v,
                        float red, float green, float blue, float opacity, int light, boolean emissive) {
        buffer.addVertex(x, y, z).setUv(u, v).setColor(red, green, blue, opacity);
        if (!emissive) buffer.setLight(light);
    }

    @Override
    public ParticleRenderType getRenderType() { return SeaFireRenderType.INSTANCE; }

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
