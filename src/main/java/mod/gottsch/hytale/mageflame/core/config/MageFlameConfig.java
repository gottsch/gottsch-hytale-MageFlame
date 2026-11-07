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
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import java.util.HashMap;
import java.util.Map;

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
            .append(new KeyedCodec<>("DespawnOnOwnerDeath", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.despawnOnOwnerDeath = value,
                    (config, extraInfo) -> config.despawnOnOwnerDeath)
            .add()
            .append(new KeyedCodec<>("PersistFlames", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.persistFlames = value,
                    (config, extraInfo) -> config.persistFlames)
            .add()
            .append(new KeyedCodec<>("LifespanSecondsByType", new MapCodec<>(Codec.INTEGER, HashMap::new)),
                    (config, value, extraInfo) -> config.lifespanSecondsByType = value,
                    (config, extraInfo) -> config.lifespanSecondsByType)
            .add()
            .build();

    // show the remaining lifespan of summoned light sources on the HUD
    private boolean enableLifespanDisplay = true;
    // max number of concurrent summoned entities per player
    private int maxSummonedEntitiesPerPlayer = 1;
    // default lifespan in seconds for every flame type not listed in lifespanSecondsByType (10 minutes, the Minecraft versions' 12000 ticks)
    private int mageFlameLifespanSeconds = 600;
    // lifespan in seconds per flame type; 0 or less means that type never expires. Defaults follow the Minecraft versions
    // (12000/18000/36000 ticks = 10/15/30 minutes; Bubble Flame 20 minutes): Mage_Flame uses the default above, the others are listed here.
    private Map<String, Integer> lifespanSecondsByType = new HashMap<>(Map.of(
            "Lesser_Revelation", 900,
            "Greater_Revelation", 1800,
            "Bubble_Flame", 1200));
    // remove a player's flames when that player dies (false: the flames stay with them through respawn)
    private boolean despawnOnOwnerDeath = false;
    // flames are removed when their owner logs out or the server stops; true brings them back (with the time they had left)
    // when the owner returns, false lets them go
    private boolean persistFlames = true;

    private MageFlameConfig() {
    }

    public boolean isPersistFlames() {
        return persistFlames;
    }

    public boolean isDespawnOnOwnerDeath() {
        return despawnOnOwnerDeath;
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

    /**
     * Lifespan in seconds of a flame type: its entry in {@code LifespanSecondsByType}, else the default
     * {@code MageFlameLifespanSeconds}. 0 or less means the flame never expires.
     */
    public int lifespanFor(String flameType) {
        Integer own = lifespanSecondsByType == null ? null : lifespanSecondsByType.get(flameType);
        return own != null ? own : mageFlameLifespanSeconds;
    }
}
