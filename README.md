<p align="center"><img src="https://raw.githubusercontent.com/wiki/gottsch/gottsch-hytale-MageFlame/images/mageflame-title.png" alt="Mage Flame" width="500"></p>

Mage Flame is a small mod that adds the ability to create dynamic light sources that follow you wherever you go.
This is done with scrolls crafted from ingredients you find in the world. No mana, no spell points, no mage classes, no levels. Just craft and use.

This is the Hytale version of the [Minecraft mod](https://www.curseforge.com/minecraft/mc-mods/mage-flame) of the same name.
Download it from [CurseForge](https://www.curseforge.com/hytale/mods/mage-flame).

## The scrolls

Right-click a scroll to read it. A flame appears and follows you. The scroll is used up only if the flame actually appears.

| Scroll | Light | Lasts | Recipe |
|---|---|---|---|
| **Mage Flame** | Warm orange, a little dimmer than a torch | 10 minutes | 2 Fibre, 1 Tree Sap, 1 Crude Torch (pocket crafting or Workbench) |
| **Lesser Revelation** | Green, a wider glow than Mage Flame | 15 minutes | Mage Flame scroll, Green Crystal, Life Essence (Workbench) |
| **Greater Revelation** | Blue, brighter than a torch | 30 minutes | Lesser Revelation scroll, Blue Crystal, Void Essence (Arcane Bench) |
| **Bubble Flame** | Warm, inside a translucent bubble. Follows you under water | 20 minutes | Mage Flame scroll, 3 Cyan Crystal (Workbench) |

Scrolls can also be found in chests: cheaper ones in the first zones, Greater Revelations deeper in.

## Screenshots

<p align="center"><img src="https://raw.githubusercontent.com/wiki/gottsch/gottsch-hytale-MageFlame/images/flames.png" alt="Greater Revelation, Mage Flame and Bubble Flame following the player, with the lifespan panel" width="720"></p>

<p align="center"><img src="https://raw.githubusercontent.com/wiki/gottsch/gottsch-hytale-MageFlame/images/lesser-revelation.png" alt="A Lesser Revelation flame lighting a dark, rainy field" width="720"></p>

## Features

- Hands-free light that keeps up with you, hovers higher in the open and lower in tunnels, and catches up if you outrun it or turn a corner underground.
- A lifespan display (top-left) with the time left and a progress bar for each flame.
- Mage Flame and the two Revelations are put out by water. The Bubble Flame is not.
- Flames are saved when you leave or the server stops, and come back with their remaining time when you join.
- One flame per player by default (configurable); a new scroll replaces the oldest flame.
- Flames survive your death and catch up with you after respawn (configurable).

## Installing

1. Put `MageFlame-1.0.0.jar` in your Hytale `UserData\Mods` folder (or your server's `mods` folder).
2. For a local world, enable it in the world's `config.json` (`UserData\Saves\<your world>`):
   ```json
   "Mods": {
     "gottsch:MageFlame": { "Enabled": true }
   }
   ```
3. Start the world and craft a Mage Flame scroll (or `/give Scroll_Mage_Flame`).

Requires Hytale server 0.7.4 or newer in the 0.7 line. Use survival or adventure mode: in creative mode flames do not follow the player.

## Configuration

`MageFlame.json` is created in the mod's data folder on first run. Restart the server after changing it.

| Setting | Default | What it does |
|---|---|---|
| `MaxSummonedEntitiesPerPlayer` | 1 | How many flames a player can have at once (1 to 4) |
| `EnableLifespanDisplay` | true | Show the lifespan panel and its progress bars |
| `MageFlameLifespanSeconds` | 600 | Default lifespan, for flames not listed below |
| `LifespanSecondsByType` | Lesser 900, Greater 1800, Bubble 1200 | Lifespan per flame; 0 or less means it never expires |
| `PersistFlames` | true | Save flames when you leave and bring them back when you join |
| `DespawnOnOwnerDeath` | false | Remove your flames when you die |

## Commands

`/mageflame healthcheck` is open to everyone. These need operator permission, so players cannot skip the scrolls:

- `/mageflame summon [--type=Mage_Flame]`: summon a flame without a scroll
- `/mageflame dispel`: remove all flames in the world
- `/mageflame status`: list flames with their distance from their owner
- `/mageflame light`: light test prototype

## Notes

- The scrolls are added to chests by replacing a few of the game's shared chest loot tables
  (`Zone1_Encounters_Tier1` to `Tier4`, `Zone2_Encounters_Tier1` to `Tier4`, `Zone3_Encounters_Tier2` to `Tier4`) with copies that include them.
  Another mod that changes the same tables will conflict, and a game update to those tables needs a Mage Flame update to follow.
- Tested solo only. Please report anything odd with several players.
- Winged Torch, Ember Hound and Glowglob (from the Minecraft version) are planned for a later release.

See the [changelog](CHANGELOG.md) for what changed in each version.

## Building

Requires Java 25. Targets the Hytale `release` patchline, server 0.7.4.

```
gradlew.bat setupHytaleDev
gradlew.bat build
gradlew.bat runServer
```

`hytaleHomeOverride` in `gradle.properties` points at the local Hytale install's `Assets.zip`.
`manifest.json` is generated from `gradle.properties`; run `gradlew.bat updatePluginManifest` after changing them.
`runServer` starts a dev server: run `/auth login browser` in its console, then connect to `127.0.0.1:5520`.

Built from the [HytaleModding plugin template](https://github.com/HytaleModding/plugin-template).

## Links

- [CurseForge](https://www.curseforge.com/hytale/mods/mage-flame)
- [Issues and suggestions](https://github.com/gottsch/gottsch-hytale-MageFlame/issues)
- [Discord](https://discord.gg/CpWXamx)
- Support the mod: [Ko-fi](https://ko-fi.com/gottsch) or [PayPal](https://www.paypal.com/paypalme/gottschsmods)

## License

LGPL-3.0-or-later. See [LICENSE.txt](LICENSE.txt).
