
let mymap = L.map('map', {crs: L.CRS.Simple, minZoom: 0, maxZoom: maxZoom, attributionControl: false}).setView([0, 0], 0)
let tileLayers = new Map();
let activeBaseLayer = null;

function size(obj) {
	var size = 0, key;
	for (key in obj) {
		if (obj.hasOwnProperty(key)) size++;
	}
	return size;
}

var compassControl = null;
var viewToggleControl = null;
var signToggleControl = null;
var playerToggleControl = null;
var bedToggleControl = null;
var portalToggleControl = null;
var spawnToggleControl = null;
var chestToggleControl = null;

function main()
{
	let queryParams = getQueryParams();
	let fragmentParams = getFragmentParams();
	
	L.TileLayer.Tectonicus = L.TileLayer.extend({
        getTileUrl: function(coords) {
            let xBin = coords.x % 16;
            let yBin = coords.y % 16;

            return this.mapId + "/" + this.layerId + "/Zoom"+coords.z
                +"/"+xBin+"/"+yBin+"/tile_"+coords.x+"_"+coords.y+"."+this.imageFormat;
        },
        getAttribution: function() {
            return '<a href="https://github.com/tectonicus/tectonicus">Tectonicus</a> - <a tabindex="0" id="mapInfo">' + this.mapName + '</a>';
        },
        initialize: function(mapId, layerId, imageFormat, mapName, backgroundColor, signs, players, chests, views, portals, beds, worldVectors, projection,
            blockStats, worldStats, viewPosition) {
            this.mapId = mapId;
            this.layerId = layerId;
            this.imageFormat = imageFormat;
            this.mapName = mapName;
            this.backgroundColor = backgroundColor;
            this.signs = signs;
            this.players = players;
            this.chests = chests;
            this.views = views;
            this.portals = portals;
            this.beds = beds;
            this.worldVectors = worldVectors;
            this.projection = projection;
            this.blockStats = blockStats;
            this.worldStats = worldStats;
			this.viewPosition = viewPosition;
        }
    });

    let baseMaps = {}
    for (let i = 0; i < contents.length; i++) {
        let tecMap = contents[i];
        for (let j = 0; j < tecMap.layers.length; j++) {
            let layer = tecMap.layers[j];
            let projection = new MinecraftProjection(tecMap.worldVectors);
			// 'startPosition' stores view pos for a given layer, so we remember where we were when switching layers
			let startPosition = new ViewPos(layer.id, tecMap.worldVectors.startView, 0, projection.worldToMap(tecMap.worldVectors.startView));

            let tileLayer = new L.TileLayer.Tectonicus(tecMap.id, layer.id, layer.imageFormat, tecMap.name, layer.backgroundColor, tecMap.signs, tecMap.players,
                tecMap.chests, tecMap.views, tecMap.portals, tecMap.beds, tecMap.worldVectors, projection, tecMap.blockStats, tecMap.worldStats, startPosition);

            if (baseMaps.hasOwnProperty(tecMap.name + " - " + layer.name)) {
                baseMaps[tecMap.name + " - " + layer.name + j] = tileLayer;  //A hack to handle duplicate layer names in the layer control
            } else {
                baseMaps[tecMap.name + " - " + layer.name] = tileLayer;
            }
            tileLayers.set(layer.id, tileLayer);
        }
    }

	if (tileLayers.size > 1) {
        L.control.layers(baseMaps).addTo(mymap);
    }

	// Try and get a starting view from the fragment params, query params, or fall back to default
	let defaultLayer = tileLayers.get("LayerA");
	let startView;
	if (size(fragmentParams) > 0) {
		startView = findStartView(fragmentParams, defaultLayer.layerId, defaultLayer.viewPosition.worldPos);
	} else {
		startView = findStartView(queryParams, defaultLayer.layerId, defaultLayer.viewPosition.worldPos);
	}

	// Set the starting view
	let startLayer = tileLayers.get(startView.layerId);
	activeBaseLayer = startLayer;
	startLayer.addTo(mymap);
	mymap.setView(startView.startPoint, startView.zoom);

	// And store the updated start point in the layer
	startLayer.viewPosition = startView;

	// Create controls
	compassControl = CreateCompassControl(startLayer.mapId + '/Compass.png');
    viewToggleControl = CreateToggleControl('views', 'Images/Picture.png', viewMarkers, viewsInitiallyVisible);
    signToggleControl = CreateToggleControl('signs', 'Images/Sign.png', signMarkers, signsInitiallyVisible);
    playerToggleControl = CreateToggleControl('players', 'Images/PlayerIcons/Tectonicus_Default_Player_Icon.png', playerMarkers, playersInitiallyVisible);
    bedToggleControl = CreateToggleControl('beds', 'Images/Bed.png', bedMarkers, bedsInitiallyVisible);
    portalToggleControl = CreateToggleControl('portals', 'Images/Portal.png', portalMarkers, portalsInitiallyVisible);
    spawnToggleControl = CreateToggleControl('spawn', 'Images/Spawn.png', spawnMarkers, spawnInitiallyVisible);
    chestToggleControl = CreateToggleControl('chests', 'Images/Chest.png', chestMarkers, false);

	//CreateLinkControl(map);

	// Add controls to the map
	L.control.attribution({position: 'bottomleft'}).addTo(mymap);

	// Register these last so that they don't get called while we're still initialising
	mymap.on('baselayerchange', onBaseLayerChange);
	mymap.on('zoomend', onProjectionChanged);
    mymap.on('moveend', onProjectionChanged);

	// Manually fire this event to set the initial state
    mymap.fireEvent('baselayerchange', {layer: startLayer});
}

