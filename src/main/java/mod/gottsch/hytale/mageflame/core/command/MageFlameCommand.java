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

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import mod.gottsch.hytale.mageflame.MageFlame;

/**
 * Root /mageflame command that holds the plugin's subcommands.
 *
 * @author Mark Gottschling on 1/21/2026
 */
public class MageFlameCommand extends AbstractCommandCollection {

    public MageFlameCommand(MageFlame mageFlame) {
        super("mageflame", "Mage Flame root command.");
        // allow anyone to run command
        this.setPermissionGroup(GameMode.Adventure);
        this.addSubCommand(new HealthCheckCommand(mageFlame.getName(), mageFlame.getManifest().getVersion().toString()));
    }
}
