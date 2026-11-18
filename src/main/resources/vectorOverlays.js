// Vector overlays drawn by Leaflet over the tiles. Geometry is kept in Minecraft
// block coordinates and projected through the dimension's MinecraftProjection.
// Flat overlays such as grids lie on one block height (overlays.y), so they line
// up with terrain at that height and drift with the camera angle elsewhere.

const OVERLAY_TYPES = ['worldBorder', 'regionGrid', 'chunkGrid', 'slimeChunks', 'features'];
// Grids are generated for the view only. Hide them when too many lines would be drawn.
const MAX_GRID_LINES = 600;
const MAX_SLIME_CHUNKS = 6000;

const overlayStyles = {
    chunkGrid: {color: '#000000', weight: 1, opacity: 0.35, interactive: false},
    regionGrid: {color: '#1f4fff', weight: 2, opacity: 0.8, interactive: false},
    slimeChunks: {stroke: false, fillColor: '#5cd65c', fillOpacity: 0.4, interactive: false},
    worldBorder: {color: '#e03030', weight: 3, opacity: 0.9, dashArray: '8 6', fill: false, interactive: false},
    features: {color: '#3388ff', weight: 3, opacity: 1, fillOpacity: 0.2}
};

const overlayRenderers = {};
let overlayControl = null;
let overlayBaseLayer = null;
const overlayGroups = new Map();
// Dynamic overlays are rebuilt for the view. Static ones are built once per base layer.
// Listed bottom to top: a view change redraws them in this order.
const dynamicOverlays = {
    slimeChunks: layer => slimeChunkPolygons(layer),
    chunkGrid: layer => gridLines(layer, 16),
    regionGrid: layer => gridLines(layer, 512)
};

// Canvas draws thousands of shapes far faster than one SVG element per shape.
// Generated overlays get their own pane below the default overlay pane, so custom
// features stay on top however often the grids are redrawn.
function getOverlayRenderer(pane = 'overlayPane') {
    if (!overlayRenderers[pane]) {
        if (!mymap.getPane(pane)) mymap.createPane(pane).style.zIndex = 390;
        overlayRenderers[pane] = L.canvas({padding: 0.5, pane});
    }
    return overlayRenderers[pane];
}

function getGeneratedOverlayRenderer() {
    return getOverlayRenderer('tectonicusGeneratedOverlays');
}

function overlayY(layer) {
    return Number.isFinite(layer.overlays?.y) ? layer.overlays.y : 64;
}

function overlayPoint(layer, point) {
    return layer.projection.worldToLatLng({x: point[0], y: point[1] ?? overlayY(layer), z: point[2]});
}

// The world X/Z rectangle seen in the view, solved on the overlay plane.
function visibleWorldBounds(layer) {
    const bounds = mymap.getBounds().pad(0.2);
    const y = overlayY(layer);
    const corners = [bounds.getNorthWest(), bounds.getNorthEast(), bounds.getSouthWest(), bounds.getSouthEast()]
        .map(corner => layer.projection.mapToWorld(corner, y));
    const xs = corners.map(corner => corner.x);
    const zs = corners.map(corner => corner.z);
    return {minX: Math.min(...xs), maxX: Math.max(...xs), minZ: Math.min(...zs), maxZ: Math.max(...zs), y};
}

function gridLines(layer, step) {
    const {minX, maxX, minZ, maxZ, y} = visibleWorldBounds(layer);
    const firstX = Math.ceil(minX / step) * step;
    const firstZ = Math.ceil(minZ / step) * step;
    const count = Math.floor((maxX - firstX) / step) + Math.floor((maxZ - firstZ) / step) + 2;
    if (!Number.isFinite(count) || count > MAX_GRID_LINES) return null;
    const segment = (from, to) => [layer.projection.worldToLatLng(from), layer.projection.worldToLatLng(to)];
    const lines = [];
    for (let x = firstX; x <= maxX; x += step) lines.push(segment({x, y, z: minZ}, {x, y, z: maxZ}));
    for (let z = firstZ; z <= maxZ; z += step) lines.push(segment({x: minX, y, z}, {x: maxX, y, z}));
    return lines.length ? L.polyline(lines, {...overlayStyles[step === 16 ? 'chunkGrid' : 'regionGrid'], renderer: getGeneratedOverlayRenderer()}) : null;
}

function slimeChunkPolygons(layer) {
    const chunks = layer.overlays?.slimeChunks?.chunks ?? [];
    const {minX, maxX, minZ, maxZ, y} = visibleWorldBounds(layer);
    const [minChunkX, maxChunkX, minChunkZ, maxChunkZ] = [minX, maxX, minZ, maxZ].map(value => Math.floor(value / 16));
    const polygons = [];
    for (let i = 0; i + 1 < chunks.length; i += 2) {
        const chunkX = chunks[i];
        const chunkZ = chunks[i + 1];
        if (chunkX < minChunkX || chunkX > maxChunkX || chunkZ < minChunkZ || chunkZ > maxChunkZ) continue;
        if (polygons.length >= MAX_SLIME_CHUNKS) return null;
        const x = chunkX * 16;
        const z = chunkZ * 16;
        polygons.push([[
            layer.projection.worldToLatLng({x, y, z}),
            layer.projection.worldToLatLng({x: x + 16, y, z}),
            layer.projection.worldToLatLng({x: x + 16, y, z: z + 16}),
            layer.projection.worldToLatLng({x, y, z: z + 16})
        ]]);
    }
    return polygons.length ? L.polygon(polygons, {...overlayStyles.slimeChunks, renderer: getGeneratedOverlayRenderer()}) : null;
}

