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
package mod.gottsch.hytale.mageflame.core.hud;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mod.gottsch.hytale.mageflame.MageFlame;
import mod.gottsch.hytale.mageflame.core.config.MageFlameConfig;
import mod.gottsch.hytale.mageflame.core.summon.SummonTracker;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps each player's {@link FlameHud} in step with their flames. A player is registered when they summon a flame; from then on
 * {@link #tick()} (called once a second from the plugin's timer) refreshes their HUD on their world's thread until they leave.
 * The HUD is (re)added to the player's HUD manager whenever it is missing, which covers respawning, where the player entity,
 * and with it the HUD, is recreated.
 *
 * @author Mark Gottschling on 11/06/2026
 */
public final class FlameHudManager {
    private record Tracked(PlayerRef player, World world) {
    }

    private static final Map<UUID, Tracked> PLAYERS = new ConcurrentHashMap<>();

    private FlameHudManager() {
    }

    /**
     * Starts keeping this player's HUD up to date. Safe to call repeatedly. An entry left over from an earlier connection
     * (the player relogged, so their PlayerRef is a different object) is replaced, otherwise the HUD would never come back.
     */
    public static void track(PlayerRef player, World world) {
        PLAYERS.compute(player.getUuid(), (id, old) ->
                old != null && old.player() == player && old.world() == world ? old : new Tracked(player, world));
    }

    /** Called about once a second from any thread; the work runs on each player's world thread. */
    public static void tick() {
        for (Map.Entry<UUID, Tracked> entry : PLAYERS.entrySet()) {
            Tracked tracked = entry.getValue();
            try {
                tracked.world().execute(() -> refresh(entry.getKey(), tracked));
            } catch (IllegalThreadStateException e) {
                // the world is gone (crashed or shut down): the player gets a fresh entry when they summon again
                PLAYERS.remove(entry.getKey());
            }
        }
    }

    private static void refresh(UUID id, Tracked tracked) {
        PlayerRef playerRef = tracked.player();
        if (!playerRef.isValid()) {
            PLAYERS.remove(id);
            return;
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        HudManager hudManager = player.getHudManager();
        CustomUIHud existing = hudManager.getCustomHud(FlameHud.KEY);
        FlameHud hud;
        if (existing instanceof FlameHud flameHud) {
            hud = flameHud;
        } else {
            hud = new FlameHud(playerRef);
            hudManager.addCustomHud(playerRef, hud);
        }

        MageFlameConfig config = MageFlame.getConfig().get();
        List<SummonTracker.Flame> flames = SummonTracker.flamesOf(id);
        hud.refresh(flames, config::lifespanFor, config.isEnableLifespanDisplay());
    }
}
