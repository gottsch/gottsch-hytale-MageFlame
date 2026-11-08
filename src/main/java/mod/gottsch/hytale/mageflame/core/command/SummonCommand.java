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
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mod.gottsch.hytale.mageflame.core.summon.FlameSummoner;

import javax.annotation.Nonnull;

/**
 * /mageflame summon [--type=Mage_Flame] - conjures a flame next to the player, in the lowest free slot. A testing and admin
 * shortcut for what the scroll items do; both go through {@link FlameSummoner}.
 *
 * @author Mark Gottschling on 11/06/2026
 */
public class SummonCommand extends AbstractPlayerCommand {
    private final DefaultArg<String> typeArg;

    public SummonCommand() {
        super("summon", "Conjure a flame that follows you (what the scroll does).");
        // no permission group: needs the generated permission node (operators), so players cannot use it to bypass the scrolls
        this.typeArg = withDefaultArg("type", "Flame type: the part of its slot role ids before _Slot<N>",
                ArgTypes.STRING, FlameSummoner.DEFAULT_FLAME_TYPE, FlameSummoner.DEFAULT_FLAME_TYPE);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String type = typeArg.get(context);
        context.sendMessage(Message.raw(FlameSummoner.summon(store, ref, playerRef, world, type).message()));
    }
}
