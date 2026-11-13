
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
let controlTooltips = [];
const useDesktopToggleMenu = typeof useToggleMenu === 'undefined' || useToggleMenu;
const narrowToggleLayout = useDesktopToggleMenu ? null : window.matchMedia?.('(max-width: 767px)');

function bindControlTooltips() {
    controlTooltips = controlTooltips.filter(tooltip => {
        if (tooltip.reference.isConnected) return true;
        tooltip.destroy();
        return false;
    });

    for (const reference of mymap.getContainer().querySelectorAll('.leaflet-control [title]')) {
        const content = reference.getAttribute('title');
        if (!content) continue;
        reference.removeAttribute('title');
        if (!reference.hasAttribute('aria-label')) {
            reference.setAttribute('aria-label', content);
        }
        controlTooltips.push(tippy(reference, {
            content,
            delay: 0,
            duration: [100, 75],
            placement: reference.closest('.leaflet-left') ? 'right' : 'left',
            theme: 'light',
            touch: false,
            onShow() {
                return reference.getClientRects().length > 0
                    && reference.getAttribute('aria-expanded') !== 'true';
            }
        }));
    }
}

function localizeMapControls() {
    const selectors = {
        '.leaflet-control-zoom-in': 'tectonicus.controls.zoomIn',
        '.leaflet-control-zoom-out': 'tectonicus.controls.zoomOut',
        '.leaflet-control-layers-toggle:not(.toggle-menu-button)': 'tectonicus.controls.layers'
    };
    const container = mymap.getContainer();
    for (const [selector, key] of Object.entries(selectors)) {
        for (const button of container.querySelectorAll(selector)) button.setAttribute('data-map-label', key);
    }
    for (const button of container.querySelectorAll('[data-map-label]')) {
        const label = mapText(button.getAttribute('data-map-label'));
        button.setAttribute('aria-label', label);
        if (button._tippy) button._tippy.setContent(label);
        else button.setAttribute('title', label);
    }
    bindControlTooltips();
}

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
var toggleMenuControl = null;
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

        await containersMainPromise;
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
                tileLayer.localizationsScript = tecMap.localizationsScript;
                tileLayer.worldLabel = tecMap.worldName || mapText('selectWorld.world', {}, tileLayer);
                if (tecMap.nameKey) tileLayer.mapName = mapText(tecMap.nameKey, {}, tileLayer, tecMap.name);
                tileLayer.maxZoom = tecMap.maxZoom ?? maxZoom;
                L.setOptions(tileLayer, {maxZoom: tileLayer.maxZoom});
                if (!dimensionStates.has(tecMap.id)) {
                    dimensionStates.set(tecMap.id, {...tileLayer.controlState});
                }
                tileLayer.controlState = dimensionStates.get(tecMap.id);
                const layerLabel = layer.nameKey ? mapText(layer.nameKey, {}, tileLayer, layer.name) : layer.name;
                tileLayer.label = tileLayer.mapName + " - " + layerLabel;
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
        viewToggleControl = CreateToggleControl('views', 'Picture.png', viewsInitiallyVisible, 'viewControlChecked');
        signToggleControl = CreateToggleControl('signs', 'Sign.png', signsInitiallyVisible, 'signControlChecked');
        playerToggleControl = CreateToggleControl('players', 'PlayerIcons/Tectonicus_Default_Player_Icon.png', playersInitiallyVisible, 'playerControlChecked');
        bedToggleControl = CreateToggleControl('beds', 'Bed.png', bedsInitiallyVisible, 'bedControlChecked');
        respawnAnchorToggleControl = CreateToggleControl('respawnAnchors', 'RespawnAnchor.png', respawnAnchorsInitiallyVisible, 'respawnAnchorControlChecked');
        beaconToggleControl = CreateToggleControl('beacons', 'beacon.png', beaconsInitiallyVisible, 'beaconControlChecked');
        portalToggleControl = CreateToggleControl('portals', 'Portal.png', portalsInitiallyVisible, 'portalControlChecked');
        spawnToggleControl = CreateToggleControl('spawn', 'SpawnCompass.png', spawnInitiallyVisible, 'spawnControlChecked');
        containerToggleControl = CreateToggleControl('containers', 'Chest.png', containersInitiallyVisible, 'containerControlChecked');

        // Rebind the map information tooltip when attribution changes with the layer.
        let attributionContainer = mymap.attributionControl.getContainer();
        new MutationObserver(bindMapInfoTippy).observe(attributionContainer, {childList: true});

	// Register these last so that they don't get called while we're still initializing
	mymap.on('baselayerchange', onBaseLayerChange);
	mymap.on('popupopen', ({popup}) => {
        const close = popup.getElement().querySelector('.leaflet-popup-close-button');
        close?.setAttribute('aria-label', mapText('mco.selectServer.close'));
    });
	mymap.on('zoomend', onProjectionChanged);
    mymap.on('moveend', onProjectionChanged);
    const updateToggleLayout = () => updateMarkerControls(activeBaseLayer);
    narrowToggleLayout?.addEventListener('change', updateToggleLayout);
    mymap.on('unload', () => {
        narrowToggleLayout?.removeEventListener('change', updateToggleLayout);
        for (const tooltip of controlTooltips) tooltip.destroy();
        controlTooltips = [];
    });

	// Manually fire this event to set the initial state
    mymap.fireEvent('baselayerchange', {layer: startLayer});
}

