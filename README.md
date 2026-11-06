# Mage Flame (Hytale)

Mage Flame adds hands-free light sources that follow you wherever you go, summoned with scrolls.
No mana, no spell points, no mage classes, no levels. Just craft and use.

This is the Hytale server plugin port of the Minecraft mod
([NeoForge](https://github.com/gottsch/gottsch-minecraft-neoforge-MageFlame)).

## Building

Requires Java 25. Targets the Hytale `release` patchline, server 0.7.4.

```
gradlew.bat setupHytaleDev
gradlew.bat build
gradlew.bat runServer
```

`hytaleHomeOverride` in `gradle.properties` points at the local Hytale install's `Assets.zip`.
`manifest.json` is generated from `gradle.properties`; run `gradlew.bat updatePluginManifest` after changing them.

Built from the [HytaleModding plugin template](https://github.com/HytaleModding/plugin-template).

## License

LGPL-3.0-or-later. See [LICENSE.txt](LICENSE.txt).
