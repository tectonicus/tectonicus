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
import java.util.List;

import tectonicus.configuration.Configuration.Dimension;
import tectonicus.world.subset.WorldSubsetFactory;

public interface Map
{
	public String getId();
	
	public String getName();
	
	public File getWorldDir();
	
	public Dimension getDimension();
	
	public WorldSubsetFactory getWorldSubsetFactory();
	
	public float getCameraAngleRad();
	public int getCameraAngleDeg();
	
	public float getCameraElevationRad();
	public int getCameraElevationDeg();
	
	public int getClosestZoomSize();
	
	public boolean useBiomeColours();
	
	public PlayerFilter getPlayerFilter();
	public SignFilter getSignFilter();
	public PortalFilter getPortalFilter();
	public ViewFilter getViewFilter();
	
	public int numLayers();
	public Layer getLayer(final int index);
	public List<Layer> getLayers();
	
	public ViewConfig getViewConfig();
	
	public NorthDirection getNorthDirection();
	public File getCustomCompassRose();
}
