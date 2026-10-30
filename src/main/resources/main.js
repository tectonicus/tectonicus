
let mymap = L.map('map', {crs: L.CRS.Simple, minZoom: 0, maxZoom: maxZoom, attributionControl: false});
let tileLayers = new Map();
let urlLayers = new Map();
let activeBaseLayer = null;
let worldLayers = new Map();
let selectedWorldLayers = new Map();
let markerVisibilityOverrides = new Map();
let worldControl = null;
let layerControlWorldId = null;
let changingBaseLayer = false;
let mapInfoTippy = null;
let mapInfoContent = '';

function bindMapInfoTippy() {
    let mapInfoLink = document.querySelector('.leaflet-control-attribution #mapInfo');
    if (mapInfoLink === null) {
        return;
    }

    if (mapInfoTippy === null || mapInfoTippy.reference !== mapInfoLink) {
        if (mapInfoTippy !== null) {
            mapInfoTippy.destroy();
        }

        mapInfoTippy = tippy(mapInfoLink, {
            content: mapInfoContent,
            allowHTML: true,
            interactive: true,
            theme: 'light',
            maxWidth: 500
        });
    } else {
        mapInfoTippy.setContent(mapInfoContent);
    }
}

function size(obj) {
	var size = 0, key;
	for (key in obj) {
		if (obj.hasOwnProperty(key)) size++;
	}
	return size;
}

var layerControl = null;
var compassControl = null;
var viewToggleControl = null;
var signToggleControl = null;
var playerToggleControl = null;
var bedToggleControl = null;
var respawnAnchorToggleControl = null;
var beaconToggleControl = null;
var portalToggleControl = null;
var spawnToggleControl = null;
var containerToggleControl = null;

