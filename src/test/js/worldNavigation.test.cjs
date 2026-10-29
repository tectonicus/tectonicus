const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

async function createViewer(contents, fragment = '') {
    let currentFragment = fragment;
    const handlers = {};
    const controls = new Set();
    const element = () => ({style: {setProperty(name, value) { this[name] = value; }}, children: [], _tippy: {setContent() {}},
        setAttribute(name, value) { this[name] = value; }});
    const mapElement = element();
    const map = {
        center: {lat: 0, lng: 0}, zoom: 0, maxZoom: 8,
        layers: new Set(),
        on(name, listener) { handlers[name] = listener; },
        fireEvent(name, event) { handlers[name]?.(event); },
        getCenter() { return this.center; },
        getZoom() { return this.zoom; },
        setView(point, zoom) {
            this.center = {lat: point.lat ?? point[0], lng: point.lng ?? point[1]};
            this.zoom = Math.min(zoom, this.maxZoom);
            handlers.moveend?.();
            return this;
        },
        setMaxZoom(zoom) {
            this.maxZoom = zoom;
            if (this.zoom > zoom) {
                this.zoom = zoom;
                handlers.zoomend?.();
            }
        },
        removeLayer(layer) { this.layers.delete(layer); },
        addControl(control) { control.addTo(this); }
    };
    const context = vm.createContext({
        contents, maxZoom: 8, showSpawn: false,
        window: Object.assign(new EventTarget(), {history: {replaceState(state, title, url) { this.url = url; }}}),
        document: {querySelector: selector => selector === '#map' ? mapElement : null},
        MutationObserver: class { observe() {} },
        tippy() {}, stats: {peakMemoryBytes: 0},
        containersMainAsync: async () => {},
        getQueryParams: () => ({}),
        getFragmentParams: () => Object.fromEntries(new URLSearchParams(currentFragment)),
        WorldCoord: function(x, y, z) { Object.assign(this, {x, y, z}); },
        MinecraftProjection: function() {
            this.worldToMap = position => [position.x, position.z];
            this.mapToWorld = point => ({x: point.lat, z: point.lng});
        },
        controlState: {playerControlChecked: true},
        ...Object.fromEntries(['views', 'signs', 'players', 'beds', 'respawnAnchors', 'beacons', 'portals', 'spawn', 'chests']
            .map(name => [name + 'InitiallyVisible', true]))
    });
    function Control(definition = {}) { Object.assign(this, definition); }
    Control.prototype.addTo = function() {
        this._container = this.onAdd ? this.onAdd(map) : element();
        controls.add(this);
        return this;
    };
    Control.prototype.remove = function() { controls.delete(this); };
    Control.prototype.getContainer = function() { return this._container; };
    Control.extend = definition => function() {
        return Object.assign(new Control(), definition);
    };
    function TileLayer() {}
    TileLayer.prototype._isValidTile = () => true;
    TileLayer.extend = definition => {
        function Layer(...args) { definition.initialize.apply(this, args); }
        Layer.prototype = Object.assign(Object.create(TileLayer.prototype), definition, {
            addTo() {
                map.layers.add(this);
                if ([...controls].some(control => Object.values(control.baseMaps || {}).includes(this))) {
                    handlers.baselayerchange?.({layer: this});
                }
                return this;
            }
        });
        return Layer;
    };
    context.L = {
        map: () => map, CRS: {Simple: {}}, TileLayer, Control,
        setOptions(layer, options) { layer.options = Object.assign(Object.create(layer.options || null), options); },
        DomUtil: {toFront() {}, create(tag, className, parent) {
            const node = Object.assign(element(), {tag, className});
            parent?.children.push(node);
            return node;
        }},
        DomEvent: {disableClickPropagation() {}, disableScrollPropagation() {}},
        control: {
            layers: baseMaps => new Control({baseMaps}),
            attribution: () => new Control({
                setPrefix() { return this; },
                addTo() { map.attributionControl = {getContainer: element}; return this; }
            })
        }
    };
    for (const file of ['controls.js', 'main.js']) {
        vm.runInContext(fs.readFileSync(path.join(__dirname, '../../main/resources', file), 'utf8'), context);
    }
    context.CreateResetViewButton = () => {};
    context.CreateCompassControl = () => new Control();
    await context.mainAsync();
    return {
        context, map, controls, mapElement,
        setFragment: value => { currentFragment = value; },
        evaluate: code => vm.runInContext(code, context),
        layerLabels: () => Object.keys([...controls].find(control => control.baseMaps)?.baseMaps || {})
    };
}

function dimension(id, worldId, layerNames = ['Day', 'Night'], extra = {}) {
    return {
        id, worldId, name: 'Surface', worldName: worldId,
        worldVectors: {startView: {x: 10, y: 64, z: 20}},
        worldStats: {}, controlState: {playerControlChecked: true},
        signs: [], players: [], chests: [], views: [], portals: [], beds: [], respawnAnchors: [], beacons: [],
        layers: layerNames.map((name, index) => ({id: `${id}_${index}`, name})),
        ...extra
    };
}

