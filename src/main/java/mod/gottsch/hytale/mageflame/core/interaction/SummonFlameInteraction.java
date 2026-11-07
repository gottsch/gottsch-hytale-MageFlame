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
package mod.gottsch.hytale.mageflame.core.interaction;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mod.gottsch.hytale.mageflame.core.summon.FlameSummoner;

/**
 * Item interaction {@code MageFlameSummon}: conjures a flame for the player using the item. The item's JSON picks the flame with
 * {@code "FlameType"} (the part of the slot role ids before {@code _Slot<N>}, default {@code Mage_Flame}), so a new flame type
 * and its scroll need only JSON. This interaction also uses up one scroll, but only when the summon succeeded (a JSON {@code ModifyInventory} step would use it up
 * even if the summon failed afterwards):
 * <pre>
 * { "Type": "MageFlameSummon", "FlameType": "Mage_Flame" }
 * </pre>
 * <p>
 * The summon itself is deferred to the world thread ({@link World#execute}) because the interaction runs while the entity
 * store is being processed, where spawning entities directly is not allowed.
 *
 * @author Mark Gottschling on 11/06/2026
 */
public class SummonFlameInteraction extends SimpleInstantInteraction {
    /** The type name used in item JSON: {@code "Type": "MageFlameSummon"}. */
    public static final String TYPE_NAME = "MageFlameSummon";

    public static final BuilderCodec<SummonFlameInteraction> CODEC = BuilderCodec
            .builder(SummonFlameInteraction.class, SummonFlameInteraction::new, SimpleInstantInteraction.CODEC)
            .documentation("Conjure a flame that follows the player using the item.")
            .append(new KeyedCodec<>("FlameType", Codec.STRING),
                    (interaction, value, extraInfo) -> interaction.flameType = value,
                    (interaction, extraInfo) -> interaction.flameType)
            .documentation("Which flame: the part of its slot role ids before _Slot<N>, for example Mage_Flame.")
            .add()
            .build();

    private String flameType = FlameSummoner.DEFAULT_FLAME_TYPE;

    public SummonFlameInteraction() {
        super();
    }

    @Override
    protected void firstRun(InteractionType type, InteractionContext context, CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (ref == null || commandBuffer == null) {
            return;
        }
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        Store<EntityStore> store = commandBuffer.getStore();
        World world = store.getExternalData().getWorld();
        final String flame = flameType;
        // the scroll to use up: where it sits now (the world thread runs this a moment later)
        final ItemContainer heldContainer = context.getHeldItemContainer();
        final short heldSlot = context.getHeldItemSlot();
        world.execute(() -> {
            FlameSummoner.Result result = FlameSummoner.summon(store, ref, playerRef, world, flame);
            // use up one scroll only if the flame appeared, so a failed summon costs nothing
            if (result.success() && heldContainer != null) {
                heldContainer.removeItemStackFromSlot(heldSlot, 1);
            }
            playerRef.sendMessage(Message.raw(result.message()));
        });
    }
}
