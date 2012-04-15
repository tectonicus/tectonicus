
// TODO:
//
//	- make players/signs/portals/beds initially visible work again 
//
//	- only show signs/beds/players toggle control when there is data to toogle?
//

function size(obj)
{
    var size = 0, key;
    for (key in obj)
    {
        if (obj.hasOwnProperty(key)) size++;
    }
    return size;
};

function createTileUrlFunc(mapId, layerId, imageFormatWithoutDot)
{
	var urlFunc = function(coord, zoom)
	{
		var xBin = coord.x % 16;
		var yBin = coord.y % 16;
		
		var url = mapId+"/"+layerId+"/Zoom"+zoom+"/"+xBin+"/"+yBin+"/tile_"+coord.x+"_"+coord.y+"."+imageFormatWithoutDot;
		return url;
	}
	return urlFunc;
}

// Only use a fraction of the lattitude range so we stay clear of the date line (where things wrap and going weird)
var lattitudeRange = 10;
var compassControl = null;

var viewToggleControl = null;
var signToggleControl = null;
var playerToggleControl = null;
var bedToggleControl = null;
var portalToggleControl = null;
var spawnToggleControl = null;

function main()
{
	var queryParams = getQueryParams();
	var fragmentParams = getFragmentParams();
	
	var mapIds = [];
	for (i in contents)
	{
		var tecMap = contents[i];
		for (j in tecMap.layers)
		{
			var layer = tecMap.layers[j];
			mapIds.push(layer.id);
		}
	}
		
	var myOptions =
	{
		zoom: 0,
		center: new google.maps.LatLng(0, 0),		// initial center (overridden later)
		mapTypeId: google.maps.MapTypeId.ROADMAP,	// initial map id (overridden later)
		mapTypeControl: mapIds.length > 1,				// Only display the map type control if we have multiple maps
		streetViewControl: false,
		overviewMapControl: true,
		mapTypeControlOptions:
		{
			mapTypeIds: mapIds,
			style: google.maps.MapTypeControlStyle.HORIZONTAL_BAR,
			position: google.maps.ControlPosition.RIGHT_TOP
		}
	};
	
	map = new google.maps.Map(document.getElementById("map_canvas"), myOptions);
	
	
	
	for (i in contents)
	{
		var tecMap = contents[i];
		
		for (j in tecMap.layers)
		{
			var layer = tecMap.layers[j];
			var overlayOptions =
			{
				name: layer.name,
				tileSize: new google.maps.Size(tileSize, tileSize),
				maxZoom: maxZoom,
				isPng: layer.isPng,
				getTileUrl: createTileUrlFunc(tecMap.id, layer.id, layer.imageFormat)
			};
			var minecraftMapType = new google.maps.ImageMapType(overlayOptions);
			minecraftMapType.projection = new MinecraftProjection(lattitudeRange, tecMap.worldVectors);
			minecraftMapType.tectonicusMap = tecMap;
			minecraftMapType.layer = layer;
			map.mapTypes.set(layer.id, minecraftMapType);
			
			// Set the starting lat-long pos
			var startPoint = minecraftMapType.projection.worldToMap(tecMap.worldVectors.spawnPosition);
			var startLatLong = minecraftMapType.projection.fromPointToLatLng(startPoint);
			tecMap.viewLatLong = startLatLong; // 'viewLatLong' stores view pos for a given map, so we don't end up looking at nothing when we toggle between terra and nether
		}
	}
	
	
	signWindow = new google.maps.InfoWindow(
	{
		maxWidth: 1500
	});
	
	// Find a default start layer
	var startMap = contents[0];
	var startLayer = startMap.layers[0];
	
	// Try and get a starting view from the fragment params, query params, or fall back to default
	var startView;
	if (size(fragmentParams) > 0)
		startView = findStartView(fragmentParams, startLayer.id, startMap.worldVectors.spawnPosition);
	else
		startView = findStartView(queryParams, startLayer.id, startMap.worldVectors.spawnPosition);
	
	// Set the starting view
	map.setMapTypeId(startView.layerId);
	map.setCenter(startView.latLong);
	map.setZoom(startView.zoom);
	
	// And update the view latLong
	var mapType = map.mapTypes.get( map.getMapTypeId() );
	mapType.tectonicusMap.viewLatLong = startView.latLong;
	
	// Create controls
	
	compassControl = CreateCompassControl(map, mapType.tectonicusMap.id + '/Compass.png');

	viewToggleControl = CreateMarkerToggle(map, 'show views', 'Images/Picture.png', viewMarkers, viewsInitiallyVisible);	
	signToggleControl = CreateMarkerToggle(map, 'show signs', 'Images/Sign.png', signMarkers, signsInitiallyVisible);
	playerToggleControl = CreateMarkerToggle(map, 'show players', 'Images/DefaultPlayer.png', playerMarkers, playersInitiallyVisible);
	bedToggleControl = CreateMarkerToggle(map, 'show beds', 'Images/Bed.png', bedMarkers, bedsInitiallyVisible);
	portalToggleControl = CreateMarkerToggle(map, 'show portals', 'Images/Portal.png', portalMarkers, portalsInitiallyVisible);
	spawnToggleControl = CreateMarkerToggle(map, 'show spawn', 'Images/Spawn.png', spawnMarkers, spawnInitiallyVisible);	

	CreateLinkControl(map);
	
	// Add controls to the map
	
	map.controls[google.maps.ControlPosition.TOP_LEFT].push( compassControl.getDiv() );
	map.controls[google.maps.ControlPosition.RIGHT_TOP].push( CreateHomeControl(map) );
	
	map.controls[google.maps.ControlPosition.BOTTOM_RIGHT].push(portalToggleControl);
	map.controls[google.maps.ControlPosition.BOTTOM_RIGHT].push(bedToggleControl);
	map.controls[google.maps.ControlPosition.BOTTOM_RIGHT].push(playerToggleControl);
	map.controls[google.maps.ControlPosition.BOTTOM_RIGHT].push(viewToggleControl);	
	map.controls[google.maps.ControlPosition.BOTTOM_RIGHT].push(signToggleControl);	
	map.controls[google.maps.ControlPosition.BOTTOM_RIGHT].push(spawnToggleControl);


	// Register these last so that they don't get called while we're still initialising	
	google.maps.event.addListener(map, 'projection_changed', onProjectionChanged);
	google.maps.event.addListener(map, 'maptypeid_changed', onMapTypeChanged);
	
	// Manually call these to set the initial state
	onMapTypeChanged();
	onProjectionChanged();
		
	runGa();
}

