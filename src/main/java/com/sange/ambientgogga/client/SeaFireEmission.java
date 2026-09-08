package com.sange.ambientgogga.client;

/** Shared sRGB lookup for smooth linear-light fading, with no per-frame power functions. */
public final class SeaFireEmission {
    private static final byte[] COLORS = new byte[256 * 256];
    static {
        for (int color = 0; color < 256; color++) {
            double srgb = color / 255.0;
            double linear = srgb <= 0.04045 ? srgb / 12.92 : Math.pow((srgb + 0.055) / 1.055, 2.4);
            for (int brightness = 0; brightness < 256; brightness++) {
                double value = linear * brightness / 255.0;
                double encoded = value <= 0.0031308 ? value * 12.92 : 1.055 * Math.pow(value, 1 / 2.4) - 0.055;
                COLORS[color * 256 + brightness] = (byte) Math.round(encoded * 255);
            }
        }
    }

    public static float scale(float color, float brightness) {
        int c = Math.clamp(Math.round(color * 255), 0, 255);
        int b = Math.clamp(Math.round(brightness * 255), 0, 255);
        return (COLORS[c * 256 + b] & 255) / 255F;
    }

    private SeaFireEmission() { }
}
