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
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.joml.Vector3d;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

/**
 * Remembers which summoned flames belong to which player, which "slot" each one holds, and when it was summoned.
 * <p>
 * A slot is a place beside the player: every flame type has one NPC role per slot ({@code <Type>_Slot1}..{@code _Slot4}) that floats
 * at a different distance and height, so several flames settle in different spots instead of fighting over one.
 * <p>
 * The plugin, not the NPC role, owns the lifespan: {@link #expire(int)} runs about once a second on a timer thread and
 * removes flames that have lived longer than {@code MageFlameLifespanSeconds}. The same spawn time lets a HUD show how long
 * each flame has left ({@link #remainingSeconds(Flame, int)}).
 * <p>
 * Entries are in memory only: after a server restart, flames that survive in the world are no longer tracked, so
 * {@link #sweepOrphans(World)} removes them.
 *
 * @author Mark Gottschling on 11/06/2026
 */
public final class SummonTracker {
    /** Number of slots. Every flame type must provide this many slot roles: {@code <Type>_Slot1} .. {@code <Type>_Slot<N>}. */
    public static final int SLOT_COUNT = 4;

    /** Returned by {@link #remainingSeconds} for a flame type that never expires. */
    public static final int NO_EXPIRY = -1;

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** A slot role id is {@code <FlameType>_Slot<N>}, for example {@code Mage_Flame_Slot2}. */
    private static final String SLOT_INFIX = "_Slot";
    private static final Pattern SLOT_ROLE = Pattern.compile("^.+_Slot[1-9][0-9]*$");

    // lists are copy-on-write: commands change them on a world thread while the expiry timer reads and prunes them
    private static final Map<UUID, List<Flame>> FLAMES = new ConcurrentHashMap<>();

    /**
     * A tracked flame.
     *
     * @param slot            the slot it holds (1-based)
     * @param type            the flame type (the part of its role id before {@code _Slot<N>}), for example {@code Mage_Flame}
     * @param ref             its entity
     * @param world           the world it lives in, needed to remove it on that world's thread
     * @param spawnedAtMillis when it was summoned ({@link System#currentTimeMillis()})
     */
    public record Flame(int slot, String type, Ref<EntityStore> ref, World world, long spawnedAtMillis, PlayerRef owner,
                        Watch watch) {
    }

    /** What the stuck watchdog remembers about a flame between checks. Only touched on the flame's world thread. */
    public static final class Watch {
        private Vector3d lastPosition;
        private int stillChecks;
    }

    /**
     * How far from its owner each slot's flame normally settles (slot 1 first). Keep in step with FollowStopDistance in every
     * flame type's slot roles (Server/NPC/Roles/MageFlame/Mage_Flame_SlotN.json): all types share the same slot distances.
     */
    private static final double[] SLOT_STOP_DISTANCE = {3.0, 4.5, 6.0, 7.5};
    /** A flame this much farther than its slot distance, and not moving, counts as pinned. */
    private static final double STUCK_EXTRA_DISTANCE = 1.5;
    /** A flame that moved less than this between two checks counts as not moving. */
    private static final double STUCK_MIN_MOVE = 0.25;
    /** Consecutive "pinned" checks (one per second) before the flame is moved. */
    private static final int STUCK_CHECKS = 2;

    private SummonTracker() {
    }

    /** The player's tracked flames, oldest first, with entities that no longer exist removed. */
    public static List<Flame> flamesOf(UUID player) {
        List<Flame> flames = FLAMES.computeIfAbsent(player, id -> new CopyOnWriteArrayList<>());
        flames.removeIf(flame -> !flame.ref().isValid());
        return flames;
    }

    /** Stops tracking all of the player's flames and returns them (the entities are left for the caller to remove). */
    public static List<Flame> takeAll(UUID player) {
        List<Flame> flames = FLAMES.remove(player);
        if (flames == null) {
            return List.of();
        }
        flames.removeIf(flame -> !flame.ref().isValid());
        return flames;
    }