spawnMarkers = [];
signMarkers = [];
viewMarkers = [];
playerMarkers = [];
portalMarkers = [];
bedMarkers = [];

function onMapTypeChanged()
{
	var mapType = map.mapTypes.get( map.getMapTypeId() );
	map.setCenter(mapType.tectonicusMap.viewLatLong);
	
	refreshSpawnMarker( spawnToggleControl.checked );
	refreshSignMarkers( signToggleControl.checked );
	refreshViewMarkers( viewToggleControl.checked );
	refreshPlayerMarkers( playerToggleControl.checked );
	refreshBedMarkers( bedToggleControl.checked );
	refreshPortalMarkers( portalToggleControl.checked );
	
	if (compassControl)
		compassControl.setCompassImage( mapType.tectonicusMap.id + '/Compass.png' );
}

function onProjectionChanged()
{
	// Store the previous latLong in the map
	var mapType = map.mapTypes.get( map.getMapTypeId() );
	mapType.tectonicusMap.viewLatLong = map.getCenter();
	
} // end onProjectionChanged callback

function refreshSpawnMarker(markersVisible)
{
	destroyMarkers(spawnMarkers);
	
	var tecMap = getActiveMap();
	var projection = getActiveProjection();
	
	// Spawn marker
	if (showSpawn)
	{
		var point = projection.worldToMap(tecMap.worldVectors.spawnPosition);
		var pos = projection.fromPointToLatLng(point);
				
		var marker = new google.maps.Marker(
		{		
			position: pos,
			map: map, 
			title: 'Spawn point',
			icon: 'Images/Spawn.png',
			optimized: false
		});
		
		// Disable this marker if we don't want signs initially visible						
		if (!markersVisible)
			marker.setMap(null);
			
		spawnMarkers.push(marker);
		
		google.maps.event.addListener(marker, 'click', function()
		{
		//	var worldSizeMb = stats.worldSizeInBytes / 1024 / 1024;
			var peakMemoryMb = stats.peakMemoryBytes / 1024 / 1024;
			
			var statsHtml = '';
		//	if (stats.worldName != '')
		//	{
		//		statsHtml += '<div><center><font size="+2">'+stats.worldName+'</font></center></div>'
		//	}
		//	statsHtml += '<div><center>Map Stats</center></div>';
		//	statsHtml += 'World size: ' + worldSizeMb.toFixed(1) + 'Mb<br/>';
		//	statsHtml += 'Surface area: ' + stats.surfaceArea + 'km&sup2;<br/>';
		//	statsHtml += 'Total chunks: ' + stats.numChunks + '<br/>';
		//	statsHtml += 'Total players: ' + stats.numPlayers + '<br/>';
		//	statsHtml += '<br/>';
			
			statsHtml += '<div><center>Render Stats</center></div>';
			statsHtml += 'Tectonicus version: ' + stats.tectonicusVersion + '<br/>';
			statsHtml += 'Render time: ' + stats.renderTime + '<br/>';
			statsHtml += 'Peak memory usage: ' + peakMemoryMb.toFixed(1) + 'Mb<br/>';
			statsHtml += 'Created on ' + stats.renderedOnDate + '<br/>';
			statsHtml += 'Created at ' + stats.renderedOnTime + '<br/>';
			statsHtml += '<br/>';

			statsHtml += '<div><center>World stats</center></div>';
			statsHtml += 'Players: ' + tecMap.worldStats.numPlayers + '<br/>';
			statsHtml += 'Chunks: ' + tecMap.worldStats.numChunks + '<br/>';
			statsHtml += 'Portals: ' + tecMap.worldStats.numPortals + '<br/>';
			statsHtml += 'Views: ' + tecMap.views.length + '<br/>';
			statsHtml += 'Signs: ' + tecMap.signs.length + '<br/>';
			statsHtml += 'Beds: ' + tecMap.beds.length + '<br/>';
			statsHtml += '<br/>';
			
			statsHtml += '<div><center>Blocks</center></div>';
			for (i in tecMap.blockStats)
			{
				var stat = tecMap.blockStats[i];
				statsHtml += stat.name + ' ' + stat.count + '<br/>';
			}
			
			var options =
			{
				content: statsHtml
			};
			signWindow.close();
			signWindow.setOptions(options);
			signWindow.open(map, this);
		});
	}
}

