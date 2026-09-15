package com.sange.ambientgogga.client.compat;

import com.mojang.logging.LogUtils;
import com.sange.ambientgogga.config.ClientConfig;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import net.neoforged.fml.ModList;

/** Optional opt-in protocol for shader packs that apply their own water displacement to Sea Fire. */
public final class SeaFireShaderCompat {
    public static final String MARKER_PATH = "shaders/ambientgogga-sea-fire.properties";
    public static final String PROTOCOL = "ambientgogga.seaFireWaveProtocol=1";
    private static final int LIGHT_MARKER = 0x000F000F;
    private static Api api;
    private static boolean attempted, warned, active, vegetation, emissive, deferredEmissive;
    private static String packName;
    private static long nextProbe;

    public static int markLight(int light) { return active ? light | LIGHT_MARKER : light; }
    public static boolean useVegetation() { return vegetation; }
    public static boolean useEmissive() { return emissive; }
    public static boolean deferredEmissive() { return emissive && deferredEmissive; }

    /** Called once per client tick, never per vertex; file capability checks are throttled. */
    public static void refresh() {
        active = false;
        vegetation = false;
        emissive = false;
        deferredEmissive = false;
        if (!ModList.get().isLoaded("iris")) return;
        try {
            if (!attempted) {
                attempted = true;
                Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
                api = new Api(irisApi.getMethod("getInstance").invoke(null), irisApi.getMethod("isShaderPackInUse"),
                        iris.getMethod("getCurrentPackName"), iris.getMethod("getShaderpacksDirectory"));
            }
            if (api == null || !Boolean.TRUE.equals(api.inUse.invoke(api.instance))) return;
            String name = (String) api.packName.invoke(null);
            if (name == null) return;
            long now = System.nanoTime();
            if (!name.equals(packName) || now >= nextProbe) {
                packName = name;
                nextProbe = now + 2_000_000_000L;
                Path root = ((Path) api.directory.invoke(null)).toAbsolutePath().normalize();
                Path path = root.resolve(name).normalize();
                api.supported = path.startsWith(root) && supports(path);
                api.deferred = path.startsWith(root) && usesDeferredEyes(path);
            }
            active = api.supported && ClientConfig.SEA_FIRE.SHADER_WAVES.get();
            vegetation = !active && ClientConfig.SEA_FIRE.SHADER_WAVES.get() && ClientConfig.SEA_FIRE.VEGETATION_WAVES.get();
            emissive = !active && ClientConfig.SEA_FIRE.SHADER_EMISSIVE.get();
            deferredEmissive = api.deferred;
        } catch (ReflectiveOperationException | IOException | RuntimeException | LinkageError error) {
            if (api != null) api.supported = false;
            if (!warned) {
                warned = true;
                LogUtils.getLogger().warn("Sea Fire shader integration unavailable; using the vanilla water surface.", error);
            }
        }
    }

    public static boolean supports(Path pack) throws IOException {
        if (Files.isDirectory(pack)) {
            Path marker = pack.resolve(MARKER_PATH);
            return Files.isRegularFile(marker) && Files.size(marker) <= 4096 && validMarker(Files.readString(marker));
        }
        if (!Files.isRegularFile(pack)) return false;
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            var entry = zip.getEntry(MARKER_PATH);
            if (entry == null || entry.getSize() > 4096) return false;
            try (var input = zip.getInputStream(entry)) {
                return validMarker(new String(input.readNBytes(4097), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
    }

    private static boolean validMarker(String text) {
        return text.length() <= 4096 && text.lines().anyMatch(line -> line.strip().equals(PROTOCOL));
    }

    /** Photon 1.21.1 writes eye geometry into the opaque G-buffer, not the final color buffer. */
    public static boolean usesDeferredEyes(Path pack) throws IOException {
        String entry = "shaders/world0/gbuffers_spidereyes.fsh";
        String implementation = "shaders/program/gbuffers_all_solid.fsh";
        String eyes, solid;
        if (Files.isDirectory(pack)) {
            eyes = boundedText(pack.resolve(entry));
            solid = boundedText(pack.resolve(implementation));
        } else if (Files.isRegularFile(pack)) {
            try (ZipFile zip = new ZipFile(pack.toFile())) {
                eyes = boundedText(zip, entry);
                solid = boundedText(zip, implementation);
            }
        } else return false;
        // Match the actual shader layout, so renamed Photon archives work as well.
        return eyes.contains("PROGRAM_GBUFFERS_SPIDEREYES")
                && eyes.contains("\"/program/gbuffers_all_solid.fsh\"")
                && solid.contains("Photon Shader by SixthSurge")
                && solid.contains("gbuffer_data_0") && solid.contains("pack_unorm_2x8");
    }

    private static String boundedText(Path path) throws IOException {
        return Files.isRegularFile(path) && Files.size(path) <= 131072 ? Files.readString(path) : "";
    }

    private static String boundedText(ZipFile zip, String name) throws IOException {
        var entry = zip.getEntry(name);
        if (entry == null || entry.getSize() > 131072) return "";
        try (var input = zip.getInputStream(entry)) {
            byte[] bytes = input.readNBytes(131073);
            return bytes.length > 131072 ? "" : new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    public static void reset() { active = false; vegetation = false; emissive = false; deferredEmissive = false; packName = null; nextProbe = 0; }

    private static final class Api {
        final Object instance;
        final Method inUse, packName, directory;
        boolean supported, deferred;
        Api(Object instance, Method inUse, Method packName, Method directory) {
            this.instance = instance; this.inUse = inUse; this.packName = packName; this.directory = directory;
        }
    }

    private SeaFireShaderCompat() { }
}
