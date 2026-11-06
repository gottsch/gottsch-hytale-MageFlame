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
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import mod.gottsch.hytale.mageflame.core.command.MageFlameCommand;
import mod.gottsch.hytale.mageflame.core.config.MageFlameConfig;
import mod.gottsch.hytale.mageflame.core.interaction.SummonFlameInteraction;
import mod.gottsch.hytale.mageflame.core.summon.OwnerDeathSystem;
import mod.gottsch.hytale.mageflame.core.summon.SummonTracker;

import javax.annotation.Nonnull;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
        // item interactions usable from asset JSON, e.g. the scroll's "Type": "MageFlameSummon"
        this.getCodecRegistry(Interaction.CODEC).register(SummonFlameInteraction.TYPE_NAME,
                SummonFlameInteraction.class, SummonFlameInteraction.CODEC);
        // systems
        this.getEntityStoreRegistry().registerSystem(new OwnerDeathSystem());
        // the plugin owns the flames' lifespan: once a second, remove the ones that have outlived the configured time
        @SuppressWarnings("unchecked")
        ScheduledFuture<Void> expiryTask = (ScheduledFuture<Void>) HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                SummonTracker.expire(Math.max(1, config.get().getMageFlameLifespanSeconds()));
                SummonTracker.watchdog();
            } catch (Exception e) {
                // an exception would silently stop the schedule, so log it and keep ticking
                LOGGER.atWarning().withCause(e).log("Mage Flame expiry check failed");
            }
        }, 1, 1, TimeUnit.SECONDS);
        this.getTaskRegistry().registerTask(expiryTask);
    }

    public static Config<MageFlameConfig> getConfig() {
        return config;
    }
}
