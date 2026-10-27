const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

async function createLayer(tileIndex) {
    const stop = new Error('Stop before creating viewer controls');
    let layer;
    function TileLayer() {}
    TileLayer.prototype._isValidTile = coords => coords.z >= 0;
    TileLayer.extend = definition => {
        function Tectonicus(...args) {
            definition.initialize.apply(this, args);
            layer = this;
        }
        Tectonicus.prototype = Object.assign(Object.create(TileLayer.prototype), definition, {
            addTo() { throw stop; }
        });
        return Tectonicus;
    };
    const map = {setView() { return this; }};
    const context = {
        L: {map: () => map, CRS: {Simple: {}}, TileLayer},
        maxZoom: 2,
        window: new EventTarget(),
        containersMainAsync: async () => {},
        getQueryParams: () => ({}),
        getFragmentParams: () => ({}),
        MinecraftProjection: function() { this.worldToMap = () => [0, 0]; },
        WorldCoord: function() {},
        controlState: {},
        contents: [{id: 'Map0', name: 'Test', worldVectors: {startView: {}},
            layers: [{id: 'LayerA', imageFormat: 'webp', tileIndex}]}]
    };
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/main.js'), 'utf8'), context);
    await assert.rejects(context.window.onload(), error => error === stop);
    return layer;
}

test('viewer skips outside tiles and holes while allowing range endpoints at every zoom', async () => {
    const layer = await createLayer([{'-3': [-18, -16, -10, -9, 0, 1, 3, 3]}, {}, {'2': [-1, -1]}]);
    for (let x = -22; x <= 22; x++) {
        const exists = [-18, -17, -16, -10, -9, 0, 1, 3].includes(x);
        assert.equal(layer._isValidTile({x, y: -3, z: 0}), exists, `x=${x}`);
    }
    assert.equal(layer._isValidTile({x: -18, y: -2, z: 0}), false);
    assert.equal(layer._isValidTile({x: -18, y: -3, z: 1}), false);
    assert.equal(layer._isValidTile({x: -1, y: 2, z: 2}), true);
    assert.equal(layer._isValidTile({x: -1, y: 2, z: 3}), false);
    assert.equal(layer.getTileUrl({x: -18, y: -3, z: 0}), 'Map0/LayerA/Zoom0/-2/-3/tile_-18_-3.webp');
});

test('empty coverage rejects all requests', async () => {
    const layer = await createLayer([{}, {}, {}]);
    assert.equal(layer._isValidTile({x: 0, y: 0, z: 0}), false);
});

test('older contents remain compatible and Leaflet validation is preserved', async () => {
    const layer = await createLayer(undefined);
    assert.equal(layer._isValidTile({x: -5, y: -5, z: 0}), true);
    assert.equal(layer._isValidTile({x: -5, y: -5, z: -1}), false);
});
