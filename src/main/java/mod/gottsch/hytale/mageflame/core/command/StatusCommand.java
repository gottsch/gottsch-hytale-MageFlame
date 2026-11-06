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
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.flock.FlockMembership;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import mod.gottsch.hytale.mageflame.core.summon.SummonTracker;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * /mageflame status - debugging aid. Lists every Mage Flame in the world with its role, its distance from the caller
 * and its flock membership, plus the caller's own flock membership.
 * Used to find out why a flame is not following (for example one that never joined the player's flock).
 *
 * @author Mark Gottschling on 11/06/2026
 */
public class StatusCommand extends AbstractPlayerCommand {

    public StatusCommand() {
        super("status", "Debug: list Mage Flames with their distance and flock membership.");
        this.setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        TransformComponent me = store.getComponent(ref, TransformComponent.getComponentType());
        if (me == null) {
            context.sendMessage(Message.raw("Could not find your position."));
            return;
        }
        FlockMembership myFlock = store.getComponent(ref, FlockMembership.getComponentType());
        List<String> lines = new ArrayList<>();
        lines.add("You: " + describe(myFlock));

        store.forEachChunk(NPCEntity.getComponentType(), (chunk, commandBuffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                NPCEntity npc = chunk.getComponent(i, NPCEntity.getComponentType());
                if (npc == null || !SummonTracker.isFlameRole(npc.getRoleName())) {
                    continue;
                }
                TransformComponent t = chunk.getComponent(i, TransformComponent.getComponentType());
                String distance = t == null ? "?" : String.format("%.1f", t.getPosition().distance(me.getPosition()));
                lines.add(npc.getRoleName() + ": " + distance + " blocks away, "
                        + describe(chunk.getComponent(i, FlockMembership.getComponentType())));
            }
        });

        if (lines.size() == 1) {
            lines.add("No Mage Flames in this world.");
        }
        lines.forEach(line -> context.sendMessage(Message.raw(line)));
    }

    private static String describe(FlockMembership membership) {
        if (membership == null) {
            return "no flock";
        }
        String id = String.valueOf(membership.getFlockId());
        return membership.getMembershipType() + " of flock " + (id.length() > 8 ? id.substring(0, 8) : id);
    }
}
