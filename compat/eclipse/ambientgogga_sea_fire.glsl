// Sea Fire protocol v1. Called only for marked Ambient Gogga vertices.
// This adapter matches Eclipse's LARGE_WAVE_DISPLACEMENT water mesh, not Physics Mod oceans.
#if defined PARTICLES && defined OVERWORLD_SHADER
uniform sampler2D noisetex;
#ifdef DISTANT_HORIZONS
uniform float far;
#endif

float agSeaFireCornerOffset(vec3 relativeCorner) {
    vec3 viewCorner = mat3(gbufferModelView) * relativeCorner + gbufferModelView[3].xyz;
    vec3 playerCorner = mat3(gbufferModelViewInverse) * viewCorner;
#ifdef DISTANT_HORIZONS
    float range = pow(1.0 - pow(1.0 - clamp(1.0 - length(playerCorner) / far, 0.0, 1.0), 3.0), 3.0);
#else
    float range = min(1.0 + pow(length(playerCorner) / 256.0, 2.0), 256.0);
#endif
    vec2 noiseUv = (relativeCorner.xz + cameraPosition.xz + frameTimeCounter * WATER_WAVE_SPEED) / 125.0;
    float wave = pow(1.0 - textureLod(noisetex, noiseUv, 0.0).r, 5.0)
        * min(WATER_WAVE_STRENGTH, 1.0) * range;
    return wave * 0.6 - 0.5;
}

float agSeaFireWaterOffset(vec3 relativePosition) {
#if defined LARGE_WAVE_DISPLACEMENT && !defined PHYSICS_OCEAN && !defined PHYSICSMOD_OCEAN_SHADER && !defined PHYSICSMOD_OCEAN_SHADER_V2
    vec3 worldPosition = relativePosition + cameraPosition;
    vec2 base = floor(worldPosition.xz);
    vec2 fraction = fract(worldPosition.xz);
    // Source water is 8/9 high. Remove the particle's epsilon and upright height.
    float surfaceY = floor(worldPosition.y - (8.0 / 9.0 + 0.002) + 0.5) + 8.0 / 9.0;
    vec3 p00 = vec3(base.x, surfaceY, base.y) - cameraPosition;
    float h00 = agSeaFireCornerOffset(p00);
    float h11 = agSeaFireCornerOffset(p00 + vec3(1.0, 0.0, 1.0));
    // Match the two actual water triangles instead of sampling an unrelated smooth height at the particle.
    if (fraction.y >= fraction.x) {
        float h01 = agSeaFireCornerOffset(p00 + vec3(0.0, 0.0, 1.0));
        return h00 * (1.0 - fraction.y) + h01 * (fraction.y - fraction.x) + h11 * fraction.x;
    }
    float h10 = agSeaFireCornerOffset(p00 + vec3(1.0, 0.0, 0.0));
    return h00 * (1.0 - fraction.x) + h10 * (fraction.x - fraction.y) + h11 * fraction.y;
#else
    return 0.0;
#endif
}
#endif
