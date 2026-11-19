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
| Block states, including waterlogged blocks and double chests | Fence, pane, wall and redstone connections; stair shapes |
| Per-block biomes and Java biome colours | Bedrock's own biome water colours |
| Signs, including back text and glowing text | Banners, beds, skulls, flower pots, item frames and other block entities |
| Day lighting, estimated from the blocks above each column | Saved light: Bedrock saves none, so Night and Cave lighting are dark |
| | Paintings, armor stands, players and maps |
| | Bedrock resource packs |

## Mapping data

The block and biome tables in `src/main/resources/bedrock` are generated from
[PrismarineJS minecraft-data](https://github.com/PrismarineJS/minecraft-data) (MIT licence) by
`SourceData/bedrock/generate_bedrock_mappings.py`. Regenerate them when a new Minecraft version adds blocks.
