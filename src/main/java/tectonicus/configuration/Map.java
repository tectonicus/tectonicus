/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
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
import tectonicus.world.subset.WorldSubset;

public interface Map
{
	String getId();
	
	String getName();
	
	File getWorldDir();
	
	Dimension getDimension();
	
	WorldSubset getWorldSubset();
	
	float getCameraAngleRad();
	int getCameraAngleDeg();
	
	float getCameraElevationRad();
	int getCameraElevationDeg();
	
	int getClosestZoomSize();
	
	boolean useBiomeColours();
	
	PlayerFilter getPlayerFilter();
	SignFilter getSignFilter();
	PortalFilter getPortalFilter();
	ViewFilter getViewFilter();
	ChestFilter getChestFilter();
	
	List<File> getModJars();
	
	int numLayers();
	Layer getLayer(final int index);
	List<Layer> getLayers();
	
	ViewConfig getViewConfig();
	
	NorthDirection getNorthDirection();
	File getCustomCompassRose();
}
