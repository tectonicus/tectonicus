/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import tectonicus.configuration.filter.BeaconFilter;
import tectonicus.configuration.filter.ChestFilter;
import tectonicus.configuration.filter.PlayerFilter;
import tectonicus.configuration.filter.PortalFilter;
import tectonicus.configuration.filter.SignFilter;
import tectonicus.configuration.filter.ViewFilter;
import tectonicus.util.Vector3l;
import tectonicus.world.subset.WorldSubset;

import java.io.File;
import java.util.List;

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
	boolean isSmoothLit();

	PlayerFilter getPlayerFilter();
	SignFilter getSignFilter();
	PortalFilter getPortalFilter();
	ViewFilter getViewFilter();
	ChestFilter getChestFilter();
	BeaconFilter getBeaconFilter();
	
	List<File> getModJars();
	
	int numLayers();
	Layer getLayer(final int index);
	List<Layer> getLayers();
	
	ViewConfig getViewConfig();
	
	NorthDirection getNorthDirection();
	File getCustomCompassRose();

	Vector3l getOrigin();
}