spawnMarkers = [];
signMarkers = [];
viewMarkers = [];
playerMarkers = [];
portalMarkers = [];
bedMarkers = [];
chestMarkers = [];

function onBaseLayerChange(e) {
    activeBaseLayer = e.layer;

	mymap.setView(e.layer.viewPosition.startPoint, e.layer.viewPosition.zoom);

	changeBackgroundColor(e.layer.backgroundColor);

    spawnToggleControl.remove();
    if (e.layer.worldVectors.hasOwnProperty('spawnPosition')) {
        mymap.addControl(spawnToggleControl);
    }

    signToggleControl.remove();
	if (e.layer.signs.length != 0) {
		mymap.addControl(signToggleControl);
	}

	viewToggleControl.remove();
	if (e.layer.views.length != 0) {
		mymap.addControl(viewToggleControl);
	}

	playerToggleControl.remove();
	if (e.layer.players.length != 0) {
		mymap.addControl(playerToggleControl);
	}

	portalToggleControl.remove();
	if (e.layer.portals.length != 0) {
		mymap.addControl(portalToggleControl);
	}

	bedToggleControl.remove();
	if (e.layer.beds.length != 0) {
		mymap.addControl(bedToggleControl);
	}

	chestToggleControl.remove();
	if (e.layer.chests.length != 0) {
		mymap.addControl(chestToggleControl);
	}

	// Add tooltips to the toggle buttons
	tippy('.button', {
        content(reference) {
          const title = reference.getAttribute('title');
          reference.removeAttribute('title');
          if (reference.checked) {
            return 'Hide ' + title;
          } else {
              return 'Show ' + title;
          }
        },
        placement: 'left',
        theme: 'light',
    });

	refreshSpawnMarker(e.layer, spawnInitiallyVisible);
	refreshSignMarkers(e.layer, signsInitiallyVisible);
	refreshViewMarkers(e.layer, viewsInitiallyVisible);
	refreshPlayerMarkers(e.layer, playersInitiallyVisible);
	refreshPortalMarkers(e.layer, portalsInitiallyVisible);
	refreshBedMarkers(e.layer, bedsInitiallyVisible);
	refreshChestMarkers(e.layer, false);

	compassControl.remove();
	compassControl = CreateCompassControl(e.layer.mapId + '/Compass.png');
	mymap.addControl(compassControl);

	// Add map info popup
    let peakMemoryMb = stats.peakMemoryBytes / 1024 / 1024;
    let statsHtml = '';
    statsHtml += '<h4 class="center">Map Info</h4>';
    statsHtml += '<div class="center">Render Stats</div>';
    statsHtml += 'Tectonicus version: ' + stats.tectonicusVersion + '<br/>';
    statsHtml += 'Render time: ' + stats.renderTime + '<br/>';
    statsHtml += 'Peak memory usage: ' + peakMemoryMb.toFixed(1) + 'Mb<br/>';
    statsHtml += 'Created on ' + stats.renderedOnDate + '<br/>';
    statsHtml += 'Created at ' + stats.renderedOnTime + '<br/>';
    statsHtml += '<br/>';

    statsHtml += '<div class="center">World stats</div>';
    statsHtml += 'Players: ' + e.layer.worldStats.numPlayers + '<br/>';
    statsHtml += 'Chunks: ' + e.layer.worldStats.numChunks + '<br/>';
    statsHtml += 'Portals: ' + e.layer.worldStats.numPortals + '<br/>';
    statsHtml += 'Views: ' + e.layer.views.length + '<br/>';
    statsHtml += 'Signs: ' + e.layer.signs.length + '<br/>';
    statsHtml += 'Player Beds: ' + e.layer.beds.length + '<br/>';

    //Do we care about the block counts anymore?
//        statsHtml += '<div class="center">Blocks</div>';
//        for (i in layer.blockStats) {
//            let stat = layer.blockStats[i];
//            statsHtml += stat.name + ' ' + stat.count + '<br/>';
//        }

    tippy('#mapInfo', {
        content: statsHtml,
        allowHTML: true,
        interactive: true,
        theme: 'light',
        maxWidth: 500
    });
}

