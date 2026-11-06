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
package mod.gottsch.hytale.mageflame.core.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Server configuration for Mage Flame, saved as MageFlame.json in the plugin's data folder.
 *
 * @author Mark Gottschling on 1/23/2026
 */
public class MageFlameConfig {

    public static final BuilderCodec<MageFlameConfig> CODEC = BuilderCodec.builder(MageFlameConfig.class, MageFlameConfig::new)
            .append(new KeyedCodec<>("EnableLifespanDisplay", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.enableLifespanDisplay = value,
                    (config, extraInfo) -> config.enableLifespanDisplay)
            .add()
            .append(new KeyedCodec<>("MaxSummonedEntitiesPerPlayer", Codec.INTEGER),
                    (config, value, extraInfo) -> config.maxSummonedEntitiesPerPlayer = value,
                    (config, extraInfo) -> config.maxSummonedEntitiesPerPlayer)
            .add()
            .append(new KeyedCodec<>("MageFlameLifespanSeconds", Codec.INTEGER),
                    (config, value, extraInfo) -> config.mageFlameLifespanSeconds = value,
                    (config, extraInfo) -> config.mageFlameLifespanSeconds)
            .add()
            .build();

    // show the remaining lifespan of summoned light sources on the HUD
    private boolean enableLifespanDisplay = true;
    // max number of concurrent summoned entities per player
    private int maxSummonedEntitiesPerPlayer = 1;
    // lifespan of a Mage Flame in seconds (10 minutes, same as the Minecraft versions' 12000 ticks)
    private int mageFlameLifespanSeconds = 600;

    private MageFlameConfig() {
    }

    public boolean isEnableLifespanDisplay() {
        return enableLifespanDisplay;
    }

    public int getMaxSummonedEntitiesPerPlayer() {
        return maxSummonedEntitiesPerPlayer;
    }

    public int getMageFlameLifespanSeconds() {
        return mageFlameLifespanSeconds;
    }
}
