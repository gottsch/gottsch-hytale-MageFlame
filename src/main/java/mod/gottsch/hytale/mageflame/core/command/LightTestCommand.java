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
import com.hypixel.hytale.protocol.ColorLight;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.DynamicLight;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * /mageflame light - prototype that toggles a {@link DynamicLight} on the calling player's entity,
 * to find out how Hytale renders entity light (radius, colour, who can see it).
 * Run it once to turn the light on and again to turn it off.
 *
 * @author Mark Gottschling on 11/06/2026
 */
public class LightTestCommand extends AbstractPlayerCommand {
    private final DefaultArg<Integer> radiusArg;
    private final DefaultArg<String> colorArg;

    public LightTestCommand() {
        super("light", "Prototype: toggle a dynamic light on yourself.");
        this.setPermissionGroup(GameMode.Adventure);
        this.radiusArg = withDefaultArg("radius", "Light radius (0-127)", ArgTypes.INTEGER, 8, "8");
        this.colorArg = withDefaultArg("color", "Colour as 3 hex digits, one per channel (0-F)", ArgTypes.STRING, "fd9", "fd9");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        // already lit -> turn it off
        if (store.removeComponentIfExists(ref, DynamicLight.getComponentType())) {
            context.sendMessage(Message.raw("Dynamic light off."));
            return;
        }

        int radius = Math.clamp(radiusArg.get(context), 0, 127);
        String color = colorArg.get(context);
        if (!color.matches("[0-9a-fA-F]{3}")) {
            context.sendMessage(Message.raw("Colour must be 3 hex digits, e.g. fd9."));
            return;
        }
        // one hex digit (0-F) per channel, same scale as the "#rgb" colours in the asset files
        byte r = (byte) Character.digit(color.charAt(0), 16);
        byte g = (byte) Character.digit(color.charAt(1), 16);
        byte b = (byte) Character.digit(color.charAt(2), 16);

        store.putComponent(ref, DynamicLight.getComponentType(), new DynamicLight(new ColorLight((byte) radius, r, g, b)));
        context.sendMessage(Message.raw("Dynamic light on: radius " + radius + ", colour #" + color + ". Run again to turn off."));
    }
}
