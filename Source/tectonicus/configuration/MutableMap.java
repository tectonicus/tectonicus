/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import tectonicus.configuration.Configuration.Dimension;
import tectonicus.world.subset.FullWorldSubsetFactory;
import tectonicus.world.subset.WorldSubsetFactory;

public class MutableMap implements Map
{
	private String id;
	private String name;
	private File worldDir;
	private Dimension dimension;
	
	private int closestZoomSize;
	
	private int cameraAngle, cameraElevation;
	
	private boolean useBiomeColours;
	
	private PlayerFilter playerFilter;
	private SignFilter signFilter;
	private PortalFilter portalFilter;
	private ViewFilter viewFilter;
	
	private MutableViewConfig viewConfig;
	
	private WorldSubsetFactory worldSubsetFactory;
	
	private ArrayList<Layer> layers;
	
	private NorthDirection northDirection;
	private String compassRoseFile;
	
	public MutableMap(String id)
	{
		this.id = id;
		this.name = "Unnamed map";
		this.worldDir = new File(".");
		this.dimension = Dimension.Terra;
		
		this.playerFilter = new PlayerFilter(PlayerFilterType.All);
		this.signFilter = SignFilter.All;
		this.portalFilter = new PortalFilter(PortalFilterType.All);
		this.viewFilter = new ViewFilter(ViewFilterType.All);
		
		this.viewConfig = new MutableViewConfig();
		
		this.layers = new ArrayList<Layer>();
		
		this.closestZoomSize = 32;
		
		this.cameraAngle = 45;
		this.cameraElevation = 45;
		
		this.useBiomeColours = false;
		
		this.worldSubsetFactory = new FullWorldSubsetFactory();
		
		this.northDirection = NorthDirection.MinusX;
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
		final float normalised = (float)cameraAngle / 360.0f;
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
		final float normalised = (float)cameraElevation / 360.0f;
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
	
	@Override
	public Dimension getDimension()
	{
		return dimension;
	}
	
	public void setDimension(final Dimension dimension)
	{
		this.dimension = dimension;
	}
	
	
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
		return new ArrayList<Layer>(layers);
	}
	
	@Override
	public MutableViewConfig getViewConfig()
	{
		return viewConfig;
	}
	
	@Override
	public WorldSubsetFactory getWorldSubsetFactory()
	{
		return worldSubsetFactory;
	}
	public void setWorldSubsetFactory(WorldSubsetFactory factory)
	{
		this.worldSubsetFactory = factory;
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
	public PlayerFilter getPlayerFilter()
	{
		return playerFilter;
	}
	public void setPlayerFilter(PlayerFilter filter)
	{
		this.playerFilter = filter;
	}
	
	@Override
	public ViewFilter getViewFilter()
	{
		return viewFilter;
	}
	public void setViewFilter(ViewFilter filter)
	{
		this.viewFilter = filter;
	}
	
	@Override
	public SignFilter getSignFilter()
	{
		return signFilter;
	}
	public void setSignFilter(SignFilter filter)
	{
		this.signFilter = filter;
	}
	
	@Override
	public PortalFilter getPortalFilter()
	{
		return portalFilter;
	}
	public void setPortalFilter(PortalFilter filter)
	{
		this.portalFilter = filter;
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
