const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

function viewer() {
    const markers = [];
    const context = vm.createContext({
        window: Object.assign(new EventTarget(), {navigator: {}}),
        URL, URLSearchParams,
        L: {map: () => ({}), CRS: {Simple: {}}, icon: options => options,
            marker: () => {
                const marker = {bindPopup(html) { this.html = html; return this; },
                    bindTooltip() { return this; }, remove() {}};
                markers.push(marker);
                return marker;
            }}
    });
    for (const file of ['containers.js', 'marker.js', 'main.js']) {
        vm.runInContext(fs.readFileSync(path.join(__dirname, '../../main/resources', file), 'utf8')
            .replace(/\{\{[^}]+\}\}/g, 'false'), context);
    }
    return {context, markers};
}

test('teleport uses world version and exact dimension, with a compatible fallback for older exports', () => {
    const {context} = viewer();
    const pos = {x: -16.5, y: -36, z: -0.5};
    for (const version of [0, 4, 12, undefined]) {
        assert.equal(context.markerTeleportCommand(pos, {dimensionId: 'minecraft:the_nether',
            worldStats: {minecraftVersion: version}}), '/tp -16.5 -35 -0.5');
    }
    for (const version of [13, 21, 264]) {
        for (const dimension of ['minecraft:overworld', 'minecraft:the_nether', 'minecraft:the_end', 'custom:moon/base']) {
            assert.equal(context.markerTeleportCommand(pos, {dimensionId: dimension,
                worldStats: {minecraftVersion: version}}), `/execute in ${dimension} run tp @s -16.5 -35 -0.5`);
        }
    }
    assert.equal(context.markerTeleportCommand(pos, {}), '/tp -16.5 -35 -0.5');
    const html = context.markerCopyControls(pos, {layerId: 'a" onclick="bad()'});
    assert.match(html, /data-copy-text="-17 -36 -1"/);
    assert.ok(!html.includes('data-copy-layer="a" onclick='));
});

test('every positioned marker gets the three copy actions outside its popup content', () => {
    const {context, markers} = viewer();
    const pos = {x: 8.5, y: 64, z: -0.5};
    const layer = {layerId: 'day', dimensionId: 'minecraft:the_end', worldStats: {minecraftVersion: 21},
        worldVectors: {spawnPosition: pos}, projection: {worldVectors: {spawnPosition: pos}, worldToMap: () => [0, 0]},
        signs: [{worldPos: pos, text1: 'Hello'}], views: [{worldPos: pos, text: 'View'}],
        beds: [{worldPos: pos}], anchors: [{worldPos: pos}], portals: [{worldPos: pos}],
        beacons: [{worldPos: pos, levels: 0}, {worldPos: pos, levels: 4, primaryEffect: 'none', secondaryEffect: 'none'}],
        containers: [{worldPos: pos, name: 'Chest', items: []}, {worldPos: pos, enderChest: true}]};
    vm.runInContext('showSpawn = true;', context);
    for (const refresh of ['SpawnMarker', 'SignMarkers', 'ViewMarkers', 'BedMarkers',
        'RespawnAnchorMarkers', 'BeaconMarkers', 'PortalMarkers', 'ContainerMarkers']) {
        context['refresh' + refresh](layer, false);
    }
    context.createPlayerMarker({name: 'Alex', worldPos: pos, donation: ''}, [0, 0], layer);
    assert.equal(markers.length, 11);
    for (const marker of markers) {
        assert.equal((marker.html.match(/<button /g) || []).length, 3);
        assert.match(marker.html, /Copy coords/);
        assert.match(marker.html, /Copy teleport/);
        assert.match(marker.html, /Copy map link/);
		assert.equal((marker.html.match(/<svg /g) || []).length, 3);
		for (const button of marker.html.match(/<button\b[^>]*>[\s\S]*?<\/button>/g)) {
			assert.match(button, /title="Copy [^"]+"/);
			assert.match(button, /aria-label="Copy [^"]+"/);
			assert.match(button, /aria-hidden="true"/);
			assert.match(button, /<\/svg><\/button>$/);
		}
        assert.match(marker.html, /data-copy-text="8 64 -1"/);
        assert.match(marker.html, /execute in minecraft:the_end run tp @s 8.5 65 -0.5/);
    }
});

for (const mode of ['clipboard', 'rejected', 'legacy', 'manual', 'legacyDenied']) {
    test(`copy feedback and fallback work with ${mode} clipboard access`, async () => {
        const {context} = viewer();
        let copied;
        let removed = false;
        let focused = false;
        const status = {};
        const button = {dataset: {copyText: '-17 64 230'},
            parentElement: {querySelector: () => status}, focus() { focused = true; }};
        if (mode === 'clipboard' || mode === 'rejected') {
            context.window.navigator.clipboard = {async writeText(text) {
                if (mode === 'rejected') throw new Error('Denied');
                copied = text;
            }};
        }
        const field = {style: {}, focus() {}, select() {}, remove() { removed = true; }};
        context.document = {createElement: () => field, body: {appendChild() {}},
            execCommand() {
                if (mode === 'legacyDenied') throw new Error('Denied');
                if (mode === 'manual') return false;
                copied = field.value;
                return true;
            }};
        context.window.prompt = (message, text) => { copied = text; };
        await context.copyMarkerValue(button);
        assert.equal(copied, '-17 64 230');
        assert.equal(status.textContent, ['manual', 'legacyDenied'].includes(mode) ? 'Text ready to copy' : 'Copied!');
        assert.equal(removed, mode !== 'clipboard');
        assert.equal(focused, mode !== 'clipboard');
    });
}
