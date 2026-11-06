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
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import mod.gottsch.hytale.mageflame.core.summon.SummonTracker;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * /mageflame dispel - removes every Mage Flame in the caller's world.
 * Prototype: flames have no owner yet, so this clears all of them, including ones left over from earlier runs.
 *
 * @author Mark Gottschling on 11/06/2026
 */
public class DispelCommand extends AbstractPlayerCommand {

    public DispelCommand() {
        super("dispel", "Dispel every Mage Flame in this world.");
        this.setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        AtomicInteger removed = new AtomicInteger();
        store.forEachChunk(NPCEntity.getComponentType(), (chunk, commandBuffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                NPCEntity npc = chunk.getComponent(i, NPCEntity.getComponentType());
                if (npc != null && SummonTracker.isFlameRole(npc.getRoleName())) {
                    commandBuffer.removeEntity(chunk.getReferenceTo(i), RemoveReason.REMOVE);
                    removed.incrementAndGet();
                }
            }
        });
        SummonTracker.clear();
        context.sendMessage(Message.raw(removed.get() == 0
                ? "There are no Mage Flames to dispel."
                : "Dispelled " + removed.get() + " Mage Flame" + (removed.get() == 1 ? "" : "s") + "."));
    }
}