const markerGroups = {
    spawnControlChecked: L.layerGroup(),
    signControlChecked: L.layerGroup(),
    viewControlChecked: L.layerGroup(),
    playerControlChecked: L.layerGroup(),
    portalControlChecked: L.layerGroup(),
    bedControlChecked: L.layerGroup(),
    respawnAnchorControlChecked: L.layerGroup(),
    beaconControlChecked: L.layerGroup(),
    containerControlChecked: L.layerGroup()
};
let playerMarkerGeneration = 0;

function updateMarkerControls(layer) {
    const toggles = [
        [spawnToggleControl, controlState.spawnControlChecked,
            layer.worldVectors.hasOwnProperty('spawnPosition') && layer.dimension !== "NETHER"],
        [signToggleControl, controlState.signControlChecked, layer.signs.length],
        [viewToggleControl, controlState.viewControlChecked, layer.views.length],
        [playerToggleControl, controlState.playerControlChecked, layer.players.length],
        [portalToggleControl, controlState.portalControlChecked, layer.portals.length],
        [bedToggleControl, controlState.bedControlChecked, layer.beds.length],
        [respawnAnchorToggleControl, controlState.respawnAnchorControlChecked, layer.anchors.length],
        [beaconToggleControl, controlState.beaconControlChecked, layer.beacons.length],
        [containerToggleControl, controlState.containerControlChecked, layer.containers.length]
    ];
    if (useDesktopToggleMenu || narrowToggleLayout?.matches) {
        toggleMenuControl ??= CreateToggleMenuControl().addTo(mymap);
        toggleMenuControl.setToggles(toggles.filter(([, , available]) => available));
    } else {
        toggleMenuControl?.remove();
        toggleMenuControl = null;
        for (const [toggle, checked, available] of toggles) {
            toggle.remove();
            if (available) {
                toggle.addTo(mymap);
                toggle.setChecked(checked);
            }
        }
    }

    // Keep world and layer navigation below the marker controls.
    for (const navigationControl of [worldControl, layerControl]) {
        if (navigationControl) {
            L.DomUtil.toFront(navigationControl.getContainer());
        }
    }

	// Add tooltips to the toggle buttons
	tippy('.button', {
        content(reference) {
          reference.removeAttribute('title');
          return reference.markerTooltip();
        },
        placement: 'left',
        theme: 'light',
        delay: 0,
        duration: [100, 75],
    });
    localizeMapControls();
}