function changeBackgroundColor(color) {
	let mapContainer = document.querySelector("#map");
	mapContainer.style.backgroundColor = color;
}

function onProjectionChanged(e) {
	// Store the previous coords in the layer
	activeBaseLayer.viewPosition.startPoint = mymap.getCenter();
	activeBaseLayer.viewPosition.zoom = mymap.getZoom();
}

function refreshSpawnMarker(layer, markersVisible) {
	destroyMarkers(spawnMarkers);

	// Spawn marker
	if (showSpawn && layer.worldVectors.hasOwnProperty('spawnPosition')) {
		let spawn = layer.projection.worldVectors.spawnPosition;
		let point = layer.projection.worldToMap(spawn);

		let myIcon = L.icon({
			iconUrl: 'Images/Spawn.png',
			// iconSize: [30, 30],
			iconAnchor: [17, 20],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});

        let marker = L.marker(point, { icon: myIcon }).bindPopup('<p class="center">' + spawn.x + ', ' + spawn.y + ', ' + spawn.z + '</p>').bindTooltip("Spawn Point");

		// Add marker to map if spawn is initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		spawnMarkers.push(marker);
	}
}

function refreshSignMarkers(layer, markersVisible) {
	destroyMarkers(signMarkers);

	// Sign markers
	for (i in layer.signs) {
		let sign = layer.signs[i];
		let point = layer.projection.worldToMap(sign.worldPos);

		let myIcon = L.icon({
			iconUrl: 'Images/Sign.png',
			// iconSize: [30, 30],
			iconAnchor: [17, 20],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});

		let marker = L.marker(point, { icon: myIcon }).bindPopup(
			'<pre><center>' + sign.text1 + '<br/>' + sign.text2 + '<br/>' + sign.text3 + '<br/>' + sign.text4 + '</center></pre>');

		// Add marker to map if signs are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		signMarkers.push(marker);
	}
}

function refreshViewMarkers(layer, markersVisible) {
	destroyMarkers(viewMarkers);

	// View markers
	for (i in layer.views) {
		let view = layer.views[i];

		let point = layer.projection.worldToMap(view.worldPos);

		let icon = L.icon({
			iconUrl: 'Images/Picture.png',
			// iconSize: [30, 30],
			iconAnchor: [17, 20],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});

		let html = '';
		html += '<div>';
		html += '<a href="' + view.imageFile + '">';
		html += '<img width="512" height="288" src="' + view.imageFile + '"/>';
		html += '</a>';
		html += '</div>';
		html += '';
		html += '<center>';
		html += view.text;
		html += '</center>';
		html += '';

		let marker = L.marker(point, { icon: icon }).bindPopup(html, {maxWidth: "auto"});

		// Add marker to map if chests are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		viewMarkers.push(marker);
	}
}

function loadIcon(src, player, pos, markersVisible) {
	var icon = new Image();
	icon.onload = function () {
		player.icon = src;
		var marker = createPlayerMarker(player, pos);

		// Add marker to map if players are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		playerMarkers.push(marker);
	};
	icon.onerror = function () {
		player.icon = player.icon = "Images/PlayerIcons/Tectonicus_Default_Player_Icon.png";
		var marker = createPlayerMarker(player, pos);

		// Add marker to map if players are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		playerMarkers.push(marker);
	};
	icon.src = src;
}

