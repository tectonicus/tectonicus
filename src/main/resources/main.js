
let mymap = L.map('map', {crs: L.CRS.Simple, minZoom: 0, maxZoom: maxZoom, attributionControl: false});
let tileLayers = new Map();
let urlLayers = new Map();
let activeBaseLayer = null;
let worldLayers = new Map();
let selectedWorldLayers = new Map();
let markerVisibilityOverrides = new Map();
let worldControl = null;
let subsetControl = null;
let layerControlWorldId = null;
let changingBaseLayer = false;
let mapInfoTippy = null;
let mapInfoContent = '';
let controlTooltips = [];
const useDesktopToggleMenu = typeof useToggleMenu === 'undefined' || useToggleMenu;
const narrowToggleLayout = useDesktopToggleMenu ? null : window.matchMedia?.('(max-width: 767px)');

// Replaces the native title tooltip of each element with a themed tippy tooltip and returns the
// created instances. `options` gives per-element tippy options such as the placement.
function bindTitleTooltips(references, options = () => ({})) {
    const tooltips = [];
    for (const reference of references) {
        const content = reference.getAttribute('title');
        if (!content) continue;
        reference.removeAttribute('title');
        if (!reference.hasAttribute('aria-label')) {
            reference.setAttribute('aria-label', content);
        }
        tooltips.push(tippy(reference, {
            content,
            delay: 0,
            duration: [100, 75],
            theme: 'minecraft',
            touch: false,
            ...options(reference)
        }));
    }
    return tooltips;
}

function bindControlTooltips() {
    controlTooltips = controlTooltips.filter(tooltip => {
        if (tooltip.reference.isConnected) return true;
        tooltip.destroy();
        return false;
    });

    const references = mymap.getContainer().querySelectorAll('.leaflet-control [title]');
    controlTooltips.push(...bindTitleTooltips(references, reference => ({
        placement: reference.closest('.leaflet-left') ? 'right' : 'left',
        onShow() {
            return reference.getClientRects().length > 0
                && reference.getAttribute('aria-expanded') !== 'true';
        }
    })));
}

// Popup buttons (copy coordinates, portal jump, ...) carry title attributes too.
function bindPopupTooltips(popup) {
    bindTitleTooltips(popup.getElement()?.querySelectorAll('[title]') ?? [], () => ({
        placement: 'top',
        appendTo: () => document.body
    }));
}

// Leaflet rebuilds popup content on every open, so drop the old instances when it closes.
function unbindPopupTooltips(popup) {
    for (const reference of popup.getElement()?.querySelectorAll('*') ?? []) reference._tippy?.destroy();
}

function localizeMapControls() {
    const selectors = {
        '.leaflet-control-zoom-in': 'tectonicus.controls.zoomIn',
        '.leaflet-control-zoom-out': 'tectonicus.controls.zoomOut',
        '.leaflet-control-layers-toggle:not(.toggle-menu-button):not(.overlay-control-toggle)': 'tectonicus.controls.layers'
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
            theme: 'minecraft',
            maxWidth: 500
        });
    } else {
        mapInfoTippy.setContent(mapInfoContent);
    }
}

/*
 * The Minecraft font (Mac's Minecraft) is drawn on a pixel grid and is only sharp when one font pixel covers a whole number of
 * device pixels. A fixed CSS size (e.g. 15px) makes a font pixel 1.5 device pixels wide, so some strokes
 * render 1 px and others 2 px. Measure the font once (by drawing glyphs that are one font pixel wide)
 * and set the --mc-* sizes in tectonicusStyles.css for the current devicePixelRatio; redo it whenever
 * the ratio changes (browser zoom, moving the window to another screen).
 */
const PIXEL_FONT = {
    family: 'Macs Minecraft',
    textScale: 2,       // device pixels per font pixel at 100 % scaling, like Minecraft's GUI scale
    largeTextScale: 3,  // zoom +/- buttons
    lineHeight: 11      // in font pixels
};