    /** A copy of every tracked player's flames. */
    public static Map<UUID, List<Flame>> allFlames() {
        Map<UUID, List<Flame>> copy = new java.util.HashMap<>();
        FLAMES.forEach((id, flames) -> copy.put(id, List.copyOf(flames)));
        return copy;
    }

    /** The lowest slot number (1-based) that none of the given flames holds, or -1 if every slot is taken. */
    public static int freeSlot(List<Flame> flames) {
        for (int slot = 1; slot <= SLOT_COUNT; slot++) {
            final int candidate = slot;
            if (flames.stream().noneMatch(flame -> flame.slot() == candidate)) {
                return slot;
            }
        }
        return -1;
    }

    /** The NPC role id for a flame type and slot, for example {@code Mage_Flame}, slot 2 -> {@code Mage_Flame_Slot2}. */
    public static String roleFor(String flameType, int slot) {
        return flameType + SLOT_INFIX + slot;
    }

    /**
     * True for the slot role of ANY flame type ({@code <Type>_Slot<N>}), so flames are recognised without the Java knowing the
     * types, including ones left in the world from before a restart.
     */
    public static boolean isFlameRole(String roleName) {
        return roleName != null && SLOT_ROLE.matcher(roleName).matches();
    }

    /**
     * Whole seconds a flame has left, never below zero. {@code lifespanSeconds} of 0 or less means the flame never expires, and
     * the result is then {@link #NO_EXPIRY}.
     */
    public static int remainingSeconds(Flame flame, int lifespanSeconds) {
        if (lifespanSeconds <= 0) {
            return NO_EXPIRY;
        }
        long elapsedMillis = System.currentTimeMillis() - flame.spawnedAtMillis();
        long remainingMillis = lifespanSeconds * 1000L - elapsedMillis;
        return (int) Math.max(0, (remainingMillis + 999) / 1000);
    }

    /**
     * Removes a flame through the world it lives in (a flame left behind in another world after a world change cannot be removed through
     * the current world's store). Safe from any thread; does nothing if that world is gone.
     */
    public static void removeFromItsWorld(Flame flame) {
        try {
            flame.world().execute(() -> {
                if (flame.ref().isValid()) {
                    flame.ref().getStore().removeEntity(flame.ref(), RemoveReason.REMOVE);
                }
            });
        } catch (IllegalThreadStateException e) {
            // the world is gone (crashed or shut down): its flames are gone with it
        }
    }

    /**
     * Removes every tracked flame that has outlived ITS type's lifespan. Safe to call from any thread: the removal itself is
     * handed to the flame's world so it runs on the world thread.
     *
     * @param lifespanOf lifespan in seconds for a flame type; 0 or less means that type never expires
     */
    public static void expire(ToIntFunction<String> lifespanOf) {
        for (List<Flame> flames : FLAMES.values()) {
            for (Flame flame : flames) {
                int remaining = remainingSeconds(flame, lifespanOf.applyAsInt(flame.type()));
                if (!flame.ref().isValid()) {
                    flames.remove(flame);
                } else if (remaining != NO_EXPIRY && remaining <= 0) {
                    flames.remove(flame);
                    try {
                        flame.world().execute(() -> {
                            if (flame.ref().isValid()) {
                                flame.ref().getStore().removeEntity(flame.ref(), RemoveReason.REMOVE);
                            }
                        });
                    } catch (IllegalThreadStateException e) {
                        // the world is gone (crashed or shut down): its flames are gone with it
                    }
                }
            }
        }
    }

