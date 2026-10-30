const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

function createViewer(yOffset = 64) {
    const corner = {children: []};
    const zoomContainer = {children: ['zoom-in', 'zoom-out'], parentNode: corner, offsetTop: 10, offsetHeight: 64};
    corner.children.push(zoomContainer);
    const entries = [new URL('https://example.test/maps/index.html?theme=dark')];
    const state = {theme: 'dark'};
    const location = {
        get href() { return entries.at(-1).href; },
        get hash() { return entries.at(-1).hash; },
        set hash(fragment) {
            const url = new URL(this.href);
            url.hash = fragment;
            entries.push(url);
        }
    };
    const history = {
        state,
        replaceState(newState, title, url) {
            this.state = newState;
            entries[entries.length - 1] = new URL(url, location.href);
        }
    };
    const map = {
        center: {lat: 0, lng: 0},
        zoom: 4,
        layers: new Set(),
        zoomControl: {getContainer: () => zoomContainer},
        getCenter() { return this.center; },
        getZoom() { return this.zoom; },
        addControl() {},
        setView(point, zoom) {
            this.center = {lat: point[0], lng: point[1]};
            this.zoom = zoom;
            context.onProjectionChanged();
        },
        removeLayer(layer) { this.layers.delete(layer); }
    };
    const window = Object.assign(new EventTarget(), {location, history});
    const context = vm.createContext({
        L: {
            map: () => map,
            CRS: {Simple: {}},
            Point: function(x, y) { this.x = x; this.y = y; },
            Control: {extend: definition => function() { Object.assign(this, definition); }},
            DomUtil: {create(tag, className, parent) {
                const element = {style: {}, tag, className, children: [], parentNode: parent,
                    setAttribute(name, value) { this[name] = value; }};
                if (parent) parent.children.push(element);
                return element;
            }},
            DomEvent: {disableClickPropagation() {}, preventDefault: event => event.preventDefault()}
        },
        maxZoom: 8,
        window,
		URL,
        URLSearchParams
    });
    for (const file of ['math.js', 'minecraftProjection.js', 'controls.js', 'marker.js', 'main.js']) {
        const source = fs.readFileSync(path.join(__dirname, '../../main/resources', file), 'utf8')
            .replace(/\{\{maxZoom\}\}/g, '8').replace(/\{\{[^}]+\}\}/g, 'false');
        vm.runInContext(source, context);
    }
    const vectors = {
        origin: {x: 0, y: 0},
        xAxis: {x: 0.35355338, y: 0.25},
        yAxis: {x: 0, y: -0.5},
        zAxis: {x: -0.35355338, y: 0.25},
        startView: {x: 0, y: 64, z: 0},
        yOffset
    };
    const layer = {
        layerId: 'LayerA',
        urlId: 'world.day',
        worldVectors: vectors,
        projection: new context.MinecraftProjection(vectors),
        viewPosition: {}
    };
    context.layer = layer;
    map.layers.add(layer);
    vm.runInContext('tileLayers.set(layer.layerId, layer); urlLayers.set(layer.urlId, layer); activeBaseLayer = layer;', context);
    return {context, map, layer, vectors, entries, history, state, window};
}

test('marker links retain query parameters, select the marker layer and center on its projected position', () => {
    const {context, map, layer, entries} = createViewer();
    for (const pos of [{x: -16.5, y: -36, z: -0.5}, {x: 100.5, y: 300, z: -80.5}]) {
        const originalUrl = entries[0].href;
        const url = new URL(context.markerShareLink(pos, layer));
        assert.equal(url.search, '?theme=dark');
        assert.equal(entries[0].href, originalUrl);
        const params = Object.fromEntries(new URLSearchParams(url.hash.slice(1)));
        assert.equal(params.layer, layer.urlId);
        assert.equal(Number(params.zoom), map.zoom);
        const target = layer.projection.worldToMap(pos);
        const restored = context.findStartView(params, layer.layerId).startPoint;
        assert.ok(Math.abs(target[0] - restored[0]) < 0.5);
        assert.ok(Math.abs(target[1] - restored[1]) < 0.5);
    }
});

