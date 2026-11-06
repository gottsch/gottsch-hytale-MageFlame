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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers which summoned flames belong to which player and which "slot" each one holds.
 * <p>
 * A slot is a place beside the player: every slot has its own NPC role ({@code Mage_Flame_1}..{@code _4}) that floats
 * at a different distance and height, so several flames settle in different spots instead of fighting over one.
 * Entries are in memory only: after a server restart, flames that survive in the world are no longer tracked.
 *
 * @author Mark Gottschling on 11/06/2026
 */
public final class SummonTracker {
    /** Number of slot roles that exist in Server/NPC/Roles/MageFlame (Mage_Flame_1 .. Mage_Flame_N). */
    public static final int SLOT_COUNT = 4;

    private static final String ROLE_PREFIX = "Mage_Flame_";

    private static final Map<UUID, List<Flame>> FLAMES = new ConcurrentHashMap<>();

    /** A tracked flame: the slot it holds (1-based) and its entity. */
    public record Flame(int slot, Ref<EntityStore> ref) {
    }

    private SummonTracker() {
    }

    /**
     * The player's tracked flames, oldest first, with entities that no longer exist removed.
     * Callers run on the world thread, so the returned list is not shared across threads.
     */
    public static List<Flame> flamesOf(UUID player) {
        List<Flame> flames = FLAMES.computeIfAbsent(player, id -> new ArrayList<>());
        flames.removeIf(flame -> !flame.ref().isValid());
        return flames;
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

    /** The NPC role id for a slot, for example slot 2 -> {@code Mage_Flame_2}. */
    public static String roleFor(int slot) {
        return ROLE_PREFIX + slot;
    }

    /** True for any of the slot roles. */
    public static boolean isFlameRole(String roleName) {
        return roleName != null && roleName.startsWith(ROLE_PREFIX);
    }

    /** Forget every tracked flame (used after a dispel). */
    public static void clear() {
        FLAMES.clear();
    }
}
