/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import lombok.Getter;
import lombok.Setter;
import tectonicus.configuration.filter.BeaconFilter;
import tectonicus.configuration.filter.BeaconFilterType;
import tectonicus.configuration.filter.ChestFilter;
import tectonicus.configuration.filter.PlayerFilter;
import tectonicus.configuration.filter.PortalFilter;
import tectonicus.configuration.filter.PortalFilterType;
import tectonicus.configuration.filter.SignFilter;
import tectonicus.configuration.filter.SignFilterType;
import tectonicus.configuration.filter.ViewFilter;
import tectonicus.configuration.filter.ViewFilterType;
import tectonicus.util.Vector3l;
import tectonicus.world.subset.FullWorldSubset;
import tectonicus.world.subset.WorldSubset;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Camera, subset and layers for one dimension of a Minecraft save. */
@Getter
@Setter
public class DimensionConfig extends RenderOptions {
	private final String id;
	private WorldConfig world;
	private String name = "Unnamed dimension";
	private File worldDir = new File(".");
	private Dimension dimension = Dimension.OVERWORLD;
	private DimensionInfo dimensionInfo = new DimensionInfo(dimension, dimension.getId());
	private MarkerVisibility markerVisibility = new MarkerVisibility();
	private int numZoomLevels = 8;
	private int closestZoomSize = 32;
	private int cameraAngleDeg = 45;
	private int cameraElevationDeg = 45;
	private boolean useBiomeColours;
	private PlayerFilter playerFilter = new PlayerFilter();
	private boolean showPlayerInventories;
	private SignFilter signFilter = new SignFilter(SignFilterType.ALL);
	private PortalFilter portalFilter = new PortalFilter(PortalFilterType.All);
	private ViewFilter viewFilter = new ViewFilter(ViewFilterType.All);
	private ChestFilter chestFilter = new ChestFilter();
	private BeaconFilter beaconFilter = new BeaconFilter(BeaconFilterType.ALL);
	private final ViewConfig viewConfig = new ViewConfig();
	private WorldSubset worldSubset = new FullWorldSubset();
	private List<File> modJars = new ArrayList<>();
	private final List<LayerConfig> layers = new ArrayList<>();
	private NorthDirection northDirection = NorthDirection.MinusX;
	private String compassRoseFile;
	private Vector3l origin = new Vector3l(0, 0, 0);

	public DimensionConfig(String id) {
		this.id = id;
	}

	/** Script variable names are independent of filesystem IDs. Escape punctuation unambiguously. */
	public String getJavascriptId() {
		StringBuilder result = new StringBuilder("dimension_");
		for (char character : id.toCharArray()) {
			if (character >= 'a' && character <= 'z' || character >= 'A' && character <= 'Z'
					|| character >= '0' && character <= '9')
				result.append(character);
			else
				result.append('_').append((int) character).append('_');
		}
		return result.toString();
	}

	public float getCameraAngleRad() {
		return cameraAngleDeg / 360.0f * (float) Math.PI * 2.0f;
	}

	public float getCameraElevationRad() {
		return cameraElevationDeg / 360.0f * (float) Math.PI * 2.0f;
	}

	public File getWorldDir() {
		return world != null ? world.getWorldDir() : worldDir;
	}

	public void setWorldDir(File worldDir) {
		this.worldDir = worldDir;
		if (world != null)
			world.setWorldDir(worldDir);
	}

	public int numLayers() {
		return layers.size();
	}

	public LayerConfig getLayer(int index) {
		return index >= 0 && index < layers.size() ? layers.get(index) : null;
	}

	public void addLayer(LayerConfig layer) {
		if (!id.equals(layer.getDimensionId()))
			throw new IllegalArgumentException("Layer belongs to a different dimension: " + layer.getId());
		if (layers.stream().anyMatch(existing -> existing.getId().equalsIgnoreCase(layer.getId())))
			throw new IllegalArgumentException("Duplicate layer ID: " + layer.getId());
		layers.add(layer);
		layer.setWorld(world);
	}

	public List<LayerConfig> getLayers() {
		return List.copyOf(layers);
	}

	public boolean useBiomeColours() {
		return useBiomeColours;
	}

	public File getCustomCompassRose() {
		return compassRoseFile != null && !compassRoseFile.isEmpty() ? new File(compassRoseFile) : null;
	}

	public void setCustomCompassRose(String file) {
		compassRoseFile = file;
	}
}