test('panning and zooming update the URL without adding history entries or losing query parameters', () => {
    const {context, map, entries, history, state} = createViewer();
    for (let i = 0; i < 4; i++) {
        map.center = {lat: i, lng: i};
        map.zoom = i;
        context.onProjectionChanged();
        assert.equal(entries.length, 1);
        assert.equal(entries[0].pathname, '/maps/index.html');
        assert.equal(entries[0].search, '?theme=dark');
        const params = new URLSearchParams(entries[0].hash.substring(1));
        assert.equal(params.get('layer'), 'world.day');
        assert.equal(params.get('zoom'), String(i));
    }
    assert.equal(history.state, state);
});

for (const yOffset of [0, 64]) {
    test(`shared integer coordinates remain stable across repeated reloads with yOffset=${yOffset}`, () => {
        const {context, map, layer, entries} = createViewer(yOffset);
        for (const worldPos of [{x: -1000, y: 64, z: -10}, {x: 1000, y: 64, z: 10}]) {
            let point = layer.projection.worldToMap(worldPos);
            for (let reload = 0; reload < 5; reload++) {
                map.center = {lat: point[0], lng: point[1]};
                context.onProjectionChanged();
                const params = Object.fromEntries(new URLSearchParams(entries.at(-1).hash.substring(1)));
                assert.equal(Number(params.x), worldPos.x);
                assert.equal(params.y, undefined);
                assert.equal(Number(params.z), worldPos.z);
                const restored = context.findStartView(params, layer.layerId);
                assert.equal(restored.zoom, map.zoom);
                assert.equal(restored.worldPos.y, worldPos.y);
                point = restored.startPoint;
            }
        }
    });
}

test('editing fragment coordinates moves the map without adding another history entry', () => {
    const {map, layer, window, entries} = createViewer();
    window.location.hash = '#layer=world.day&x=-1000&z=250&zoom=6';
    window.dispatchEvent(new Event('hashchange'));
    const expected = layer.projection.worldToMap({x: -1000, y: 64, z: 250});
    assert.equal(map.center.lat, expected[0]);
    assert.equal(map.center.lng, expected[1]);
    assert.equal(map.zoom, 6);
    assert.equal(entries.length, 2);
});

test('editing the layer switches base layers and uses the target map height', () => {
    const {context, map, layer, vectors, window} = createViewer();
    const control = {remove() {}, setChecked() {}};
    for (const name of ['spawn', 'sign', 'view', 'player', 'portal', 'bed', 'respawnAnchor', 'beacon', 'container']) {
        context[name + 'ToggleControl'] = control;
    }
    context.compassControl = control;
    context.CreateCompassControl = () => control;
    context.tippy = () => {};
    context.stats = {peakMemoryBytes: 0};
    context.document = {querySelector: selector => selector === '#map' ? {style: {setProperty() {}}} : null};
    const targetVectors = {...vectors, startView: {x: 200, y: 90, z: 300}};
    const targetLayer = {
        layerId: 'LayerB',
        urlId: 'world.night',
        worldVectors: targetVectors,
        projection: new context.MinecraftProjection(targetVectors),
        controlState: {},
        worldStats: {},
        signs: [], players: [], containers: [], views: [], portals: [], beds: [], anchors: [], beacons: [],
        addTo(map) {
            map.layers.add(this);
            // Leaflet's layer control fires baselayerchange when the new base layer is added.
            context.onBaseLayerChange({layer: this});
        }
    };
    context.targetLayer = targetLayer;
    vm.runInContext('tileLayers.set(targetLayer.layerId, targetLayer); urlLayers.set(targetLayer.urlId, targetLayer);', context);
    window.location.hash = '#layer=world.night&x=100&y=12&z=-200&zoom=5';
    window.dispatchEvent(new Event('hashchange'));
    const expected = targetLayer.projection.worldToMap({x: 100, y: 90, z: -200});
    assert.equal(map.layers.has(layer), false);
    assert.equal(map.layers.has(targetLayer), true);
    assert.equal(map.center.lat, expected[0]);
    assert.equal(map.center.lng, expected[1]);
    assert.equal(map.zoom, 5);
    assert.equal(targetLayer.viewPosition.worldPos.y, 90);
    assert.equal(context.findStartView({layer: 'world.night', x: '100', y: '12', z: '-200'}, layer.layerId).worldPos.y, 90);
});