function refreshSignMarkers(markersVisible)
{
	destroyMarkers(signMarkers);
	
	var tecMap = getActiveMap();
	var projection = getActiveProjection();
	
	// Sign markers
	for (i in tecMap.signs)
	{
		var sign = tecMap.signs[i];
		
		var point = projection.worldToMap(sign.worldPos);
		var pos = projection.fromPointToLatLng(point);
			
		var marker = new google.maps.Marker(
		{		
			position: pos,
			map: map, 
			title: '',
			icon: 'Images/Sign.png',
			optimized: false
		});
		
		// Disable this marker if we don't want signs initially visible						
		if (!markersVisible)
			marker.setMap(null);
		
		marker.sign = sign; // save this ref in the marker so we can fetch it in the bound function below
		
		google.maps.event.addListener(marker, 'click', function()
		{
			var options =
			{
				content: '<pre><center>' + this.sign.text1 + '<br/>' + this.sign.text2 + '<br/>' + this.sign.text3 + '<br/>' + this.sign.text4 + '</center></pre>'
			};
			signWindow.close();
			signWindow.setOptions(options);
			signWindow.open(map, this);
		});
		
		signMarkers.push(marker);
	}
}

function refreshViewMarkers(markersVisible)
{
	destroyMarkers(viewMarkers);
	
	var tecMap = getActiveMap();
	var projection = getActiveProjection();
	
	// View markers
	for (i in tecMap.views)
	{
		var view = tecMap.views[i];
		
		var point = projection.worldToMap(view.worldPos);
		var pos = projection.fromPointToLatLng(point);
			
		var marker = new google.maps.Marker(
		{		
			position: pos,
			map: map, 
			title: '',
			icon: 'Images/Picture.png',
			optimized: false
		});
		
		// Disable this marker if we don't want signs initially visible						
		if (!markersVisible)
			marker.setMap(null);
		
		marker.view = view; // save this ref in the marker so we can fetch it in the bound function below
		
		google.maps.event.addListener(marker, 'click', function()
		{
			var html = '';

			html += '<div>';
			html += '<a href="' + this.view.imageFile + '">';
			html += '<img width="512" height="288" src="' + this.view.imageFile + '"/>';
			html += '</a>';
			html += '</div>';
			
			html += '';
			html += '<center>';
			html += this.view.text;
			html += '</center>';
			html += '';
		
			
			var options =
			{
				content: html
			};
			signWindow.close();
			signWindow.setOptions(options);
			signWindow.open(map, this);
		});
		
		viewMarkers.push(marker);
	}
}


function refreshPlayerMarkers(markersVisible)
{
	destroyMarkers(playerMarkers);

	var tecMap = getActiveMap();
	var projection = getActiveProjection();
	
	// Player markers
	for (i in tecMap.players)
	{
		var player = tecMap.players[i];
		
		player.donation = '';
		
		var point = projection.worldToMap(player.worldPos);
		var pos = projection.fromPointToLatLng(point);
			
		var marker = createPlayerMarker(map, player, pos, signWindow);
		
		// Disable this marker if we don't want signs initially visible						
		if (!markersVisible)
			marker.setMap(null);
			
		playerMarkers.push(marker);
	}	
}