function onBaseLayerChange(e) {
    changingBaseLayer = true;
    const targetView = {...e.layer.viewPosition};
    activeBaseLayer = e.layer;
    const title = document.querySelector('title[data-map-default]');
    if (title) title.textContent = mapText('tectonicus.title');
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
    const fontMask = fontMaskUrl('Font.png', e.layer);
    mapStyle.setProperty('--minecraft-font', fontMask ? 'url("' + fontMask + '")' : 'none');
    mapStyle.setProperty('--enchanted-glint', 'url("' + worldImageUrl('EnchantedGlint.png', e.layer) + '")');

    updateMarkerControls(e.layer);

    for (const group of Object.values(markerGroups)) {
        group.remove();
    }
    refreshSpawnMarker(e.layer);
    refreshSignMarkers(e.layer);
    refreshViewMarkers(e.layer);
    refreshPlayerMarkers(e.layer);
    refreshPortalMarkers(e.layer);
    refreshBedMarkers(e.layer);
    refreshRespawnAnchorMarkers(e.layer);
    refreshBeaconMarkers(e.layer);
    refreshContainerMarkers(e.layer);
    for (const [key, group] of Object.entries(markerGroups)) {
        if (controlState[key]) {
            group.addTo(mymap);
        }
    }
	compassControl.remove();
	compassControl = CreateCompassControl(e.layer.compassUrl);
	mymap.addControl(compassControl);

	// Add map info popup
    mapInfoContent = renderMapInfo(e.layer);
    bindMapInfoTippy();
    onProjectionChanged();
}

function renderMapInfo(layer) {
    const text = (key, values) => escapeHtml(mapText('tectonicus.stats.' + key, values));
    const {renderedAt, renderDurationMs} = stats;
    const date = Number.isFinite(renderedAt) ? formatMapDate(renderedAt, {dateStyle: 'medium'}) : stats.renderedOnDate;
    const time = Number.isFinite(renderedAt) ? formatMapDate(renderedAt, {timeStyle: 'long'}) : stats.renderedOnTime;
    const duration = Number.isFinite(renderDurationMs) ? formatMapDuration(renderDurationMs) : stats.renderTime;
    const memory = formatMapNumber(stats.peakMemoryBytes / 1000000,
        {style: 'unit', unit: 'megabyte', minimumFractionDigits: 1, maximumFractionDigits: 1});
    const line = (key, values) => text(key, values) + '<br/>';
    return '<h4 class="center">' + text('mapInfo') + '</h4>'
        + '<div class="center">' + text('renderStats') + '</div>'
        + line('version', {version: stats.tectonicusVersion})
        + line('renderTime', {duration})
        + line('peakMemory', {memory})
        + line('createdOn', {date})
        + line('createdAt', {time})
        + '<br/><div class="center">' + text('worldStats') + '</div>'
        + line('players', {label: mapText('mco.configure.world.buttons.players'), count: formatMapNumber(layer.worldStats.numPlayers)})
        + line('chunks', {count: formatMapNumber(layer.worldStats.numChunks)})
        + line('portals', {count: formatMapNumber(layer.worldStats.numPortals)})
        + line('views', {count: formatMapNumber(layer.views.length)})
        + line('signs', {count: formatMapNumber(layer.signs.length)})
        + line('beds', {count: formatMapNumber(layer.beds.length)});
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
    const group = markerGroups[key];
    if (visible) {
        group.addTo(mymap);
    } else {
        group.remove();
    }
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

function refreshSpawnMarker(layer) {
	const group = markerGroups.spawnControlChecked;
	group.clearLayers();

	// Spawn marker
	if (showSpawn && layer.dimension !== 'NETHER' && layer.worldVectors.hasOwnProperty('spawnPosition')) {
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

        const coordinates = escapeHtml(mapText('tectonicus.marker.coordinates', formatMapCoordinates(spawn)));
        let marker = bindMarkerPopup(L.marker(point, { icon: myIcon }), '<p class="center">' + coordinates + '</p>', spawn, layer)
            .bindTooltip(escapeHtml(mapText('tectonicus.marker.spawnPoint')));

		group.addLayer(marker);
	}
}

function refreshSignMarkers(layer) {
	const group = markerGroups.signControlChecked;
	group.clearLayers();

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

		group.addLayer(marker);
	}
}

function refreshViewMarkers(layer) {
	const group = markerGroups.viewControlChecked;
	group.clearLayers();

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

		group.addLayer(marker);
	}
}