test('URL y values are ignored when loading and editing coordinates', () => {
    const {context, map, layer, vectors, window} = createViewer();
    const expected = layer.projection.worldToMap({x: -1000, y: vectors.startView.y, z: 250});
    for (const y of ['200', 'invalid']) {
        const restored = context.findStartView({x: '-1000', y, z: '250'}, layer.layerId);
        assert.equal(restored.worldPos.y, vectors.startView.y);
        assert.equal(restored.startPoint[0], expected[0]);
        assert.equal(restored.startPoint[1], expected[1]);

        window.location.hash = `#layer=world.day&x=-1000&y=${y}&z=250&zoom=6`;
        window.dispatchEvent(new Event('hashchange'));
        assert.equal(map.center.lat, expected[0]);
        assert.equal(map.center.lng, expected[1]);
        assert.equal(map.zoom, 6);
    }
});

test('the reset button below zoom resets the active layer without switching layers or reloading', () => {
    const {context, map, layer, vectors, entries, window} = createViewer();
    const otherVectors = {...vectors, startView: {x: 200, y: 90, z: 300}};
    const otherLayer = {
        layerId: 'LayerB',
        urlId: 'world.night',
        worldVectors: otherVectors,
        projection: new context.MinecraftProjection(otherVectors),
        viewPosition: {}
    };
    const location = window.location;
    const button = context.CreateResetViewButton(map);
    const zoomContainer = map.zoomControl.getContainer();
    assert.deepEqual(zoomContainer.children, ['zoom-in', 'zoom-out']);
    assert.equal(button.parentNode.parentNode, zoomContainer.parentNode);
    assert.equal(button.parentNode.className, 'leaflet-bar leaflet-control leaflet-control-reset');
    for (const selectedLayer of [layer, otherLayer]) {
        context.selectedLayer = selectedLayer;
        vm.runInContext('activeBaseLayer = selectedLayer;', context);
        map.layers = new Set([selectedLayer]);
        for (let reset = 0; reset < 2; reset++) {
            map.center = {lat: -500 - reset, lng: 1000 + reset};
            map.zoom = 6;
            context.onProjectionChanged();
            const click = new Event('click', {cancelable: true});
            button.onclick(click);
            assert.equal(click.defaultPrevented, true);

            const expected = selectedLayer.projection.worldToMap(selectedLayer.worldVectors.startView);
            assert.equal(map.center.lat, expected[0]);
            assert.equal(map.center.lng, expected[1]);
            assert.equal(map.zoom, 0);
            assert.deepEqual([...map.layers], [selectedLayer]);
            assert.equal(window.location, location);
            assert.equal(entries.length, 1);
            assert.equal(entries[0].search, '?theme=dark');
            const params = new URLSearchParams(location.hash.slice(1));
            assert.equal(params.get('layer'), selectedLayer.urlId);
            assert.equal(Number(params.get('x')), selectedLayer.worldVectors.startView.x);
            assert.equal(Number(params.get('z')), selectedLayer.worldVectors.startView.z);
            assert.equal(params.get('zoom'), '0');
        }
    }
});

test('the compass is no longer a reset button', () => {
    const {context, map} = createViewer();
    const compass = context.CreateCompassControl('Compass.png').onAdd(map);
    assert.equal(compass.onclick, undefined);
});

test('invalid fragment edits leave the current view unchanged', () => {
    const {map, window} = createViewer();
    const center = map.center;
    for (const fragment of ['#layer=missing&x=10&z=20', '#x=invalid&z=20', '#x=10&z=20&zoom=invalid']) {
        window.location.hash = fragment;
        window.dispatchEvent(new Event('hashchange'));
        assert.equal(map.center, center);
        assert.equal(map.zoom, 4);
    }
});