function worldBorderShape(layer) {
    const border = layer.overlays?.worldBorder;
    if (!border) return null;
    const half = border.size / 2;
    const y = overlayY(layer);
    const corners = [[-half, -half], [half, -half], [half, half], [-half, half]].map(([dx, dz]) =>
        layer.projection.worldToLatLng({x: border.centerX + dx, y, z: border.centerZ + dz}));
    return L.polygon(corners, {...overlayStyles.worldBorder, renderer: getGeneratedOverlayRenderer()});
}

function featureStyle(feature) {
    const style = {...overlayStyles.features, renderer: getOverlayRenderer()};
    if (feature.color) style.color = feature.color;
    if (feature.fillColor ?? feature.color) style.fillColor = feature.fillColor ?? feature.color;
    if (Number.isFinite(feature.width)) style.weight = feature.width;
    if (Number.isFinite(feature.opacity)) style.opacity = feature.opacity;
    if (Number.isFinite(feature.fillOpacity)) style.fillOpacity = feature.fillOpacity;
    return style;
}

function featurePopup(feature) {
    const name = feature.name ? '<div class="overlay-feature-name">' + escapeHtml(feature.name) + '</div>' : '';
    const description = feature.description
        ? '<div class="overlay-feature-description">' + escapeHtml(feature.description) + '</div>' : '';
    return name + description;
}

function featureLayers(layer) {
    const group = L.featureGroup();
    for (const feature of layer.overlays?.features?.items ?? []) {
        const style = featureStyle(feature);
        let shape;
        let worldPos = null;
        if (feature.type === 'marker') {
            const [x, y, z] = feature.point;
            worldPos = {x, y: y ?? overlayY(layer), z};
            shape = L.circleMarker(overlayPoint(layer, feature.point), {...style, radius: 7, fillOpacity: style.fillOpacity > 0.2 ? style.fillOpacity : 0.8});
        } else if (feature.type === 'polygon') {
            shape = L.polygon(feature.points.map(point => overlayPoint(layer, point)), style);
        } else {
            shape = L.polyline(feature.points.map(point => overlayPoint(layer, point)), {...style, fill: false});
        }
        const popup = featurePopup(feature);
        if (popup || worldPos) bindMarkerPopup(shape, popup, worldPos, layer);
        if (feature.name) shape.bindTooltip(escapeHtml(feature.name), {sticky: feature.type !== 'marker'});
        group.addLayer(shape);
    }
    return group;
}

function overlayGroup(type) {
    if (!overlayGroups.has(type)) overlayGroups.set(type, L.layerGroup());
    return overlayGroups.get(type);
}

function rebuildOverlay(type, layer) {
    const group = overlayGroup(type);
    group.clearLayers();
    let shape = null;
    if (dynamicOverlays[type]) shape = dynamicOverlays[type](layer);
    else if (type === 'worldBorder') shape = worldBorderShape(layer);
    else if (type === 'features') shape = featureLayers(layer);
    if (shape) group.addLayer(shape);
}

function availableOverlays(layer) {
    const overlays = layer?.overlays;
    if (!overlays) return [];
    return OVERLAY_TYPES.filter(type => overlays[type]
        && (type !== 'slimeChunks' || overlays.slimeChunks.chunks?.length)
        && (type !== 'features' || overlays.features.items?.length));
}

// Visibility is shared by all layers of a dimension and kept while switching layers.
function overlayVisibility(layer) {
    if (!layer.overlayVisibility) {
        layer.overlayVisibility = {};
        for (const type of OVERLAY_TYPES) layer.overlayVisibility[type] = !!layer.overlays?.[type]?.visible;
    }
    return layer.overlayVisibility;
}

function refreshOverlays(layer) {
    // The layer control reports removals as user toggles. Detach it first and keep
    // the remembered visibility while the overlays are rebuilt.
    overlayBaseLayer = null;
    overlayControl?.remove();
    overlayControl = null;
    for (const group of overlayGroups.values()) {
        group.remove();
        group.clearLayers();
    }

    const types = availableOverlays(layer);
    if (!types.length) return;
    overlayBaseLayer = layer;
    const visibility = overlayVisibility(layer);
    const entries = {};
    for (const type of types) {
        rebuildOverlay(type, layer);
        entries[escapeHtml(mapText('tectonicus.overlay.' + type, {}, layer))] = overlayGroup(type);
        if (visibility[type]) overlayGroup(type).addTo(mymap);
    }
    overlayControl = L.control.layers(null, entries, {collapsed: true}).addTo(mymap);
    const container = overlayControl.getContainer();
    container.classList.add('overlay-control');
    const toggle = container.querySelector('.leaflet-control-layers-toggle');
    toggle.classList.add('overlay-control-toggle');
    toggle.setAttribute('data-map-label', 'tectonicus.controls.overlays');
    toggle.setAttribute('title', mapText('tectonicus.controls.overlays', {}, layer));
    localizeMapControls();
}

function overlayTypeOf(group) {
    for (const [type, candidate] of overlayGroups) if (candidate === group) return type;
}

function onOverlayToggled(e) {
    const type = overlayTypeOf(e.layer);
    if (!type || !overlayBaseLayer) return;
    overlayVisibility(overlayBaseLayer)[type] = e.type === 'overlayadd';
    if (e.type === 'overlayadd' && dynamicOverlays[type]) rebuildOverlay(type, overlayBaseLayer);
}

function onOverlayViewChanged() {
    if (!overlayBaseLayer) return;
    for (const type of Object.keys(dynamicOverlays)) {
        if (mymap.hasLayer(overlayGroup(type))) rebuildOverlay(type, overlayBaseLayer);
    }
}

function initOverlays() {
    mymap.on('overlayadd overlayremove', onOverlayToggled);
    mymap.on('moveend', onOverlayViewChanged);
}
