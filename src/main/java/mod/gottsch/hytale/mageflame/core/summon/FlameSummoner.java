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
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import mod.gottsch.hytale.mageflame.MageFlame;
import org.joml.Vector3d;

import java.util.List;

/**
 * Conjures a flame of a given type for a player: picks the lowest free slot, replaces the player's oldest flame if they are at the
 * limit, spawns the type's slot role ({@code <FlameType>_Slot<N>}) above them and tracks it. Shared by {@code /mageflame summon}
 * and the scroll items so both behave identically. Must run on the world thread.
 * <p>
 * A flame type is only a name: it needs {@link SummonTracker#SLOT_COUNT} slot roles in the asset pack and nothing in Java.
 * Types share the player's limit and slots.
 *
 * @author Mark Gottschling on 11/06/2026
 */
public final class FlameSummoner {
    /** The flame type used when none is given. */
    public static final String DEFAULT_FLAME_TYPE = "Mage_Flame";

    /** What happened, and the message to show the player. */
    public record Result(boolean success, String message) {
    }

    private FlameSummoner() {
    }

    public static Result summon(Store<EntityStore> store, Ref<EntityStore> playerEntity, PlayerRef playerRef, World world,
                                String flameType) {
        TransformComponent transform = store.getComponent(playerEntity, TransformComponent.getComponentType());
        if (transform == null) {
            return new Result(false, "Could not find your position.");
        }
        if (flameType == null || flameType.isBlank()) {
            flameType = DEFAULT_FLAME_TYPE;
        }

        // enforce the per-player limit (never more than there are slot roles): summoning past it replaces the oldest flame
        int max = Math.clamp(MageFlame.getConfig().get().getMaxSummonedEntitiesPerPlayer(), 1, SummonTracker.SLOT_COUNT);
        List<SummonTracker.Flame> mine = SummonTracker.flamesOf(playerRef.getUuid());
        boolean replaced = false;
        while (mine.size() >= max) {
            SummonTracker.Flame oldest = mine.remove(0);
            if (oldest.ref().isValid()) {
                store.removeEntity(oldest.ref(), RemoveReason.REMOVE);
            }
            replaced = true;
        }

        // each flame takes the lowest free slot; the slot's role decides where it floats beside the player
        int slot = SummonTracker.freeSlot(mine);
        String role = SummonTracker.roleFor(flameType, slot);

        // just above the player's head height
        Vector3d spawnPos = new Vector3d(transform.getPosition()).add(0, 2, 0);
        var spawned = NPCPlugin.get().spawnNPC(store, role, null, spawnPos, Rotation3f.ZERO);
        if (spawned == null) {
            return new Result(false, "Could not spawn " + role + " (is the role loaded?).");
        }
        mine.add(new SummonTracker.Flame(slot, flameType, spawned.left(), world, System.currentTimeMillis(), playerRef,
                new SummonTracker.Watch()));
        return new Result(true, replaced ? "Your flame is renewed." : "A flame appears.");
    }
}
