/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
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

public class MutableMap implements Map
{
	private final String id;
	private String name;
	private File worldDir;
	@Getter
	@Setter
	private Dimension dimension;
	
	private int closestZoomSize;
	
	private int cameraAngle, cameraElevation;
	
	private boolean useBiomeColours;

	@Getter
	@Setter
	private boolean smoothLit;
	
	@Getter
	@Setter
	private PlayerFilter playerFilter;
	@Getter
	@Setter
	private SignFilter signFilter;
	@Getter
	@Setter
	private PortalFilter portalFilter;
	@Getter
	@Setter
	private ViewFilter viewFilter;
	@Getter
	@Setter
	private ChestFilter chestFilter;
	@Getter
	@Setter
	private BeaconFilter beaconFilter;
	
	private MutableViewConfig viewConfig;

	@Getter
	@Setter
	private WorldSubset worldSubset;
	
	private List<File> modJars;
	
	private List<Layer> layers;
	
	private NorthDirection northDirection;
	private String compassRoseFile;

	@Getter
	@Setter
	private Vector3l origin;
	
	public MutableMap(String id)
	{
		this.id = id;
		this.name = "Unnamed map";
		this.worldDir = new File(".");
		this.dimension = Dimension.OVERWORLD;
		
		this.playerFilter = new PlayerFilter();
		this.signFilter = new SignFilter(SignFilterType.ALL);
		this.portalFilter = new PortalFilter(PortalFilterType.All);
		this.viewFilter = new ViewFilter(ViewFilterType.All);
		this.chestFilter = new ChestFilter();
		this.beaconFilter = new BeaconFilter(BeaconFilterType.ALL);
		
		this.viewConfig = new MutableViewConfig();
		
		this.modJars = new ArrayList<>();
		
		this.layers = new ArrayList<>();
		
		this.closestZoomSize = 32;
		
		this.cameraAngle = 45;
		this.cameraElevation = 45;
		
		this.useBiomeColours = false;
		
		this.worldSubset = new FullWorldSubset();
		
		this.northDirection = NorthDirection.MinusX;

		this.origin = new Vector3l(0, 0, 0);

		this.smoothLit = true;
	}
	
	@Override
	public String getId()
	{
		return id;
	}
	
	
	public void setCameraAngleDeg(final int angleInDeg)
	{
		this.cameraAngle = angleInDeg;
	}
	@Override
	public int getCameraAngleDeg()
	{
		return cameraAngle;
	}
	@Override
	public float getCameraAngleRad()
	{
		final float normalised = cameraAngle / 360.0f;
		final float rad = normalised * (float)Math.PI * 2.0f;
		return rad;
	}
	
	public void setCameraElevationDeg(final int elevationInDeg)
	{
		this.cameraElevation = elevationInDeg;
	}
	@Override
	public int getCameraElevationDeg()
	{
		return cameraElevation;
	}
	@Override
	public float getCameraElevationRad()
	{
		final float normalised = cameraElevation / 360.0f;
		final float rad = normalised * (float)Math.PI * 2.0f;
		return rad;
	}
	
	public void setClosestZoomSize(final int size)
	{
		this.closestZoomSize = size;
	}
	@Override
	public int getClosestZoomSize()
	{
		return closestZoomSize;
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	public void setName(final String name)
	{
		this.name = name;
	}
	
	@Override
	public File getWorldDir()
	{
		return worldDir;
	}
	
	public void setWorldDir(final File worldDir)
	{
		this.worldDir = worldDir;
	}
	
	public void setModJars(List<File> modJars)
	{
		this.modJars = modJars;
	}
	public List<File> getModJars() { return modJars; }
	
	@Override
	public int numLayers()
	{
		return layers.size();
	}
	
	@Override
	public Layer getLayer(final int index)
	{
		if (index < 0 || index >= layers.size())
			return null;
		
		return layers.get(index);
	}
	
	public void addLayer(Layer newLayer)
	{
		if (layers.contains(newLayer))
			throw new RuntimeException("Map already contains this layer!");
		
		layers.add(newLayer);
	}
	
	@Override
	public List<Layer> getLayers()
	{
		return new ArrayList<>(layers);
	}
	
	@Override
	public MutableViewConfig getViewConfig()
	{
		return viewConfig;
	}
	
	@Override
	public boolean useBiomeColours()
	{
		return useBiomeColours;
	}
	public void setUseBiomeColours(final boolean use)
	{
		this.useBiomeColours = use;
	}
	
	@Override
	public NorthDirection getNorthDirection()
	{
		return northDirection;
	}
	
	public void setNorthDirection(NorthDirection dir)
	{
		this.northDirection = dir;
	}
	
	@Override
	public File getCustomCompassRose()
	{
		if (compassRoseFile != null && compassRoseFile.length() > 0)
			return new File(compassRoseFile);
		
		return null;
	}
	
	public void setCustomCompassRose(String file)
	{
		this.compassRoseFile = file;
	}
}