    /**
     * Removes flame entities in this world that the tracker does not know. They are leftovers: a server restart saves the flames
     * in the world but forgets who owns them, and a hot reload of a flame's asset re-creates the entity under a new reference.
     * Untracked, they would escape the summon limit, the HUD and the lifespan for good. Must run on the world's thread.
     *
     * @return how many were removed
     */
    public static int sweepOrphans(World world) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        Set<Integer> known = new HashSet<>();
        for (List<Flame> flames : FLAMES.values()) {
            for (Flame flame : flames) {
                if (flame.world() == world && flame.ref().isValid()) {
                    known.add(flame.ref().getIndex());
                }
            }
        }
        AtomicInteger removed = new AtomicInteger();
        store.forEachChunk(NPCEntity.getComponentType(), (chunk, commandBuffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                NPCEntity npc = chunk.getComponent(i, NPCEntity.getComponentType());
                if (npc != null && isFlameRole(npc.getRoleName()) && !known.contains(chunk.getReferenceTo(i).getIndex())) {
                    commandBuffer.removeEntity(chunk.getReferenceTo(i), RemoveReason.REMOVE);
                    removed.incrementAndGet();
                }
            }
        });
        if (removed.get() > 0) {
            LOGGER.atInfo().log("Removed %d untracked Mage Flame(s) left over from before a restart or reload", removed.get());
        }
        return removed.get();
    }

    /** {@link #sweepOrphans(World)} in every loaded world, each on its own thread. Safe to call from any thread. */
    public static void sweepAllWorlds() {
        for (World world : Universe.get().getWorlds().values()) {
            try {
                if (world.isAlive()) {
                    world.execute(() -> sweepOrphans(world));
                }
            } catch (IllegalThreadStateException e) {
                // the world is shutting down
            }
        }
    }

    /**
     * Frees flames that are pinned against terrain. The NPC's own steering can wedge a flame into a corner of the rocks
     * while its owner is out in the open, and a fixed teleport distance cannot catch that because the flame is often still
     * fairly close. Safe to call from any thread: each check runs on the flame's world thread.
     */
    public static void watchdog() {
        for (List<Flame> flames : FLAMES.values()) {
            for (Flame flame : flames) {
                try {
                    flame.world().execute(() -> checkStuck(flame));
                } catch (IllegalThreadStateException e) {
                    // the world is gone (crashed or shut down): forget its flames instead of failing every second
                    flames.remove(flame);
                }
            }
        }
    }

    private static void checkStuck(Flame flame) {
        if (!flame.ref().isValid() || !flame.owner().isValid()) {
            return;
        }
        Store<EntityStore> store = flame.ref().getStore();
        TransformComponent transform = store.getComponent(flame.ref(), TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        Vector3d position = new Vector3d(transform.getPosition());
        Vector3d ownerPosition = new Vector3d(flame.owner().getTransform().getPosition());

        Watch watch = flame.watch();
        double moved = watch.lastPosition == null ? Double.MAX_VALUE : position.distance(watch.lastPosition);
        watch.lastPosition = position;

        double slotDistance = SLOT_STOP_DISTANCE[Math.clamp(flame.slot() - 1, 0, SLOT_STOP_DISTANCE.length - 1)];
        boolean farFromOwner = position.distance(ownerPosition) > slotDistance + STUCK_EXTRA_DISTANCE;
        watch.stillChecks = (farFromOwner && moved < STUCK_MIN_MOVE) ? watch.stillChecks + 1 : 0;
        if (watch.stillChecks < STUCK_CHECKS) {
            return;
        }

        // pinned: put it beside the owner, each slot on its own side
        watch.stillChecks = 0;
        LOGGER.atInfo().log("Mage Flame slot %d looked pinned (%.1f blocks from its owner, %.1f above them) "
                        + "flame at %.1f %.1f %.1f, owner at %.1f %.1f %.1f, summoned %d s ago; moving it next to them",
                flame.slot(), position.distance(ownerPosition), position.y - ownerPosition.y,
                position.x, position.y, position.z, ownerPosition.x, ownerPosition.y, ownerPosition.z,
                (System.currentTimeMillis() - flame.spawnedAtMillis()) / 1000);
        double angle = Math.toRadians(90.0 * flame.slot());
        Vector3d target = new Vector3d(ownerPosition).add(Math.cos(angle) * 2.5, 1.5, Math.sin(angle) * 2.5);
        store.putComponent(flame.ref(), Teleport.getComponentType(), Teleport.createExact(target, Rotation3f.ZERO));
    }

    /** Forget every tracked flame (used after a dispel). */
    public static void clear() {
        FLAMES.clear();
    }
}