function refreshPlayerMarkers(layer) {
	const group = markerGroups.playerControlChecked;
	const generation = ++playerMarkerGeneration;
	group.clearLayers();
	const fallbackIcon = worldImageUrl('PlayerIcons/Tectonicus_Default_Player_Icon.png', layer);
	for (const player of layer.players) {
		player.donation = '';
		const point = layer.projection.worldToMap(player.worldPos);
		if (player.defaultPlayerName || !player.name || player.name === 'null') {
			player.icon = fallbackIcon;
			group.addLayer(createPlayerMarker(player, point, layer));
			continue;
		}
		const src = worldImageUrl('PlayerIcons/' + player.name + '.png', layer);
		const icon = new Image();
		const addMarker = iconUrl => {
			if (generation !== playerMarkerGeneration || layer !== activeBaseLayer) return;
			player.icon = iconUrl;
			group.addLayer(createPlayerMarker(player, point, layer));
		};
		icon.onload = () => addMarker(src);
		icon.onerror = () => addMarker(fallbackIcon);
		icon.src = src;
	}
}

function refreshBedMarkers(layer) {
	const group = markerGroups.bedControlChecked;
	group.clearLayers();

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

		let marker = bindMarkerPopup(L.marker(point, { icon: icon }), '<p class="center">'
			+ escapeHtml(mapText('tectonicus.marker.bed', {player: playerDisplayName(bed)})) + '</p>', bed.worldPos, layer);

		group.addLayer(marker);
	}
}

function refreshRespawnAnchorMarkers(layer) {
	const group = markerGroups.respawnAnchorControlChecked;
	group.clearLayers();

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

		let marker = bindMarkerPopup(L.marker(point, { icon: icon }), '<p class="center">'
			+ escapeHtml(mapText('tectonicus.marker.respawnAnchor', {player: playerDisplayName(anchor)})) + '</p>', anchor.worldPos, layer);

		group.addLayer(marker);
	}
}

function refreshBeaconMarkers(layer) {
	const group = markerGroups.beaconControlChecked;
	group.clearLayers();

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
		    let primaryEffect = escapeHtml(beacon.primaryEffect !== "none" ? localize("effect.minecraft." + beacon.primaryEffect) : mapText('gui.none'));
		    let secondaryEffect = escapeHtml(beacon.secondaryEffect !== "none" ? localize("effect.minecraft." + beacon.secondaryEffect) : mapText('gui.none'));

		    marker = bindMarkerPopup(L.marker(point, { icon: icon }), `<p style="width:230px; height:64px"><img style="float:left; margin:4px;" src="${worldImageUrl('beacon_level_' + parseInt(beacon.levels) + '.png', layer)}" width="64" height="64" />
		        ${escapeHtml(mapText('block.minecraft.beacon.primary'))}:<br /><img style="padding-right:4px; vertical-align:middle" src="${worldImageUrl('effects/' + beacon.primaryEffect + '.png', layer)}" />${primaryEffect}<br />
		        ${escapeHtml(mapText('block.minecraft.beacon.secondary'))}:<br /><img style="padding-right:4px; vertical-align:middle" src="${worldImageUrl('effects/' + beacon.secondaryEffect + '.png', layer)}" />${secondaryEffect}</p>`, beacon.worldPos, layer);
		} else {
		    marker = bindMarkerPopup(L.marker(point, { icon: icon }), '<p class="center">' + escapeHtml(mapText('tectonicus.marker.beaconInactive')) + '</p>', beacon.worldPos, layer);
		}

		group.addLayer(marker);
	}
}

function refreshPortalMarkers(layer) {
	const group = markerGroups.portalControlChecked;
	group.clearLayers();

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
			'<div style="text-align:center">' + escapeHtml(mapText('block.minecraft.nether_portal')) + '</div><br/> '
			+ escapeHtml(mapText('tectonicus.marker.coordinates', formatMapCoordinates(portal.worldPos))), portal.worldPos, layer);

		group.addLayer(marker);
	}
}

function refreshContainerMarkers(layer) {
	const group = markerGroups.containerControlChecked;
	group.clearLayers();
        
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

		group.addLayer(marker);
	}
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