async function mainAsync()
{        
        window.onmousemove = function (e) {
                // Set handler to update position of item description tooltip to follow the mouse cursor
                updateContainerItemTooltipPosition(e);
        };
    
        // Initialize containers asynchronously, so that main initialization can continue in the meantime
        const containersMainPromise = containersMainAsync();
        
	let queryParams = getQueryParams();
	let fragmentParams = getFragmentParams();
	
	L.TileLayer.Tectonicus = L.TileLayer.extend({
        _isValidTile: function(coords) {
            if (!L.TileLayer.prototype._isValidTile.call(this, coords)) {
                return false;
            }
            // Older contents.js files have no index; keep them compatible with this viewer.
            if (!this.tileIndex) {
                return true;
            }
            const rows = this.tileIndex[coords.z];
            const ranges = rows && rows[coords.y];
            if (!ranges) {
                return false;
            }
            // Rows contain sorted, inclusive [start, end] pairs, preserving holes in coverage.
            let low = 0;
            let high = ranges.length / 2 - 1;
            while (low <= high) {
                const middle = Math.floor((low + high) / 2);
                if (coords.x < ranges[middle * 2]) {
                    high = middle - 1;
                } else if (coords.x > ranges[middle * 2 + 1]) {
                    low = middle + 1;
                } else {
                    return true;
                }
            }
            return false;
        },
        getTileUrl: function(coords) {
            let xBin = coords.x % 16;
            let yBin = coords.y % 16;

            return this.mapId + "/" + this.directoryName + "/Zoom"+coords.z
                +"/"+xBin+"/"+yBin+"/tile_"+coords.x+"_"+coords.y+"."+this.imageFormat;
        },
        getAttribution: function() {
            return '<a href="https://github.com/tectonicus/tectonicus">Tectonicus</a> - <a tabindex="0" id="mapInfo">' + escapeHtml(this.mapName) + '</a>';
        },
        initialize: function(mapId, layerId, dimension, imageFormat, mapName, backgroundColor, signs, players, containers, views, portals, beds, anchors, beacons, worldVectors, projection,
            blockStats, worldStats, viewPosition, controlState, tileIndex) {
                this.mapId = mapId;
                this.layerId = layerId;
                this.directoryName = layerId;
                this.dimension = dimension;
                this.imageFormat = imageFormat;
                this.mapName = mapName;
                this.backgroundColor = backgroundColor;
                this.signs = signs;
                this.players = players;
                this.containers = containers;
                this.views = views;
                this.portals = portals;
                this.beds = beds;
                this.anchors = anchors;
                this.beacons = beacons;
                this.worldVectors = worldVectors;
                this.projection = projection;
                this.blockStats = blockStats;
                this.worldStats = worldStats;
                this.viewPosition = viewPosition;
                this.controlState = normalizeContainerVisibility(controlState);
                this.tileIndex = tileIndex;
            }
        });

        let dimensionStates = new Map();
        for (let i = 0; i < contents.length; i++) {
            let tecMap = contents[i];
            for (let j = 0; j < tecMap.layers.length; j++) {
                let layer = tecMap.layers[j];
                let projection = new MinecraftProjection(tecMap.worldVectors);
                            // 'startPosition' stores view pos for a given layer, so we remember where we were when switching layers
                            let startPosition = new ViewPos(layer.id, tecMap.worldVectors.startView, 0, projection.worldToMap(tecMap.worldVectors.startView));

                let tileLayer = new L.TileLayer.Tectonicus(tecMap.outputId || tecMap.id, layer.id, layer.dimension, layer.imageFormat, tecMap.name, layer.backgroundColor, tecMap.signs, tecMap.players,
                    tecMap.containers ?? tecMap.chests ?? [], tecMap.views, tecMap.portals, tecMap.beds, tecMap.respawnAnchors, tecMap.beacons, tecMap.worldVectors, projection, tecMap.blockStats, tecMap.worldStats, startPosition, tecMap.controlState || controlState, layer.tileIndex);
                tileLayer.worldId = tecMap.worldId || 'World0';
				tileLayer.dimensionId = tecMap.dimensionId;
                tileLayer.worldName = tecMap.worldName || 'World';
                tileLayer.images = tecMap.images || 'Images/';
                tileLayer.localizationsUrl = tecMap.localizations || 'Scripts/localizations.json';
                tileLayer.maxZoom = tecMap.maxZoom ?? maxZoom;
                L.setOptions(tileLayer, {maxZoom: tileLayer.maxZoom});
                if (!dimensionStates.has(tecMap.id)) {
                    dimensionStates.set(tecMap.id, {...tileLayer.controlState});
                }
                tileLayer.controlState = dimensionStates.get(tecMap.id);
                tileLayer.label = tecMap.name + " - " + layer.name;
                tileLayer.layerName = layer.name;
                tileLayer.directoryName = layer.directory || layer.id;
                tileLayer.compassUrl = tecMap.compass || tecMap.id + '/Compass.png';
                if (!worldLayers.has(tileLayer.worldId)) {
                    worldLayers.set(tileLayer.worldId, []);
                }
                worldLayers.get(tileLayer.worldId).push(tileLayer);

                tileLayers.set(layer.id, tileLayer);
            }
        }

        assignLayerUrlIds();

        // Try and get a starting view from the fragment params, query params, or fall back to default
        let defaultLayer = tileLayers.values().next().value;
        if (!defaultLayer) {
            return;
        }
        let startView;
        if (size(fragmentParams) > 0) {
            startView = findStartView(fragmentParams, defaultLayer.layerId);
        } else {
            startView = findStartView(queryParams, defaultLayer.layerId);
        }

        // Add the starting layer to the map
        let startLayer = tileLayers.get(startView.layerId);
        activeBaseLayer = startLayer;
        // Register attribution before layers so Leaflet also observes removal of the first layer.
        L.control.attribution({position: 'bottomleft'}).setPrefix('<a href="http://www.leafletjs.com">Leaflet</a>').addTo(mymap);
        mymap.setView(startView.startPoint, startView.zoom);
        startLayer.addTo(mymap);
        // Store the updated start point in the layer
        startLayer.viewPosition = startView;

        // Create controls
        if (worldLayers.size > 1) {
            worldControl = CreateWorldControl(worldLayers, startLayer.worldId, switchWorld);
            worldControl.addTo(mymap);
        }
        updateWorldLayerControl(startLayer);
        CreateResetViewButton(mymap);
	    compassControl = CreateCompassControl(startLayer.compassUrl);
        viewToggleControl = CreateToggleControl('views', 'Picture.png', viewMarkers, viewsInitiallyVisible, 'viewControlChecked');
        signToggleControl = CreateToggleControl('signs', 'Sign.png', signMarkers, signsInitiallyVisible, 'signControlChecked');
        playerToggleControl = CreateToggleControl('players', 'PlayerIcons/Tectonicus_Default_Player_Icon.png', playerMarkers, playersInitiallyVisible, 'playerControlChecked');
        bedToggleControl = CreateToggleControl('beds', 'Bed.png', bedMarkers, bedsInitiallyVisible, 'bedControlChecked');
        respawnAnchorToggleControl = CreateToggleControl('respawn anchors', 'RespawnAnchor.png', respawnAnchorMarkers, respawnAnchorsInitiallyVisible, 'respawnAnchorControlChecked');
        beaconToggleControl = CreateToggleControl('beacons', 'beacon.png', beaconMarkers, beaconsInitiallyVisible, 'beaconControlChecked');
        portalToggleControl = CreateToggleControl('portals', 'Portal.png', portalMarkers, portalsInitiallyVisible, 'portalControlChecked');
        spawnToggleControl = CreateToggleControl('spawn', 'SpawnCompass.png', spawnMarkers, spawnInitiallyVisible, 'spawnControlChecked');
        containerToggleControl = CreateToggleControl('containers', 'Chest.png', containerMarkers, containersInitiallyVisible, 'containerControlChecked');

        // Rebind the map information tooltip when attribution changes with the layer.
        let attributionContainer = mymap.attributionControl.getContainer();
        new MutationObserver(bindMapInfoTippy).observe(attributionContainer, {childList: true});

        // We have done all we can. Now we have to await the containers initialization
        await containersMainPromise;

	// Register these last so that they don't get called while we're still initializing
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
respawnAnchorMarkers = [];
beaconMarkers = [];
containerMarkers = [];

function onBaseLayerChange(e) {
    changingBaseLayer = true;
    const targetView = {...e.layer.viewPosition};
    activeBaseLayer = e.layer;
    controlState = {...e.layer.controlState, ...markerVisibilityOverrides.get(e.layer.worldId)};
    selectedWorldLayers.set(e.layer.worldId, e.layer.layerId);
    if (mymap.setMaxZoom) {
        mymap.setMaxZoom(e.layer.maxZoom ?? maxZoom);
    }
    updateWorldLayerControl(e.layer);
    if (worldControl) {
        worldControl.setWorld(e.layer.worldId);
    }

	mymap.setView(targetView.startPoint, targetView.zoom, {animate: false});
    changingBaseLayer = false;

	changeBackgroundColor(e.layer.backgroundColor);
    const mapStyle = document.querySelector('#map').style;
    mapStyle.setProperty('--minecraft-font', 'url("' + worldImageUrl('Font.png', e.layer) + '")');
    mapStyle.setProperty('--enchanted-glint', 'url("' + worldImageUrl('EnchantedGlint.png', e.layer) + '")');

    spawnToggleControl.remove();
    if (e.layer.worldVectors.hasOwnProperty('spawnPosition') && e.layer.dimension !== "NETHER") {
        mymap.addControl(spawnToggleControl);
        spawnToggleControl.setChecked(controlState.spawnControlChecked);
    }

    signToggleControl.remove();
    if (e.layer.signs.length != 0) {
        mymap.addControl(signToggleControl);
        signToggleControl.setChecked(controlState.signControlChecked);
    }

    viewToggleControl.remove();
    if (e.layer.views.length != 0) {
        mymap.addControl(viewToggleControl);
        viewToggleControl.setChecked(controlState.viewControlChecked);
    }

    playerToggleControl.remove();
    if (e.layer.players.length != 0) {
        mymap.addControl(playerToggleControl);
        playerToggleControl.setChecked(controlState.playerControlChecked);
    }

    portalToggleControl.remove();
    if (e.layer.portals.length != 0) {
        mymap.addControl(portalToggleControl);
        portalToggleControl.setChecked(controlState.portalControlChecked);
    }

    bedToggleControl.remove();
    if (e.layer.beds.length != 0) {
        mymap.addControl(bedToggleControl);
        bedToggleControl.setChecked(controlState.bedControlChecked);
    }

    respawnAnchorToggleControl.remove();
    if (e.layer.anchors.length != 0) {
        mymap.addControl(respawnAnchorToggleControl);
        respawnAnchorToggleControl.setChecked(controlState.respawnAnchorControlChecked);
    }

    beaconToggleControl.remove();
    if (e.layer.beacons.length != 0) {
        mymap.addControl(beaconToggleControl);
        beaconToggleControl.setChecked(controlState.beaconControlChecked);
    }

    containerToggleControl.remove();
    if (e.layer.containers.length != 0) {
        mymap.addControl(containerToggleControl);
        containerToggleControl.setChecked(controlState.containerControlChecked);
    }

    // Marker controls are rebuilt on layer changes; keep navigation below their row.
    for (const navigationControl of [worldControl, layerControl]) {
        if (navigationControl) {
            L.DomUtil.toFront(navigationControl.getContainer());
        }
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

	if (e.layer.dimension === "NETHER") {
        destroyMarkers(spawnMarkers);
    } else {
        refreshSpawnMarker(e.layer, controlState.spawnControlChecked);
    }
    refreshSignMarkers(e.layer, controlState.signControlChecked);
    refreshViewMarkers(e.layer, controlState.viewControlChecked);
    refreshPlayerMarkers(e.layer, controlState.playerControlChecked);
    refreshPortalMarkers(e.layer, controlState.portalControlChecked);
    refreshBedMarkers(e.layer, controlState.bedControlChecked);
    refreshRespawnAnchorMarkers(e.layer, controlState.respawnAnchorControlChecked);
    refreshBeaconMarkers(e.layer, controlState.beaconControlChecked);
    refreshContainerMarkers(e.layer, controlState.containerControlChecked);

	compassControl.remove();
	compassControl = CreateCompassControl(e.layer.compassUrl);
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

    mapInfoContent = statsHtml;
    bindMapInfoTippy();
    onProjectionChanged();
}


// Normalize old map exports at the boundary; the viewer uses container names internally.
function normalizeContainerVisibility(state = {}) {
    const result = {...state};
    result.containerControlChecked = state.containerControlChecked ?? state.chestControlChecked
        ?? (typeof containersInitiallyVisible === 'undefined' ? false : containersInitiallyVisible);
    delete result.chestControlChecked;
    return result;
}

function setMarkerVisibility(key, visible) {
    controlState[key] = visible;
    if (!markerVisibilityOverrides.has(activeBaseLayer.worldId)) {
        markerVisibilityOverrides.set(activeBaseLayer.worldId, {});
    }
    markerVisibilityOverrides.get(activeBaseLayer.worldId)[key] = visible;
}

function updateWorldLayerControl(layer) {
    if (layerControlWorldId === layer.worldId) {
        return;
    }
    if (layerControl) {
        layerControl.remove();
        layerControl = null;
    }
    layerControlWorldId = layer.worldId;
    const layers = worldLayers.get(layer.worldId) || [];
    if (layers.length > 1) {
        const baseMaps = Object.create(null);
        for (const candidate of layers) {
            let label = candidate.label;
            let suffix = 2;
            while (Object.hasOwn(baseMaps, escapeHtml(label))) {
                label = candidate.label + ' (' + suffix++ + ')';
            }
            baseMaps[escapeHtml(label)] = candidate;
        }
        layerControl = L.control.layers(baseMaps).addTo(mymap);
    }
}

// Reserve unsuffixed names first so a duplicate "Day" cannot take an existing "Day1".
function uniqueUrlNames(names, fallback) {
    const bases = names.map(name => String(name || '').normalize('NFKD')
        .replace(/[\u0300-\u036f]/g, '').toLowerCase()
        .replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '') || fallback);
    const reserved = new Set(bases);
    const used = new Set();
    return bases.map(base => {
        let name = base;
        let suffix = 1;
        while (used.has(name)) {
            do { name = base + suffix++; } while (reserved.has(name));
        }
        used.add(name);
        return name;
    });
}

function assignLayerUrlIds() {
    const worlds = [...worldLayers.values()];
    const worldNames = uniqueUrlNames(worlds.map(layers => layers[0].worldName), 'world');
    worlds.forEach((layers, worldIndex) => {
        const layerNames = uniqueUrlNames(layers.map(layer => layer.layerName), 'layer');
        layers.forEach((layer, layerIndex) => {
            layer.urlId = worldNames[worldIndex] + '.' + layerNames[layerIndex];
            urlLayers.set(layer.urlId, layer);
        });
    });
}

function escapeHtml(text) {
    return String(text).replace(/[&<>"']/g, character => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    })[character]);
}

function selectBaseLayer(layer) {
    if (layer === activeBaseLayer) {
        return;
    }
    mymap.removeLayer(activeBaseLayer);
    layer.addTo(mymap);
    // Leaflet only emits this event for layers registered in its current control.
    if (activeBaseLayer !== layer) {
        onBaseLayerChange({layer});
    }
}

function switchWorld(worldId) {
    const layers = worldLayers.get(worldId);
    if (!layers || layers.length === 0) {
        return;
    }
    const remembered = tileLayers.get(selectedWorldLayers.get(worldId));
    selectBaseLayer(remembered || layers[0]);
}

function changeBackgroundColor(color) {
	let mapContainer = document.querySelector("#map");
	mapContainer.style.backgroundColor = color;
}

function worldImageUrl(filename, layer = activeBaseLayer) {
    return (layer?.images || 'Images/') + filename;
}

function onProjectionChanged(e) {
    if (changingBaseLayer || activeBaseLayer === null) return;
	// Store the previous coords in the layer
	activeBaseLayer.viewPosition.startPoint = mymap.getCenter();
	activeBaseLayer.viewPosition.zoom = mymap.getZoom();
	updateLinkUrl(activeBaseLayer);
}

function onHashChange() {
    if (activeBaseLayer === null) {
        return;
    }

    const params = getFragmentParams();
    const layer = urlLayers.get(params.layer || activeBaseLayer.urlId);
    if (!layer || ['x', 'z', 'zoom'].some(key =>
        params.hasOwnProperty(key) && !Number.isFinite(parseInt(params[key])))) {
        return;
    }

    const view = findStartView(params, layer.layerId);
    const {startPoint, zoom} = view;
    layer.viewPosition = view;
    if (layer !== activeBaseLayer) {
        selectBaseLayer(layer);
    }
    mymap.setView(startPoint, zoom, {animate: false});
}

//TODO: refactor these refresh marker methods to use Leaflet LayerGroups
function refreshSpawnMarker(layer, markersVisible) {
	destroyMarkers(spawnMarkers);

	// Spawn marker
	if (showSpawn && layer.worldVectors.hasOwnProperty('spawnPosition')) {
		let spawn = layer.projection.worldVectors.spawnPosition;
		let point = layer.projection.worldToMap(spawn);

		let myIcon = L.icon({
			iconUrl: worldImageUrl('SpawnCompass.png', layer),
			// iconSize: [30, 30],
			iconAnchor: [17, 20],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});

        let marker = bindMarkerPopup(L.marker(point, { icon: myIcon }), '<p class="center">' + spawn.x + ', ' + spawn.y + ', ' + spawn.z + '</p>', spawn, layer).bindTooltip("Spawn Point");

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
			iconUrl: worldImageUrl('Sign.png', layer),
			// iconSize: [30, 30],
			iconAnchor: [17, 20],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});

		let marker = bindMarkerPopup(L.marker(point, { icon: myIcon }),
			'<pre><center>' + sign.text1 + '<br/>' + sign.text2 + '<br/>' + sign.text3 + '<br/>' + sign.text4 + '</center></pre>', sign.worldPos, layer);

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
			iconUrl: worldImageUrl('Picture.png', layer),
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

		let marker = bindMarkerPopup(L.marker(point, { icon: icon }), html, view.worldPos, layer, {maxWidth: "auto"});

		// Add marker to map if containers are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		viewMarkers.push(marker);
	}
}

function loadIcon(src, player, pos, markersVisible, layer) {
	var icon = new Image();
	icon.onload = function () {
        if (layer !== activeBaseLayer) return;
		player.icon = src;
		var marker = createPlayerMarker(player, pos, layer);

		// Add marker to map if players are initially visible
		if (controlState.playerControlChecked) {
			marker.addTo(mymap);
		}

		playerMarkers.push(marker);
	};
	icon.onerror = function () {
        if (layer !== activeBaseLayer) return;
		player.icon = worldImageUrl('PlayerIcons/Tectonicus_Default_Player_Icon.png', layer);
		var marker = createPlayerMarker(player, pos, layer);

		// Add marker to map if players are initially visible
		if (controlState.playerControlChecked) {
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

		loadIcon(worldImageUrl('PlayerIcons/' + player.name + '.png', layer), player, point, markersVisible, layer);
	}
}

function refreshBedMarkers(layer, markersVisible) {
	destroyMarkers(bedMarkers);

	// Bed markers
	for (i in layer.beds) {
		let bed = layer.beds[i];
		let point = layer.projection.worldToMap(bed.worldPos);

		let icon = L.icon({
			iconUrl: worldImageUrl('Items/' + bed.color + '_bed.png', layer),
			iconSize: [38, 38],
			iconAnchor: [20, 24],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});

		let marker = bindMarkerPopup(L.marker(point, { icon: icon }), '<p class="center">' + bed.playerName + "'s bed</p>", bed.worldPos, layer);

		// Add marker to map if beds are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		bedMarkers.push(marker);
	}
}

function refreshRespawnAnchorMarkers(layer, markersVisible) {
	destroyMarkers(respawnAnchorMarkers);

	// Respawn anchor markers
	for (i in layer.anchors) {
		let anchor = layer.anchors[i];
		let point = layer.projection.worldToMap(anchor.worldPos);

		let icon = L.icon({
			iconUrl: worldImageUrl('RespawnAnchor.png', layer),
			// iconSize: [30, 30],
			iconAnchor: [17, 20],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});

		let marker = bindMarkerPopup(L.marker(point, { icon: icon }), '<p class="center">' + anchor.playerName + "'s respawn anchor</p>", anchor.worldPos, layer);

		// Add marker to map if beds are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		respawnAnchorMarkers.push(marker);
	}
}

function refreshBeaconMarkers(layer, markersVisible) {
	destroyMarkers(beaconMarkers);

	// Beacon markers
	for (i in layer.beacons) {
		let beacon = layer.beacons[i];
		let point = layer.projection.worldToMap(beacon.worldPos);

		let icon = L.icon({
			iconUrl: worldImageUrl('beacon.png', layer),
			// iconSize: [30, 30],
			iconAnchor: [17, 20],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});

		let marker;
		if (beacon.levels > 0) {
		    let primaryEffect = beacon.primaryEffect !== "none" ? localize("effect.minecraft." + beacon.primaryEffect) : "None";
		    let secondaryEffect = beacon.secondaryEffect !== "none" ? localize("effect.minecraft." + beacon.secondaryEffect) : "None";

		    marker = bindMarkerPopup(L.marker(point, { icon: icon }), `<p style="width:230px; height:64px"><img style="float:left; margin:4px;" src="${worldImageUrl('beacon_level_' + parseInt(beacon.levels) + '.png', layer)}" width="64" height="64" />
		        Primary Power:<br /><img style="padding-right:4px; vertical-align:middle" src="${worldImageUrl('effects/' + beacon.primaryEffect + '.png', layer)}" />${primaryEffect}<br />
		        Secondary Power:<br /><img style="padding-right:4px; vertical-align:middle" src="${worldImageUrl('effects/' + beacon.secondaryEffect + '.png', layer)}" />${secondaryEffect}</p>`, beacon.worldPos, layer);
		} else {
		    marker = bindMarkerPopup(L.marker(point, { icon: icon }), '<p class="center">Not Activated</p>', beacon.worldPos, layer);
		}

//		let marker = L.marker(point, { icon: icon }).bindPopup('<p style="width:200px; height:64px"><img style=\"float:left; margin:4px;\" src=\"Images/beacon_level_1.png\" width=\"64\" height=\"64\" />Primary Power: <img src="Images/' + beacon.primaryEffect + '.png" alt="' + localize("effect.minecraft." + beacon.primaryEffect)
//		    + '"><br /> Secondary Power: <img src="Images/' + "haste" + '.png" alt="' + localize("effect.minecraft." + beacon.secondaryEffect) + '"></p>');

		// Add marker to map if beacons are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		beaconMarkers.push(marker);
	}
}

function refreshPortalMarkers(layer, markersVisible) {
	destroyMarkers(portalMarkers);

	// Portal markers
	for (i in layer.portals) {
		let portal = layer.portals[i];

		let point = layer.projection.worldToMap(portal.worldPos);

		let icon = L.icon({
			iconUrl: worldImageUrl('Portal.png', layer),
			// iconSize: [30, 30],
			iconAnchor: [17, 20],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});

		let marker = bindMarkerPopup(L.marker(point, { icon: icon }),
			'<div style=\"text-align:center\">Portal</div><br/> position (' + portal.worldPos.x + ', ' + portal.worldPos.y + ', ' + portal.worldPos.z + ')', portal.worldPos, layer);

		// Add marker to map if containers are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		portalMarkers.push(marker);
	}
}

function refreshContainerMarkers(layer, markersVisible) {
	destroyMarkers(containerMarkers);
        
	// Container markers
	for (i in layer.containers) {
		let point = layer.projection.worldToMap(layer.containers[i].worldPos);

		let icon = L.icon({
			iconUrl: worldImageUrl(containerMarkerIcon(layer.containers[i]), layer),
			// iconSize: [30, 30],
			iconAnchor: [17, 20],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});
                
                let markerPopup = createContainerPopup(layer.containers[i]);
		let marker = bindMarkerPopup(L.marker(point, { icon: icon }),
                        markerPopup,
                        layer.containers[i].worldPos, layer,
                        { maxWidth: "auto" } // Fix for incorrect sizing of the popup
                );

		// Add marker to map if containers are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		containerMarkers.push(marker);
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

function findStartView(params, defaultLayerId) {
	let queryLayerId = defaultLayerId;
	let queryZoom = 0;

	if (params.hasOwnProperty('layer') && urlLayers.get(params.layer)) {
		queryLayerId = urlLayers.get(params.layer).layerId;
	}
	let layer = tileLayers.get(queryLayerId);
	let queryPos = new WorldCoord(layer.worldVectors.startView.x, layer.worldVectors.startView.y, layer.worldVectors.startView.z);

	if (params.hasOwnProperty('x')
		&& params.hasOwnProperty('z')) {
		queryPos.x = Number.isFinite(parseInt(params.x)) ? parseInt(params.x) : queryPos.x;
		queryPos.z = Number.isFinite(parseInt(params.z)) ? parseInt(params.z) : queryPos.z;
	}
	if (params.hasOwnProperty('zoom')) {
		queryZoom = Number.isFinite(parseInt(params.zoom)) ? parseInt(params.zoom) : 0;
	}

	if (queryZoom < 0)
		queryZoom = 0;
	if (queryZoom > (layer.maxZoom ?? maxZoom))
		queryZoom = layer.maxZoom ?? maxZoom;

	let startPoint = layer.projection.worldToMap(queryPos);

	return new ViewPos(queryLayerId, queryPos, queryZoom, startPoint);
}

window.onload = mainAsync;
window.addEventListener('hashchange', onHashChange);