function measureInkWidth(family, character, size) {
    const canvas = document.createElement('canvas');
    canvas.width = canvas.height = size * 2;
    const context = canvas.getContext('2d', {willReadFrequently: true});
    context.font = size + 'px "' + family + '"';
    context.fillText(character, size / 2, size * 1.5);
    const data = context.getImageData(0, 0, canvas.width, canvas.height).data;
    let columns = 0;
    for (let x = 0; x < canvas.width; x++) {
        for (let y = 0; y < canvas.height; y++) {
            if (data[(y * canvas.width + x) * 4 + 3] > 127) {
                columns++;
                break;
            }
        }
    }
    return columns;
}

// Returns font pixels per em, or null if the font does not look like a pixel font.
function measureFontPixelsPerEm(family) {
    const size = 200;
    // One font pixel wide in the Minecraft font; take the narrowest in case one is not.
    const widths = ['|', '.', '!', 'i'].map(c => measureInkWidth(family, c, size)).filter(w => w > 0);
    if (!widths.length) return null;
    const pixelsPerEm = Math.round(size / Math.min(...widths));
    return pixelsPerEm >= 4 && pixelsPerEm <= 32 ? pixelsPerEm : null;
}

function applyPixelFontSizes(pixelsPerEm) {
    const ratio = window.devicePixelRatio || 1;
    const fontPixel = scale => Math.max(1, Math.round(scale * ratio)) / ratio;
    const unit = fontPixel(PIXEL_FONT.textScale);
    const style = document.documentElement.style;
    style.setProperty('--mc-u', unit + 'px');
    style.setProperty('--mc-fs', unit * pixelsPerEm + 'px');
    style.setProperty('--mc-fs-l', fontPixel(PIXEL_FONT.largeTextScale) * pixelsPerEm + 'px');
    style.setProperty('--mc-lh', unit * PIXEL_FONT.lineHeight + 'px');
}

function setUpPixelFont() {
    if (!document.fonts?.load) return;
    document.fonts.load('16px "' + PIXEL_FONT.family + '"').then(faces => {
        if (!faces.length) return; // font missing: keep the CSS defaults
        const pixelsPerEm = measureFontPixelsPerEm(PIXEL_FONT.family);
        if (!pixelsPerEm) return;
        const update = () => {
            applyPixelFontSizes(pixelsPerEm);
            const ratio = window.matchMedia('(resolution: ' + (window.devicePixelRatio || 1) + 'dppx)');
            ratio.addEventListener('change', update, {once: true});
        };
        update();
    }).catch(() => {});
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
        setUpPixelFont();

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
        let overlayStates = new Map();
        for (let i = 0; i < contents.length; i++) {
            let tecMap = contents[i];
            for (let j = 0; j < tecMap.layers.length; j++) {
                let layer = tecMap.layers[j];
                let projection = new MinecraftProjection(tecMap.worldVectors);
                const startPosition = new ViewPos(layer.id, tecMap.worldVectors.startView,
                    mymap.getMinZoom(), projection.worldToMap(tecMap.worldVectors.startView));

                let tileLayer = new L.TileLayer.Tectonicus(tecMap.outputId || tecMap.id, layer.id, layer.dimension, layer.imageFormat, tecMap.name, layer.backgroundColor, tecMap.signs, tecMap.players,
                    tecMap.containers ?? tecMap.chests ?? [], tecMap.views, tecMap.portals, tecMap.beds, tecMap.respawnAnchors, tecMap.beacons, tecMap.worldVectors, projection, tecMap.blockStats, tecMap.worldStats, startPosition, tecMap.controlState || controlState, layer.tileIndex);
                tileLayer.worldId = tecMap.worldId || 'World0';
				tileLayer.dimensionId = tecMap.dimensionId;
                tileLayer.worldName = tecMap.worldName || 'World';
                tileLayer.images = tecMap.images || 'Images/';
                tileLayer.markerShadows = tecMap.markerShadows;
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
                tileLayer.overlays = tecMap.overlays ?? null;
                if (!overlayStates.has(tecMap.id)) {
                    overlayStates.set(tecMap.id, overlayVisibility(tileLayer));
                }
                tileLayer.overlayVisibility = overlayStates.get(tecMap.id);
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
        mymap.setMaxZoom(startLayer.maxZoom);
        mymap.setView(startView.startPoint, startView.zoom, {animate: false});
        startLayer.addTo(mymap);
        // Store the updated start point in the layer
        startLayer.viewPosition = startView;

        // Create controls
        if (worldLayers.size > 1) {
            worldControl = CreateWorldControl(worldLayers, startLayer.worldId, switchWorld);
            worldControl.addTo(mymap);
        }
        updateWorldLayerControl(startLayer);
        subsetControl = CreateSubsetControl();
        subsetControl.addTo(mymap);
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
        bindPopupTooltips(popup);
    });
    mymap.on('popupclose', ({popup}) => unbindPopupTooltips(popup));
	mymap.on('zoomend', onProjectionChanged);
    mymap.on('moveend', onProjectionChanged);
    const updateToggleLayout = () => updateMarkerControls(activeBaseLayer);
    narrowToggleLayout?.addEventListener('change', updateToggleLayout);
    mymap.on('unload', () => {
        narrowToggleLayout?.removeEventListener('change', updateToggleLayout);
        for (const tooltip of controlTooltips) tooltip.destroy();
        controlTooltips = [];
    });

	initOverlays();

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

    // Put the subset selector directly below the marker controls.
    for (const navigationControl of [subsetControl, worldControl, layerControl]) {
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
        theme: 'minecraft',
        delay: 0,
        duration: [100, 75],
    });
    localizeMapControls();
}

