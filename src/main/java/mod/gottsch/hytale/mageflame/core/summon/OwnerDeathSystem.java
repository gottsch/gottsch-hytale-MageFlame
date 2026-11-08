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
package mod.gottsch.hytale.mageflame.core.summon;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mod.gottsch.hytale.mageflame.MageFlame;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Removes a player's Mage Flames when that player dies, if {@code DespawnOnOwnerDeath} is enabled in the config.
 * With the setting off (the default) this does nothing and the flames keep following the player after respawn.
 * Only flames that {@link SummonTracker} knows about are removed.
 *
 * @author Mark Gottschling on 11/06/2026
 */
public class OwnerDeathSystem extends DeathSystems.OnDeathSystem {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        // only players, not every NPC that dies
        return PlayerRef.getComponentType();
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent death,
                                 @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (!MageFlame.getConfig().get().isDespawnOnOwnerDeath()) {
            return;
        }
        PlayerRef player = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null) {
            return;
        }
        List<SummonTracker.Flame> mine = SummonTracker.flamesOf(player.getUuid());
        for (SummonTracker.Flame flame : new ArrayList<>(mine)) {
            SummonTracker.removeFromItsWorld(flame);   // not this commandBuffer: the flame may be in another world
        }
        mine.clear();
    }
}
