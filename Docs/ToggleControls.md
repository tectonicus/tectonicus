# Marker toggle buttons

Marker toggle buttons appear in a popout menu by default. To restore the original
always-visible buttons at the top right of the map on wide viewports, set `useToggleMenu="false"`
on the global `<config>` element:

```xml
<tectonicus version="2">
    <config outputDir="RenderedMaps" useToggleMenu="false"/>
    <world worldDir="Worlds/Survival">
        <dimension id="minecraft:overworld">
            <layer name="Day"/>
        </dimension>
    </world>
</tectonicus>
```

Set `useToggleMenu="true"` or omit the option to use the popout menu. This viewer
setting applies to every world and dimension in the output. It does not change
which marker categories are exported or their initial visibility.

The command-line option `--useToggleMenu=false` overrides the XML setting.
The popout appears at viewport widths up to 767 CSS pixels. The layout updates
when the viewport crosses that breakpoint.

Regenerate the map's viewer resources to apply the change to an existing output.