function refreshBedMarkers(markersVisible)
{
	destroyMarkers(bedMarkers);

	var tecMap = getActiveMap();
	var projection = getActiveProjection();
	
	// Bed markers
	for (i in tecMap.beds)
	{
		var bed = tecMap.beds[i];
		
		var point = projection.worldToMap(bed.worldPos);
		var pos = projection.fromPointToLatLng(point);
			
		var marker = new google.maps.Marker(
		{		
			position: pos,
			map: map, 
			title: bed.playerName + "'s bed",
			icon: 'Images/Bed.png',
			optimized: false
		});

		// Disable this marker if we don't want signs initially visible						
		if (!markersVisible)
			marker.setMap(null);
		
		marker.bed = bed; // save this ref in the marker so we can fetch it in the bound function below
		
		google.maps.event.addListener(marker, 'click', function()
		{
			var options =
			{
				content: '<center>' + this.bed.playerName + "'s bed</center>"
			};
			signWindow.close();
			signWindow.setOptions(options);
			signWindow.open(map, this);
		});
		
		bedMarkers.push(marker);
	}
}

function refreshPortalMarkers(markersVisible)
{
	destroyMarkers(portalMarkers);

	var tecMap = getActiveMap();
	var projection = getActiveProjection();
	
	// Portal markers
	for (i in tecMap.portals)
	{
		var portal = tecMap.portals[i];
		
		var point = projection.worldToMap(portal.worldPos);
		var pos = projection.fromPointToLatLng(point);
			
		var marker = new google.maps.Marker(
		{		
			position: pos,
			map: map, 
			title: '',
			icon: 'Images/Portal.png',
			optimized: false
		});
		
		marker.portal = portal; // save this ref in the marker so we can fetch it in the bound function below
		
		google.maps.event.addListener(marker, 'click', function()
		{
			var options =
			{
				content: '<center>Portal</center><br/> position ('+this.portal.worldPos.x+', '+this.portal.worldPos.y+', '+this.portal.worldPos.z+')'
			};
			signWindow.close();
			signWindow.setOptions(options);
			signWindow.open(map, this);
		});
		
		// Disable this marker if we don't want signs initially visible						
		if (!markersVisible)
			marker.setMap(null);
			
		portalMarkers.push(marker);
	}
}

function getActiveLayer()
{
	var mapType = map.mapTypes.get( map.getMapTypeId() );
	var layer = mapType.layer;
	return layer;
}

function getActiveMap()
{
	var mapType = map.mapTypes.get( map.getMapTypeId() );
	var tecMap = mapType.tectonicusMap;
	return tecMap;
}

function getActiveProjection()
{
	var mapType = map.mapTypes.get( map.getMapTypeId() );
	var projection = mapType.projection;
	return projection;
}

function destroyMarkers(markers)
{
	for (i in markers)
	{
		var marker = markers[i];
		marker.setMap(null);
		
		// remove marker from map?
		// ..
	}
	
	markers.length = 0; // todo: check this works		
}

function ViewPos(layerId, worldPos, zoom, latLong)
{
	this.layerId = layerId;
	this.worldPos = worldPos;
	this.zoom = zoom;
	this.latLong = latLong;
}

function findStartView(params, defaultLayerId, defaultSpawnPos)
{
	var queryLayerId = defaultLayerId;
	var queryPos = new WorldCoord(defaultSpawnPos.x, defaultSpawnPos.y, defaultSpawnPos.z);
	var queryZoom = 0;
	
	if (params.hasOwnProperty('layerId'))
	{
		queryLayerId = params['layerId'];
	}
	
	if (params.hasOwnProperty('worldX')
		&& params.hasOwnProperty('worldY')
		&& params.hasOwnProperty('worldZ'))
	{
		queryPos.x = parseInt( params['worldX'] );
		queryPos.y = parseInt( params['worldY'] );
		queryPos.z = parseInt( params['worldZ'] );
	}
	
	if (params.hasOwnProperty('zoom'))
	{
		queryZoom = parseInt( params['zoom'] );
	}
	
	if (queryZoom < 0)
		queryZoom = 0;
	if (queryZoom > maxZoom)
		queryZoom = maxZoom;

	var mapType = map.mapTypes.get(queryLayerId);
	var projection = mapType.projection;
	
	var startPoint = projection.worldToMap(queryPos);
	var startLatLong = projection.fromPointToLatLng(startPoint);
	
	return new ViewPos(queryLayerId, queryPos, queryZoom, startLatLong);
}
