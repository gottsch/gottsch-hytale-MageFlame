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
package mod.gottsch.hytale.mageflame.core.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import mod.gottsch.hytale.mageflame.MageFlame;
import mod.gottsch.hytale.mageflame.core.summon.SummonTracker;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * /mageflame summon - conjures a Mage Flame next to the player, in the lowest free slot.
 * The slot's NPC role ({@code Mage_Flame_1}..) makes the nearest player its flock leader, so the flame follows
 * whoever summoned it, and decides where beside the player it floats.
 *
 * @author Mark Gottschling on 11/06/2026
 */
public class SummonCommand extends AbstractPlayerCommand {

    public SummonCommand() {
        super("summon", "Prototype: conjure a Mage Flame that follows you.");
        this.setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            context.sendMessage(Message.raw("Could not find your position."));
            return;
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
        String role = SummonTracker.roleFor(slot);

        // just above the player's head height
        Vector3d spawnPos = new Vector3d(transform.getPosition()).add(0, 2, 0);
        var spawned = NPCPlugin.get().spawnNPC(store, role, null, spawnPos, Rotation3f.ZERO);
        if (spawned == null) {
            context.sendMessage(Message.raw("Could not spawn " + role + " (is the role loaded?)."));
            return;
        }
        mine.add(new SummonTracker.Flame(slot, spawned.left(), world, System.currentTimeMillis(), playerRef,
                new SummonTracker.Watch()));
        context.sendMessage(Message.raw(replaced ? "Your Mage Flame is renewed." : "A Mage Flame appears."));
    }
}
