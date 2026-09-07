package com.sange.ambientgogga.client.compat;

import com.mojang.logging.LogUtils;
import com.teamtea.eclipticseasons.api.EclipticSeasonsApi;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/** The nested bridge is resolved only after the optional mod is known to be installed. */
public final class EclipticSeasonsCompat {
    private static boolean warned;

    private EclipticSeasonsCompat() { }

    public static boolean isInstalled() { return ModList.get().isLoaded("eclipticseasons"); }

    public static String currentTerm(Level level) {
        if (!isInstalled()) return null;
        try {
            return Bridge.currentTerm(level);
        } catch (LinkageError | RuntimeException error) {
            if (!warned) {
                warned = true;
                LogUtils.getLogger().warn("Unable to read Ecliptic Seasons' calendar; seasonal firefly spawning waits for a valid term.", error);
            }
            return null;
        }
    }

    private static final class Bridge {
        private static String currentTerm(Level level) {
            // These two public API methods already existed in the early 1.21.1
            // releases. Avoid newer climate APIs and assumptions about enum order.
            var term = EclipticSeasonsApi.getInstance().getSolarTerm(level);
            return term == null ? null : term.name();
        }
    }
}
