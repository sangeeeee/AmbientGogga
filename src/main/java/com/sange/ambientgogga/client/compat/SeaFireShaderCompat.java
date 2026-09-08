package com.sange.ambientgogga.client.compat;

import com.mojang.logging.LogUtils;
import com.sange.ambientgogga.client.SeaFireClientConfig;
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
    private static boolean attempted, warned, active, vegetation;
    private static String packName;
    private static long nextProbe;

    public static int markLight(int light) { return active ? light | LIGHT_MARKER : light; }
    public static boolean useVegetation() { return vegetation; }

    /** Called once per client tick, never per vertex; file capability checks are throttled. */
    public static void refresh() {
        active = false;
        vegetation = false;
        if (!SeaFireClientConfig.SHADER_WAVES.get() || !ModList.get().isLoaded("iris")) return;
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
            }
            active = api.supported;
            vegetation = !active && SeaFireClientConfig.VEGETATION_WAVES.get();
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

    public static void reset() { active = false; vegetation = false; packName = null; nextProbe = 0; }

    private static final class Api {
        final Object instance;
        final Method inUse, packName, directory;
        boolean supported;
        Api(Object instance, Method inUse, Method packName, Method directory) {
            this.instance = instance; this.inUse = inUse; this.packName = packName; this.directory = directory;
        }
    }

    private SeaFireShaderCompat() { }
}
