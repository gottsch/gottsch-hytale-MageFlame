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

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import mod.gottsch.hytale.mageflame.core.summon.SummonTracker;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * The lifespan HUD: one row per conjured flame showing its name, the time it has left (for example {@code Mage Flame  9:42}) and a progress bar.
 * <p>
 * Each row has a progress bar under it showing the share of the lifespan left (the game's own {@code ProgressBar} element).
 * The layout is inline markup built in Java (the same way vanilla's {@code SpectatingHud} does it), so it needs no layout file
 * and no asset restart. It is built once with a hidden row for every slot; {@link #refresh} then shows, hides and re-texts rows.
 * Place and size it with {@link #ANCHOR}.
 *
 * @author Mark Gottschling on 11/06/2026
 */
public class FlameHud extends CustomUIHud {
    /** HUD key, unique per HUD. */
    public static final String KEY = "MageFlameHud";

    /** Position of the HUD on screen (left edge, distance from the top, width), in UI pixels. */
    private static final String ANCHOR = "(Left: 20, Top: 120, Width: 240)";

    private static final int ROWS = SummonTracker.SLOT_COUNT;

    /** What the rows showed at the last update, so unchanged HUDs send no packet. */
    private String lastSignature = "";

    public FlameHud(PlayerRef playerRef) {
        super(playerRef, KEY);
    }

    @Override
    protected void build(UICommandBuilder builder) {
        StringBuilder markup = new StringBuilder("Group #MageFlameHud {\n  Anchor: " + ANCHOR + ";\n  LayoutMode: Top;\n");
        for (int i = 1; i <= ROWS; i++) {
            markup.append("""
                      Group #FlameRow%1$d {
                        Visible: false;
                        LayoutMode: Top;
                        Anchor: (Bottom: 6, Height: 46);
                        Background: #000000(0.35);
                        Padding: (Horizontal: 10, Vertical: 5);

                        Group {
                          LayoutMode: Left;
                          Anchor: (Height: 24);

                          Label #FlameName%1$d {
                            Anchor: (Width: 140);
                            Text: "";
                            Style: (FontSize: 15, TextColor: #ffb347, VerticalAlignment: Center);
                          }

                          Label #FlameTime%1$d {
                            Text: "";
                            Style: (FontSize: 17, TextColor: #ffffff, RenderBold: true, VerticalAlignment: Center);
                          }
                        }

                        Group {
                          Anchor: (Top: 3, Height: 8);
                          Background: #1b1b24(0.9);

                          ProgressBar #FlameBar%1$d {
                            Value: 1.0;
                            BarTexturePath: "Common/ProgressBarFill.png";
                          }
                        }
                      }
                    """.formatted(i));
        }
        markup.append("}\n");
        builder.appendInline(null, markup.toString());
    }

    /**
     * Shows one row per flame (oldest first) with the time it has left, and hides the unused rows. Sends nothing when the
     * text is the same as last time.
     *
     * @param flames     the player's flames
     * @param lifespanOf lifespan in seconds for a flame type (0 or less: never expires, shown as {@code --})
     * @param enabled    false hides every row (the {@code EnableLifespanDisplay} setting)
     */
    public void refresh(List<SummonTracker.Flame> flames, ToIntFunction<String> lifespanOf, boolean enabled) {
        UICommandBuilder builder = new UICommandBuilder();
        StringBuilder signature = new StringBuilder();
        for (int i = 1; i <= ROWS; i++) {
            boolean visible = enabled && i <= flames.size();
            builder.set("#FlameRow" + i + ".Visible", visible);
            if (visible) {
                SummonTracker.Flame flame = flames.get(i - 1);
                String name = flame.type().replace('_', ' ');
                int remaining = SummonTracker.remainingSeconds(flame, lifespanOf.applyAsInt(flame.type()));
                String time = remaining == SummonTracker.NO_EXPIRY ? "--" : format(remaining);
                // share of the lifespan still left; a flame that never expires keeps a full bar
                int total = lifespanOf.applyAsInt(flame.type());
                float share = remaining == SummonTracker.NO_EXPIRY || total <= 0 ? 1.0f : Math.clamp(remaining / (float) total, 0f, 1f);
                builder.set("#FlameName" + i + ".Text", name);
                builder.set("#FlameTime" + i + ".Text", time);
                builder.set("#FlameBar" + i + ".Value", share);
                signature.append(i).append(name).append(time).append(Math.round(share * 100)).append('|');
            } else {
                signature.append(i).append("-|");
            }
        }
        if (signature.toString().equals(lastSignature)) {
            return;
        }
        lastSignature = signature.toString();
        update(false, builder);
    }

    /** Seconds as {@code m:ss}, for example 582 -> {@code 9:42}. */
    static String format(int seconds) {
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }
}