function onBaseLayerChange(e) {
    changingBaseLayer = true;
    const sameSubsetMap = activeBaseLayer !== e.layer
        && activeBaseLayer?.worldVectors === e.layer.worldVectors
        && e.layer.worldVectors.subsets?.length > 1;
    const targetView = sameSubsetMap ? activeBaseLayer.viewPosition : e.layer.viewPosition;
    activeBaseLayer = e.layer;
    const title = document.querySelector('title[data-map-default]');
    if (title) title.textContent = mapText('tectonicus.title');
    controlState = {...e.layer.controlState, ...markerVisibilityOverrides.get(e.layer.worldId)};
    selectedWorldLayers.set(e.layer.worldId, e.layer.layerId);
    const layerMaxZoom = e.layer.maxZoom ?? maxZoom;
    // Leaflet animates a zoom clamp at the old center. Move to the destination before lowering the limit.
    const clampZoom = mymap.getZoom() > layerMaxZoom;
    if (!clampZoom) mymap.setMaxZoom?.(layerMaxZoom);
    updateWorldLayerControl(e.layer);
    if (worldControl) {
        worldControl.setWorld(e.layer.worldId);
    }
    subsetControl.setSubsets(e.layer.worldVectors.subsets ?? []);

    mymap.setView(targetView.startPoint, Math.min(targetView.zoom, layerMaxZoom), {animate: false});
    if (clampZoom) mymap.setMaxZoom?.(layerMaxZoom);
    changingBaseLayer = false;

	changeBackgroundColor(e.layer.backgroundColor);
    const mapStyle = document.querySelector('#map').style;
    const fontMask = fontMaskUrl('Font.png', e.layer);
    mapStyle.setProperty('--minecraft-font', fontMask ? 'url("' + fontMask + '")' : 'none');
    // url() in a custom property resolves against the stylesheet that uses it (Scripts/), so pass an absolute URL.
    const glintUrl = new URL(worldImageUrl('EnchantedGlint.png', e.layer), document.baseURI).href;
    mapStyle.setProperty('--enchanted-glint', 'url("' + glintUrl + '")');

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
	refreshOverlays(e.layer);
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

function showSubset(layer, subset) {
    const {minX, minZ, maxX, maxZ} = subset;
    const {minY = layer.worldVectors.startView.y, maxY = minY} = subset;
    // Project both elevations at each corner for rotated isometric cameras.
    const points = [minY, maxY].flatMap(y => [
        {x: minX, y, z: minZ},
        {x: minX, y, z: maxZ + 1},
        {x: maxX + 1, y, z: minZ},
        {x: maxX + 1, y, z: maxZ + 1}
    ]);
    const bounds = L.latLngBounds(points.map(point => layer.projection.worldToMap(point)));
    mymap.fitBounds(bounds, {animate: false, padding: [24, 24]});
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
    layer.viewPosition = view;
    if (layer !== activeBaseLayer) {
        selectBaseLayer(layer);
    }
    mymap.setView(view.startPoint, view.zoom, {animate: false});
}

function refreshSpawnMarker(layer) {
	const group = markerGroups.spawnControlChecked;
	group.clearLayers();

	// Spawn marker
	if (showSpawn && layer.dimension !== 'NETHER' && layer.worldVectors.hasOwnProperty('spawnPosition')) {
		let spawn = layer.projection.worldVectors.spawnPosition;
		let point = layer.projection.worldToMap(spawn);

		const myIcon = createWorldMarkerIcon('SpawnCompass.png', [17, 20], layer);

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
	for (const sign of layer.signs ?? []) {
		const point = layer.projection.worldToMap(sign.worldPos);
		const myIcon = createWorldMarkerIcon('Sign.png', [17, 20], layer);
		const popup = () => renderSignPopup(sign, layer);

		group.addLayer(bindMarkerPopup(L.marker(point, {icon: myIcon}), popup, sign.worldPos, layer));
	}
}

function renderSignPopup(sign, layer) {
	const color = minecraftTextColor(sign.color);
	const outline = minecraftTextColor(sign.outlineColor);
	const classes = ['sign_text'];
	const styles = [];
	if (color) styles.push(`color:${color};`);
	if (outline) {
		classes.push('sign_text_glowing');
		styles.push(`--sign-outline:${outline};`);
	}
	const backgrounds = worldLocalizations.get(layer?.worldId)?.signBackgrounds;
	if (backgrounds?.includes(sign.background)) {
		classes.push('sign_text_board');
		if (sign.background.includes('/hanging/')) classes.push('sign_text_hanging_board');
		const url = encodeURI(worldImageUrl(sign.background, layer)).replaceAll("'", '%27');
		styles.push(`background-image:url('${url}');`);
	}
	const lines = [sign.text1, sign.text2, sign.text3, sign.text4].map((line, index) =>
		renderMinecraftText(line, 'sign_text_line', layer, sign.textColors?.[index]) || '<div class="sign_text_line"></div>');
	const style = styles.length ? ` style="${escapeHtml(styles.join(''))}"` : '';
	return `<div class="${classes.join(' ')}"${style}>${lines.join('')}</div>`;
}

function refreshViewMarkers(layer) {
	const group = markerGroups.viewControlChecked;
	group.clearLayers();

	// View markers
	for (i in layer.views) {
		let view = layer.views[i];

		let point = layer.projection.worldToMap(view.worldPos);

		const icon = createWorldMarkerIcon('Picture.png', [17, 20], layer);

		let html = '';
		html += '<div>';
		const imageFile = escapeHtml(view.imageFile);
		html += '<a href="' + imageFile + '">';
		html += '<img width="512" height="288" src="' + imageFile + '"/>';
		html += '</a>';
		html += '</div>';
		html += '';
		html += '<center>';
		// View text comes from player-written signs, so it must never be treated as markup.
		html += escapeHtml(view.text);
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

		const icon = createWorldMarkerIcon('Items/' + bed.color + '_bed.png', [20, 24], layer, [38, 38]);

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

		const icon = createWorldMarkerIcon('RespawnAnchor.png', [17, 20], layer);

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

		const icon = createWorldMarkerIcon('beacon.png', [17, 20], layer);

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

	for (const portal of layer.portals) {
		const point = layer.projection.worldToMap(portal.worldPos);
		const icon = createWorldMarkerIcon('Portal.png', [17, 20], layer);
		const marker = bindMarkerPopup(L.marker(point, {icon}), () =>
			'<div style="text-align:center">' + escapeHtml(mapText('block.minecraft.nether_portal', {}, layer)) + '</div><br/>'
			+ '<div class="center">' + escapeHtml(mapText('tectonicus.marker.coordinates', formatMapCoordinates(portal.worldPos, Math.floor), layer)) + '</div>'
			+ portalJumpButton(layer, portal), portal.worldPos, layer);
		group.addLayer(marker);
	}
}

function findPortalExit(layer, portal) {
	const overworld = 'minecraft:overworld';
	const nether = 'minecraft:the_nether';
	if (!portal.destinationId || (layer.dimensionId !== overworld && layer.dimensionId !== nether)) return;
	const destinationDimension = layer.dimensionId === overworld ? nether : overworld;
	for (const target of worldLayers.get(layer.worldId) ?? []) {
		if (target.dimensionId !== destinationDimension) continue;
		// The renderer resolves the exit against saved terrain, including portals outside all map subsets.
		const index = target.portals.findIndex(candidate => candidate.id === portal.destinationId);
		if (index >= 0) return {layer: target, index};
	}
}

function portalJumpButton(layer, portal) {
	const exit = findPortalExit(layer, portal);
	if (!exit) return '';
	const label = escapeHtml(mapText('tectonicus.marker.portalExit', {}, layer));
	const arrow = 'M2 12H16.5 M12.2 7.7L16.5 12 12.2 16.3';
	const icon = `<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
		<rect x="11" y="3" width="10" height="18" fill="#8b43cf" stroke="#000" stroke-width="2.5"/>
		<path d="M13.1 5.5h1.4V8h-1.4z M17.4 8h1.3v1.3h-1.3z" fill="#ac70ec"/>
		<rect x="17.4" y="16.6" width="1.3" height="2" fill="#6632a2"/>
		<g fill="none" stroke-linecap="round" stroke-linejoin="round">
			<path d="${arrow}" stroke="#f5f5f5" stroke-width="4.3"/>
			<path d="${arrow}" stroke="currentColor" stroke-width="2.3"/>
		</g>
	</svg>`;
	return `<div class="portal_jump_control"><button type="button" data-portal-layer="${escapeHtml(exit.layer.layerId)}" data-portal-index="${exit.index}"
		title="${label}" aria-label="${label}" onclick="jumpToPortal(this)">${icon}</button></div>`;
}

function jumpToPortal(button) {
	const layer = tileLayers.get(button.dataset.portalLayer);
	const index = Number(button.dataset.portalIndex);
	const portal = layer?.portals[index];
	if (!portal || layer.worldId !== activeBaseLayer?.worldId || layer === activeBaseLayer) return;

	const zoom = Math.max(mymap.getMinZoom(), Math.min(mymap.getZoom(), layer.maxZoom ?? maxZoom));
	layer.viewPosition = new ViewPos(layer.layerId, portal.worldPos, zoom, layer.projection.worldToMap(portal.worldPos));
	selectBaseLayer(layer);
	if (controlState.portalControlChecked) {
		const marker = markerGroups.portalControlChecked.getLayers()[index];
		const popup = marker.getPopup();
		const autoPan = popup.options.autoPan;
		popup.options.autoPan = false;
		try {
			marker.openPopup();
		} finally {
			popup.options.autoPan = autoPan;
		}
	}
}

function refreshContainerMarkers(layer) {
	const group = markerGroups.containerControlChecked;
	group.clearLayers();
        
	// Container markers
	for (i in layer.containers) {
		let point = layer.projection.worldToMap(layer.containers[i].worldPos);

		const icon = createWorldMarkerIcon(containerMarkerIcon(layer.containers[i]), [17, 20], layer);
                
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
	const minZoom = mymap.getMinZoom();
	let queryZoom = minZoom;

	if (params.hasOwnProperty('layer') && urlLayers.get(params.layer)) {
		queryLayerId = urlLayers.get(params.layer).layerId;
	}
	const layer = tileLayers.get(queryLayerId);
	const queryPos = new WorldCoord(layer.worldVectors.startView.x, layer.worldVectors.startView.y, layer.worldVectors.startView.z);

	if (params.hasOwnProperty('x')
		&& params.hasOwnProperty('z')) {
		queryPos.x = Number.isFinite(parseInt(params.x)) ? parseInt(params.x) : queryPos.x;
		queryPos.z = Number.isFinite(parseInt(params.z)) ? parseInt(params.z) : queryPos.z;
	}
	if (params.hasOwnProperty('zoom')) {
		queryZoom = Number.isFinite(parseInt(params.zoom)) ? parseInt(params.zoom) : minZoom;
	}

	if (queryZoom < minZoom)
		queryZoom = minZoom;
	if (queryZoom > (layer.maxZoom ?? maxZoom))
		queryZoom = layer.maxZoom ?? maxZoom;

	let startPoint = layer.projection.worldToMap(queryPos);

	return new ViewPos(queryLayerId, queryPos, queryZoom, startPoint);
}

window.onload = mainAsync;
window.addEventListener('hashchange', onHashChange);
