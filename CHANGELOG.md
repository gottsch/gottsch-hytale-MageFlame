# Changelog for Mage Flame for Hytale

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-11-08

First version: hands-free light sources that follow you, summoned with scrolls. Targets Hytale server 0.7.4.

### Added

- **Four scrolls**, each conjuring a flame that follows you and lights your way. Right-click a scroll to read it; it is used up only if the flame appears.
  - **Mage Flame** (warm orange, a little dimmer than a torch, 10 minutes). Crafted from 2 Fibre, 1 Tree Sap and 1 Crude Torch at the Workbench.
  - **Lesser Revelation** (green, 15 minutes). Mage Flame scroll + Green Crystal + Life Essence at the Workbench.
  - **Greater Revelation** (blue, brighter than a torch, 30 minutes). Lesser Revelation scroll + Blue Crystal + Void Essence at the Arcane Bench.
  - **Bubble Flame** (warm light inside a translucent bubble, 20 minutes). Mage Flame scroll + 3 Cyan Crystal at the Workbench. Follows you under water.
- The flames fly beside you, keep up while you run, hover higher in the open and lower in tunnels, and teleport to you if you outrun them or turn a corner underground.
- Mage Flame, Lesser Revelation and Greater Revelation are put out by water (with a hiss and a puff of smoke). Bubble Flame is not.
- A lifespan display (top-left) listing each flame, the time it has left and a progress bar showing how much of its life remains. Can be turned off.
- A per-player limit on flames (default 1); summoning past it replaces the oldest flame.
- Flames are saved when you leave or the server stops, and come back with their remaining time when you join. Time does not run while you are away.
- Scrolls can be found in chests in the world (shared encounter loot, zones 1 to 3): cheaper scrolls early on, Greater Revelation deeper in.
- Configuration file `MageFlame.json`: `EnableLifespanDisplay`, `MaxSummonedEntitiesPerPlayer`, `MageFlameLifespanSeconds` (default lifespan), `LifespanSecondsByType`
  (per-flame lifespan; 0 or less means never expires), `DespawnOnOwnerDeath`, `PersistFlames`.
- Commands under `/mageflame`: `healthcheck` (everyone), and for operators only (they need the command's permission): `summon [--type=...]` (a flame without a scroll),
  `dispel` (remove all flames in the world), `status` (debug) and `light` (light test prototype).

### Notes

- The chest loot tables listed below are **replaced** by copies of the game's tables with scroll chances added (Hytale cannot add to an existing table). Another mod that
  replaces the same tables will conflict with this one, and a game update that changes them will need a Mage Flame update to follow:
  `Zone1_Encounters_Tier1` to `Tier4`, `Zone2_Encounters_Tier1` to `Tier4`, `Zone3_Encounters_Tier2` to `Tier4`.
- Not yet in this version (they exist in the Minecraft mod): Winged Torch, Ember Hound and Glowglob.
