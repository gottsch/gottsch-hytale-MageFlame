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
package mod.gottsch.hytale.mageflame;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import mod.gottsch.hytale.mageflame.core.command.MageFlameCommand;
import mod.gottsch.hytale.mageflame.core.config.MageFlameConfig;
import mod.gottsch.hytale.mageflame.core.summon.OwnerDeathSystem;

import javax.annotation.Nonnull;

/**
 * Mage Flame plugin entry point.
 *
 * @author Mark Gottschling on 1/21/2026
 */
public class MageFlame extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static Config<MageFlameConfig> config;

    public MageFlame(@Nonnull JavaPluginInit init) {
        super(init);
        // registers the configuration with the filename "MageFlame"
        config = this.withConfig("MageFlame", MageFlameConfig.CODEC);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up %s version %s", this.getName(), this.getManifest().getVersion().toString());
        // write the config file with defaults if it doesn't exist yet
        config.save();
        // commands
        this.getCommandRegistry().registerCommand(new MageFlameCommand(this));
        // systems
        this.getEntityStoreRegistry().registerSystem(new OwnerDeathSystem());
    }

    public static Config<MageFlameConfig> getConfig() {
        return config;
    }
}