function refreshPlayerMarkers(layer, markersVisible) {
	destroyMarkers(playerMarkers);

	// Player markers
	for (i in layer.players) {
		let player = layer.players[i];

		player.donation = '';

		let point = layer.projection.worldToMap(player.worldPos);

		loadIcon("Images/PlayerIcons/" + player.name + ".png", player, point, markersVisible);
	}
}

function refreshBedMarkers(layer, markersVisible) {
	destroyMarkers(bedMarkers);

	// Bed markers
	for (i in layer.beds) {
		let bed = layer.beds[i];
		let point = layer.projection.worldToMap(bed.worldPos);

		let icon = L.icon({
			iconUrl: 'Images/Bed.png',
			// iconSize: [30, 30],
			iconAnchor: [17, 20],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});

		let marker = L.marker(point, { icon: icon }).bindPopup('<center>' + bed.playerName + "'s bed</center>");

		// Add marker to map if beds are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		bedMarkers.push(marker);
	}
}

function refreshPortalMarkers(layer, markersVisible) {
	destroyMarkers(portalMarkers);

	// Portal markers
	for (i in layer.portals) {
		let portal = layer.portals[i];

		let point = layer.projection.worldToMap(portal.worldPos);

		let icon = L.icon({
			iconUrl: 'Images/Portal.png',
			// iconSize: [30, 30],
			iconAnchor: [17, 20],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});

		let marker = L.marker(point, { icon: icon }).bindPopup(
			'<div style=\"text-align:center\">Portal</div><br/> position (' + portal.worldPos.x + ', ' + portal.worldPos.y + ', ' + portal.worldPos.z + ')');

		// Add marker to map if chests are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		portalMarkers.push(marker);
	}
}

function refreshChestMarkers(layer, markersVisible) {
	destroyMarkers(chestMarkers);

	// Chest markers
	for (i in layer.chests) {
		let point = layer.projection.worldToMap(layer.chests[i].worldPos);

		let icon = L.icon({
			iconUrl: 'Images/Chest.png',
			// iconSize: [30, 30],
			iconAnchor: [17, 20],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});

		let marker = L.marker(point, { icon: icon }).bindPopup('<img width="300" height="131" src="Images/SmallChest.png"/>');

		// Add marker to map if chests are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		chestMarkers.push(marker);
	}
}

function destroyMarkers(markers) {
	for (i in markers) {
		let marker = markers[i];
		marker.remove();
	}

	markers.length = 0;
}

function ViewPos(layerId, worldPos, zoom, startPoint) {
	this.layerId = layerId;
	this.worldPos = worldPos;
	this.zoom = zoom;
	this.startPoint = startPoint;
}

function findStartView(params, defaultLayerId, defaultSpawnPos) {
	let queryLayerId = defaultLayerId;
	let queryPos = new WorldCoord(defaultSpawnPos.x, defaultSpawnPos.y, defaultSpawnPos.z);
	let queryZoom = 0;

	if (params.hasOwnProperty('layerId')) {
		queryLayerId = params['layerId'];
	}

	if (params.hasOwnProperty('worldX')
		&& params.hasOwnProperty('worldY')
		&& params.hasOwnProperty('worldZ')) {
		queryPos.x = parseInt(params['worldX']);
		queryPos.y = parseInt(params['worldY']);
		queryPos.z = parseInt(params['worldZ']);
	}

	if (params.hasOwnProperty('zoom')) {
		queryZoom = parseInt(params['zoom']);
	}

	if (queryZoom < 0)
		queryZoom = 0;
	if (queryZoom > maxZoom)
		queryZoom = maxZoom;

	let layer = tileLayers.get(queryLayerId);
	let startPoint = layer.projection.worldToMap(queryPos);

	return new ViewPos(queryLayerId, queryPos, queryZoom, startPoint);
}

/*
 * Workaround for 1px lines appearing in some browsers due to fractional transforms
 * and resulting anti-aliasing.
 * https://github.com/Leaflet/Leaflet/issues/3575
 */
(function(){
    var originalInitTile = L.GridLayer.prototype._initTile
    L.GridLayer.include({
        _initTile: function (tile) {
            originalInitTile.call(this, tile);

            var tileSize = this.getTileSize();

            tile.style.width = tileSize.x + 1 + 'px';
            tile.style.height = tileSize.y + 1 + 'px';
        }
    });
})()

window.onload = main;