test('tile URLs share a world folder while dimension resources remain separate', async () => {
    const viewer = await createViewer([
        dimension('surface', 'World0', ['Day'], {
            outputId: 'World0', compass: 'Scripts/surface/Compass.png',
            layers: [{id: 'World0.Layer0', directory: 'Layer0', name: 'Day', imageFormat: 'png'}]
        }),
        dimension('nether', 'World0', ['Nether'], {
            outputId: 'World0', compass: 'Scripts/nether/Compass.png',
            layers: [{id: 'World0.Layer1', directory: 'Layer1', name: 'Nether', imageFormat: 'png'}]
        })
    ]);
    assert.equal(viewer.evaluate("tileLayers.get('World0.Layer0').getTileUrl({x: 1, y: 2, z: 3})"),
        'World0/Layer0/Zoom3/1/2/tile_1_2.png');
    assert.equal(viewer.evaluate("tileLayers.get('World0.Layer1').getTileUrl({x: 1, y: 2, z: 3})"),
        'World0/Layer1/Zoom3/1/2/tile_1_2.png');
    assert.equal(viewer.evaluate("tileLayers.get('World0.Layer1').compassUrl"), 'Scripts/nether/Compass.png');
});

test('world selection filters flat layers and restores the selected layer and camera in each world', async () => {
    const viewer = await createViewer([dimension('a', 'Survival'), dimension('b', 'Creative', ['Day'])]);
    const {context, map, evaluate} = viewer;
    assert.equal(evaluate('worldControl.select.children.length'), 2);
    assert.deepEqual(viewer.layerLabels(), ['Surface - Day', 'Surface - Night']);
    evaluate("selectBaseLayer(tileLayers.get('a_1'))");
    map.setView([120, -80], 7);
    context.switchWorld('Creative');
    assert.equal(evaluate('activeBaseLayer.layerId'), 'b_0');
    assert.deepEqual(viewer.layerLabels(), []);
    map.setView([-600, 300], 2);
    context.switchWorld('Survival');
    assert.equal(evaluate('activeBaseLayer.layerId'), 'a_1');
    assert.equal(map.center.lat, 120);
    assert.equal(map.center.lng, -80);
    assert.equal(map.zoom, 7);
    context.switchWorld('Creative');
    assert.equal(map.center.lat, -600);
    assert.equal(map.center.lng, 300);
    assert.equal(map.zoom, 2);
});

test('one-world outputs omit the world selector and disambiguate duplicate names without editing data', async () => {
    const contents = [dimension('a', 'Survival', ['Day', 'Day', 'Day'])];
    const viewer = await createViewer(contents);
    assert.equal(viewer.evaluate('worldControl'), null);
    assert.deepEqual(viewer.layerLabels(), ['Surface - Day', 'Surface - Day (2)', 'Surface - Day (3)']);
    assert.deepEqual(contents[0].layers.map(layer => layer.name), ['Day', 'Day', 'Day']);
});

test('marker overrides survive lighting, dimension and world switches while other worlds keep their own defaults', async () => {
    const viewer = await createViewer([
        dimension('a', 'Survival'),
        dimension('nether', 'Survival', ['Nether'], {controlState: {playerControlChecked: false}}),
        dimension('b', 'Creative')
    ]);
    const {context, evaluate} = viewer;
    evaluate("selectBaseLayer(tileLayers.get('nether_0'))");
    assert.equal(evaluate('controlState.playerControlChecked'), false);
    evaluate("selectBaseLayer(tileLayers.get('a_0'))");
    assert.equal(evaluate('controlState.playerControlChecked'), true);
    context.setMarkerVisibility('playerControlChecked', false);
    evaluate("selectBaseLayer(tileLayers.get('a_1'))");
    assert.equal(evaluate('controlState.playerControlChecked'), false);
    context.switchWorld('Creative');
    assert.equal(evaluate('controlState.playerControlChecked'), true);
    context.switchWorld('Survival');
    assert.equal(evaluate('controlState.playerControlChecked'), false);
});

test('deep links select the correct world, clamp zoom and use the target dimension origin', async () => {
    const viewer = await createViewer([
        dimension('a', 'Survival'),
        dimension('b', 'Creative', ['Day'], {maxZoom: 3, worldVectors: {startView: {x: 1000, y: 90, z: -500}}})
    ], 'layer=creative.day&zoom=7');
    assert.equal(viewer.evaluate('activeBaseLayer.layerId'), 'b_0');
    assert.equal(viewer.evaluate('worldControl.select.value'), 'Creative');
    assert.equal(viewer.map.center.lat, 1000);
    assert.equal(viewer.map.center.lng, -500);
    assert.equal(viewer.map.zoom, 3);
});

