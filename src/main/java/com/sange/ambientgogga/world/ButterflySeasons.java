package com.sange.ambientgogga.world;

import com.sange.ambientgogga.config.ServerConfig;

import com.sange.ambientgogga.compat.EclipticSeasonsCompat;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;

/** Reuse the calendar result across spawn attempts in the same dimension and game tick. */
public final class ButterflySeasons {
    private record Calendar(long tick, String term) { }
    private static final WeakHashMap<ServerLevel, Calendar> CALENDARS = new WeakHashMap<>();

    public static boolean allowsNaturalSpawn(ServerLevel level) {
        ButterflySeasonMode mode = ServerConfig.SEASON.get();
        if (mode == ButterflySeasonMode.ALL_YEAR || !EclipticSeasonsCompat.isInstalled()) return true;
        long tick = level.getGameTime();
        Calendar calendar = CALENDARS.get(level);
        if (calendar == null || calendar.tick != tick) {
            calendar = new Calendar(tick, EclipticSeasonsCompat.currentTerm(level));
            CALENDARS.put(level, calendar);
        }
        return mode.allows(true, calendar.term);
    }

    private ButterflySeasons() { }
}
