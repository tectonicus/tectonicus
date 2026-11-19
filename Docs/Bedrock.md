# Bedrock Edition worlds (experimental)

Tectonicus can render Bedrock Edition worlds. Set `worldDir` to the world's folder, the one that holds `db` and
`level.dat`. Tectonicus detects the format on its own; nothing else in the config changes. Dimensions work as for
Java worlds: `dimension="nether"` or `dimension="end"` on the `<map>` element.

```xml
<map name="My Bedrock World" worldDir="C:\Users\xxxx\AppData\Roaming\Minecraft Bedrock\Users\...\games\com.mojang\minecraftWorlds\AbCdEfGh=">
	<layer name="Day" lighting="day"/>
</map>
```

## How it works

- Tectonicus reads the world's LevelDB database with its own read-only reader. It never writes to, locks or compacts
  the world, but close Minecraft (or copy the world) before rendering, because the game rewrites the database while
  the world is open.
- Bedrock block states are converted to Java Edition block states, and Bedrock biome ids to Java biomes. The map
  is drawn with the models and textures of a **Java Edition client jar**, so it looks like the Java version of the
  world. Without `minecraftJar` in the config, Tectonicus uses the newest jar the Minecraft Launcher has downloaded;
  use a recent one (1.21.11 or 26.x), since block names are mapped to the newest Java names.
- Chunks the game has not finished generating, at the edges of explored land, are skipped, as with Java worlds.

## Supported so far

| Supported | Not yet |
| --- | --- |
| Worlds saved by Bedrock 1.2.13 and later (sub-chunk formats 1, 8 and 9) | Older Pocket Edition worlds |
| Overworld (Y -64 to 319 from 1.18, 0 to 255 before), Nether and End | |
| Block states, including waterlogged blocks and double chests | |
| States Bedrock leaves to the game: fence, pane, bar, chorus and tripwire connections, redstone wire, stair shapes, door halves, tall plants and snowy grass | Fire faces, attached stems and a few other cosmetic states |
| Per-block biomes and Java biome colours | Bedrock's own biome water colours |
| Sky and block light, computed as Java does since Bedrock saves none, so Day, Night and Cave lighting all work | |
| Signs (with back and glowing text), banners (including the ominous banner), bed colours, heads, flower pots, item frames and glow item frames, decorated pots, shulker boxes, beacons, and chest, barrel and shulker box contents | Player head skins: Bedrock saves none |
| Paintings and armor stands with their equipment (poses: default, none, brandish, salute and zombie) | Other armor stand poses |
| Players: the local player and others who joined, with position, dimension, health and inventory | Player names and skins: Bedrock saves none, so the local player takes `singlePlayerName` from the config and others show as "Player" |
| Maps in item frames | |
| | Bedrock resource packs |

Paintings are placed from the centre Bedrock saves, using the vanilla painting sizes; a painting from an add-on is
treated as 1x1.

## Mapping data

The block and biome tables in `src/main/resources/bedrock` are generated from
[PrismarineJS minecraft-data](https://github.com/PrismarineJS/minecraft-data) (MIT licence) by
`SourceData/bedrock/generate_bedrock_mappings.py`. Regenerate them when a new Minecraft version adds blocks.