test('switching from a highly zoomed world does not overwrite a target world camera during zoom clamping', async () => {
    const viewer = await createViewer([dimension('a', 'Survival'), dimension('b', 'Creative', ['Day'], {maxZoom: 2})]);
    viewer.map.setView([500, 600], 8);
    viewer.context.switchWorld('Creative');
    assert.equal(viewer.map.center.lat, 10);
    assert.equal(viewer.map.center.lng, 20);
    assert.equal(viewer.map.zoom, 0);
    viewer.context.switchWorld('Survival');
    assert.equal(viewer.map.center.lat, 500);
    assert.equal(viewer.map.zoom, 8);
});

test('invalid initial layer links fall back to the first configured layer', async () => {
    const viewer = await createViewer([dimension('a', 'Survival')], 'layer=missing&zoom=oops');
    assert.equal(viewer.evaluate('activeBaseLayer.layerId'), 'a_0');
    assert.equal(viewer.map.zoom, 0);
});

test('friendly deep links select the world and layer without changing tile paths', async () => {
    const viewer = await createViewer([
        dimension('a', 'Survival'), dimension('b', 'Creative')
    ], 'layer=creative.night&x=120&z=-80&zoom=3');
    assert.equal(viewer.evaluate('activeBaseLayer.layerId'), 'b_1');
    assert.equal(viewer.evaluate('worldControl.select.value'), 'Creative');
    assert.equal(viewer.map.center.lat, 120);
    assert.equal(viewer.map.zoom, 3);
    assert.equal(viewer.evaluate('window.history.url'), '#layer=creative.night&x=120&z=-80&zoom=3');
    assert.equal(viewer.evaluate('activeBaseLayer.getTileUrl({x: 0, y: 0, z: 0})').split('/').slice(0, 2).join('/'), 'b/b_1');
});

test('internal IDs are not accepted as URL aliases', async () => {
    const viewer = await createViewer([dimension('Map0', 'Survival')], 'layer=Map0_1');
    assert.equal(viewer.evaluate('activeBaseLayer.layerId'), 'Map0_0');
    assert.match(viewer.evaluate('window.history.url'), /^#layer=survival.day&/);
    viewer.setFragment('layer=survival.day&x=25&z=50&zoom=2');
    viewer.context.onHashChange();
    assert.equal(viewer.evaluate('activeBaseLayer.layerId'), 'Map0_0');
    assert.equal(viewer.map.center.lat, 25);
    assert.equal(viewer.map.center.lng, 50);
});

test('friendly identifiers disambiguate names at each scope and reserve existing numeric suffixes', async () => {
    const viewer = await createViewer([
        dimension('a', 'world-a', ['Day', 'DAY', 'Day1'], {worldName: 'Block Gallery', name: 'Overworld'}),
        dimension('b', 'world-a', ['Day'], {worldName: 'Block Gallery', name: 'OVERWORLD'}),
        dimension('c', 'world-b', ['Day'], {worldName: 'Block Gallery', name: 'Overworld'})
    ]);
    const ids = viewer.evaluate('[...tileLayers.values()].map(layer => layer.urlId)');
    assert.deepEqual(Array.from(ids), [
        'block-gallery.day', 'block-gallery.day2', 'block-gallery.day1',
        'block-gallery.day3', 'block-gallery1.day'
    ]);
});

test('friendly identifiers normalize punctuation and accents and handle empty slugs', async () => {
    const viewer = await createViewer([
        dimension('a', 'a', ['Dáy / Night', '?!', '世界'], {worldName: ' My Wörld! ', name: 'A.B / C'})
    ]);
    assert.deepEqual(Array.from(viewer.evaluate('[...tileLayers.values()].map(layer => layer.urlId)')), [
        'my-world.day-night', 'my-world.layer', 'my-world.layer1'
    ]);
});

test('world switches update control images, font and enchanted glint without changing tile paths', async () => {
    const viewer = await createViewer([
        dimension('a', 'World0', ['Day'], {images: 'World0/Images/', localizations: 'Scripts/World0/localizations.json'}),
        dimension('b', 'World1', ['Day'], {images: 'World1/Images/', localizations: 'Scripts/World1/localizations.json'})
    ]);
    for (const id of ['World0', 'World1', 'World0']) {
        viewer.context.switchWorld(id);
        assert.equal(viewer.evaluate('worldImageUrl("Items/stone.png")'), `${id}/Images/Items/stone.png`);
        assert.equal(viewer.evaluate('activeBaseLayer.localizationsUrl'), `Scripts/${id}/localizations.json`);
        assert.equal(viewer.mapElement.style['--minecraft-font'], `url("${id}/Images/Font.png")`);
        assert.equal(viewer.mapElement.style['--enchanted-glint'], `url("${id}/Images/EnchantedGlint.png")`);
        assert.equal(viewer.evaluate('CreateToggleControl("chests", "Chest.png", [], true).addTo(mymap).getContainer().src'),
            `${id}/Images/Chest.png`);
    }
});
