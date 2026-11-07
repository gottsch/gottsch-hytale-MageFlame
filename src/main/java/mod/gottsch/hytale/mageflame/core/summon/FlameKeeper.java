/*
 * This file is part of  Mage Flame.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
 *
 * Mage Flame is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mage Flame is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Mage Flame.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.hytale.mageflame.core.summon;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mod.gottsch.hytale.mageflame.MageFlame;
import mod.gottsch.hytale.mageflame.core.config.MageFlameConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Takes a player's flames out of the world when they leave and, when {@code PersistFlames} is on, conjures them again when they
 * return. The flames are never kept alive across a logout or restart: a flame is an entity whose reference does not survive,
 * so it is removed and a fresh one is spawned from what was saved (type and how long it had already burned). That leaves
 * no orphans and nothing to match up. The burn time is paused while the player is away.
 * <p>
 * Saved flames live in memory and in {@code SavedFlames.txt} in the plugin's data folder (one line per flame:
 * player id, flame type, seconds already burned), so they also survive a server restart. A crash loses them, and the
 * flames it leaves in the world are removed by {@link SummonTracker#sweepOrphans(World)}.
 *
 * @author Mark Gottschling on 11/07/2026
 */
public final class FlameKeeper {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** A flame waiting for its owner to come back. */
    private record Saved(String type, long elapsedSeconds) {
    }

    private static final Map<UUID, List<Saved>> SAVED = new ConcurrentHashMap<>();
    private static Path file;

    private FlameKeeper() {
    }

    /** Reads the saved flames. Call once from the plugin's setup. */
    public static void init(Path dataDirectory) {
        file = dataDirectory.resolve("SavedFlames.txt");
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] parts = line.split("\t");
                if (parts.length != 3) {
                    continue;
                }
                try {
                    SAVED.computeIfAbsent(UUID.fromString(parts[0]), id -> new ArrayList<>())
                            .add(new Saved(parts[1], Long.parseLong(parts[2])));
                } catch (IllegalArgumentException e) {
                    LOGGER.atWarning().log("Ignoring a bad line in %s: %s", file.getFileName(), line);
                }
            }
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Could not read %s", file);
        }
    }

    /** A player left: remove their flames from the world, and keep them for next time if persistence is on. */
    public static void onDisconnect(PlayerRef player) {
        List<SummonTracker.Flame> flames = SummonTracker.takeAll(player.getUuid());
        if (flames.isEmpty()) {
            return;
        }
        if (persist()) {
            remember(player.getUuid(), flames);
            write();
        }
        for (SummonTracker.Flame flame : flames) {
            try {
                flame.world().execute(() -> {
                    if (flame.ref().isValid()) {
                        flame.ref().getStore().removeEntity(flame.ref(), RemoveReason.REMOVE);
                    }
                });
            } catch (IllegalThreadStateException e) {
                // the world is gone: so is the flame
            }
        }
    }

    /** A player is in the world: conjure the flames they had when they left. */
    public static void onReady(PlayerReadyEvent event) {
        Ref<EntityStore> ref = event.getPlayerRef();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        List<Saved> saved = SAVED.remove(playerRef.getUuid());
        if (saved == null) {
            return;
        }
        write();
        World world = store.getExternalData().getWorld();
        MageFlameConfig config = MageFlame.getConfig().get();
        world.execute(() -> {
            long now = System.currentTimeMillis();
            for (Saved flame : saved) {
                int lifespan = config.lifespanFor(flame.type());
                if (lifespan > 0 && flame.elapsedSeconds() >= lifespan) {
                    continue;   // it would have burned out already (the lifespan was shortened while the player was away)
                }
                FlameSummoner.Result result = FlameSummoner.restore(store, ref, playerRef, world, flame.type(),
                        now - flame.elapsedSeconds() * 1000L);
                if (!result.success()) {
                    LOGGER.atWarning().log("Could not restore a %s for %s: %s", flame.type(), playerRef.getUsername(), result.message());
                }
            }
        });
    }

    /** The plugin is shutting down: save every online player's flames so they come back after the restart. */
    public static void saveAll() {
        if (persist()) {
            for (Map.Entry<UUID, List<SummonTracker.Flame>> entry : SummonTracker.allFlames().entrySet()) {
                remember(entry.getKey(), entry.getValue());
            }
        }
        write();
    }

    private static void remember(UUID player, List<SummonTracker.Flame> flames) {
        long now = System.currentTimeMillis();
        List<Saved> list = new ArrayList<>();
        for (SummonTracker.Flame flame : flames) {
            if (flame.ref().isValid()) {
                list.add(new Saved(flame.type(), Math.max(0, (now - flame.spawnedAtMillis()) / 1000)));
            }
        }
        if (list.isEmpty()) {
            SAVED.remove(player);
        } else {
            SAVED.put(player, list);
        }
    }

    private static boolean persist() {
        return MageFlame.getConfig().get().isPersistFlames();
    }

    private static synchronized void write() {
        if (file == null) {
            return;
        }
        List<String> lines = new ArrayList<>();
        if (persist()) {
            SAVED.forEach((id, list) -> list.forEach(s -> lines.add(id + "\t" + s.type() + "\t" + s.elapsedSeconds())));
        }
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Could not write %s", file);
        }
    }
}
