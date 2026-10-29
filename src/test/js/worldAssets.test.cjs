const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

function createAssetsViewer(fetch) {
    const markers = [];
    const images = [];
    const context = vm.createContext({
        fetch, console: {error() {}},
        contents: [
            {localizations: 'Scripts/World0/localizations.json'},
            {localizations: 'Scripts/World0/localizations.json'},
            {localizations: 'Scripts/World1/localizations.json'}
        ],
        window: new EventTarget(),
        Image: function() { images.push(this); },
        L: {
            map: () => ({}), CRS: {Simple: {}}, icon: options => options,
            marker(point, options) {
                const marker = {point, options, addTo() { return this; }, remove() {},
                    bindPopup(html) { this.html = html; return this; }, bindTooltip() { return this; }};
                markers.push(marker);
                return marker;
            }
        }
    });
    for (const file of ['containers.js', 'marker.js', 'main.js']) {
        const source = fs.readFileSync(path.join(__dirname, '../../main/resources', file), 'utf8')
            .replace(/\{\{maxZoom\}\}/g, '8').replace(/\{\{[^}]+\}\}/g, 'false');
        vm.runInContext(source, context);
    }
    function selectWorld(index) {
        context.selectedLayer = {
            images: `World${index}/Images/`, localizationsUrl: `Scripts/World${index}/localizations.json`,
            projection: {worldToMap: point => point},
            signs: [{worldPos: {x: 0, y: 64, z: 0}, text1: 'Sign'}]
        };
        vm.runInContext('activeBaseLayer = selectedLayer;', context);
    }
    selectWorld(0);
    return {context, markers, images, selectWorld, evaluate: code => vm.runInContext(code, context)};
}

test('translations are loaded once per world and remain distinct after switching worlds', async () => {
    const requests = [];
    const viewer = createAssetsViewer(async url => {
        requests.push(url);
        return {ok: true, json: async () => ({'item.minecraft.stone': url.includes('World0') ? 'Survival Stone' : 'Creative Stone'})};
    });
    await viewer.context.containersMainAsync();
    assert.deepEqual(requests, ['Scripts/World0/localizations.json', 'Scripts/World1/localizations.json']);
    for (const index of [0, 1, 0]) {
        viewer.selectWorld(index);
        assert.equal(viewer.context.localize('item.minecraft.stone'), index === 0 ? 'Survival Stone' : 'Creative Stone');
    }
});

test('a missing translation file falls back without discarding another world translations', async () => {
    const viewer = createAssetsViewer(async url => {
        if (url.includes('World1')) throw new Error('Missing file');
        return {ok: true, json: async () => ({stone: 'Stone'})};
    });
    await viewer.context.containersMainAsync();
    viewer.selectWorld(1);
    assert.equal(viewer.context.localize('stone', 'Fallback'), 'Fallback');
    viewer.selectWorld(0);
    assert.equal(viewer.context.localize('stone'), 'Stone');
});

test('chest images, item masks, overlays, player status and markers use the selected world assets', () => {
    const viewer = createAssetsViewer();
    const item = {id: 'minecraft:leather_helmet', count: 2, slot: 0, color: 0x123456,
        enchantments: [{id: 'minecraft:protection', level: 1}]};
    for (const index of [0, 1]) {
        viewer.selectWorld(index);
        const prefix = `World${index}/Images/`;
        const html = viewer.context.createChestPopup({name: 'Chest', items: [item]});
        for (const name of ['SmallChest.png', 'Items/leather_helmet.png', 'Items/leather_helmet_overlay.png']) {
            assert.ok(html.includes(prefix + name), name);
        }
        assert.ok(viewer.context.getHealthHtml({health: 20}).includes(prefix + 'FullHeart.png'));
        assert.ok(viewer.context.getFoodHtml({food: 20}).includes(prefix + 'FullFood.png'));
        assert.ok(viewer.context.getAirHtml({air: 300}).includes(prefix + 'FullAir.png'));
        viewer.context.refreshSignMarkers(viewer.context.selectedLayer, false);
        assert.equal(viewer.markers.at(-1).options.icon.iconUrl, prefix + 'Sign.png');
        assert.ok(!html.includes(`World${1 - index}/Images/`));
    }
});

test('delayed player icons cannot leak across worlds and fallback icons use their own world', () => {
    const viewer = createAssetsViewer();
    const player = {name: 'Explorer', donation: ''};
    const firstLayer = viewer.context.selectedLayer;
    viewer.context.loadIcon('World0/Images/PlayerIcons/Explorer.png', player, [0, 0], false, firstLayer);
    viewer.selectWorld(1);
    const secondLayer = viewer.context.selectedLayer;
    viewer.context.loadIcon('World1/Images/PlayerIcons/Explorer.png', player, [0, 0], false, secondLayer);
    viewer.images[0].onerror();
    assert.equal(viewer.markers.length, 0);
    viewer.images[1].onerror();
    assert.equal(viewer.markers.at(-1).options.icon.iconUrl, 'World1/Images/PlayerIcons/Tectonicus_Default_Player_Icon.png');
});
