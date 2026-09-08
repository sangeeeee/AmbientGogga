package com.sange.ambientgogga.client;

/** Keeps the upright quad on the viewer's side of the water's depth surface. */
public final class SeaFireGeometry {
    public static double bottom(double spawnHeight, double eyeHeight, float halfSize, float lift) {
        // Surface samples include a 0.002-block offset above the water mesh.
        double surface = spawnHeight - 0.002;
        return eyeHeight < surface
                ? surface - 0.002 - halfSize * 2 - lift
                : spawnHeight + lift;
    }

    private SeaFireGeometry() { }
}
