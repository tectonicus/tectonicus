# Map overlays

Overlays are lines and shapes that the map viewer draws over the rendered tiles. They are
stored as Minecraft coordinates, so they need no extra tile rendering and can be turned on
and off in the viewer. Every overlay is off unless you configure it.

Add an `<overlays>` element to a `<dimension>` or `<map>`. An `<overlays>` element on a
`<world>` or `<config>` applies to every dimension below it, and a dimension can override
single attributes.

```xml
<tectonicus version="2">
    <config outputDir="RenderedMaps"/>
    <world worldDir="Worlds/Survival">
        <dimension id="minecraft:overworld">
            <overlays chunkGrid="hidden" regionGrid="hidden" slimeChunks="hidden"
                      worldBorder="on" file="roads.json" y="64"/>
            <layer name="Day"/>
        </dimension>
    </world>
</tectonicus>
```

Each overlay takes one of three values:

| Value | Result |
| --- | --- |
| `off` (default) | Not exported, no toggle in the viewer. |
| `on` | Exported and shown when the map opens. |
| `hidden` | Exported, but hidden until it is turned on in the viewer. |

| Attribute | Overlay |
| --- | --- |
| `chunkGrid` | 16 × 16 chunk boundaries. Drawn only when zoomed in far enough. |
| `regionGrid` | 512 × 512 region file boundaries. |
| `slimeChunks` | Chunks where slimes can spawn below Y 40. Overworld only. |
| `worldBorder` | The world border saved in the world. |
| `file` | A JSON file of lines, polygons and markers, relative to the config file. |
| `fileVisible` | `on` (default) or `hidden` for the overlays from `file`. |
| `y` | The block height flat overlays are drawn at. Default 64. |

When overlays are configured, an overlay button appears at the top right of the map. It lists
the overlays of the current dimension. Choices are kept while switching between layers of the
same dimension.

## Height and the camera angle

Tectonicus draws the world at an angle, so a point's height changes where it appears on the
map. Grids, slime chunks and the world border are drawn flat at the height set by `y`. They
line up with terrain at that height and appear shifted on higher or lower ground. Set `y` to
the height that matters most for the map, such as sea level or the height of a town. A map
rendered with `cameraElevation="90"` is seen from straight above and has no shift.

Points in an overlay file can carry their own height, so a road can follow the terrain.

## Slime chunks and the world seed

Tectonicus reads the seed from the save while rendering and exports only the list of slime
chunks inside the rendered area. The seed itself is not written to the output. Note that a
long list of slime chunks can still help someone recover part of the seed with public tools,
and the rendered biomes can help with the rest. Keep the overlay off if the seed must stay
secret.

## Overlay files

An overlay file holds a `features` array (or just the array):

```json
{
  "features": [
    {
      "type": "line",
      "name": "North road",
      "description": "Spawn to the ice spikes",
      "color": "#d03030",
      "width": 4,
      "points": [[0, 70, 0], [0, 72, -400], [120, 80, -650]]
    },
    {
      "type": "polygon",
      "name": "Market lot",
      "color": "#3060d0",
      "fillOpacity": 0.2,
      "points": [[100, -50], [140, -50], [140, -10], [100, -10]]
    },
    {
      "type": "marker",
      "name": "Shop",
      "description": "Open on weekends",
      "point": {"x": 30, "y": 65, "z": -12}
    }
  ]
}
```

- `type` is `line`, `polygon` or `marker`.
- Points are `[x, y, z]`, `[x, z]` or `{"x": ..., "y": ..., "z": ...}` in block coordinates.
  A point without a height uses the `y` attribute.
- `name` appears when hovering, and `name` and `description` appear when clicking.
- `color` and `fillColor` take `#rgb`, `#rrggbb` or a color name. `width` is the line width in
  pixels. `opacity` and `fillOpacity` range from 0 to 1.

Each dimension reads its own file. A file with errors is reported in the log and skipped; the
rest of the render continues.
