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
                const marker = {point, options, addTo() { this.added = true; return this; }, remove() { this.removed = true; },
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
            layerId: `World${index}.day`,
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
    for (const index of [0, 1, 0]) {
        viewer.selectWorld(index);
        const prefix = `World${index}/Images/`;
        const html = viewer.context.createChestPopup({name: 'Chest', items: [item]});
        assert.ok(html.includes(prefix + 'SmallChest.png'));
        assert.ok(viewer.context.createChestPopup({name: 'Large Chest', large: true, items: []}).includes(prefix + 'LargeChest.png'));
        const player = {name: 'Explorer', icon: prefix + 'PlayerIcons/Explorer.png', donation: '',
            health: 20, food: 20, air: 300, xpLevel: 12,
            inventory: [{...item, slot: 103}], enderChestInventory: [item]};
        for (const markup of [html, viewer.context.createItemHtml(item, 1, 1),
            viewer.context.getInventoryHtml(player), viewer.context.getEnderChestHtml(player),
            viewer.context.getPlayerHtml(player)]) {
            for (const name of ['Items/leather_helmet.png', 'Items/leather_helmet_overlay.png']) {
                assert.ok(markup.includes(prefix + name), name);
            }
            assert.match(markup, /class="mc_text_container item_count"/);
            assert.match(markup, /class="item_description"/);
            assert.match(markup, /background-color: #123456/);
            const urls = Array.from(markup.matchAll(/(?:src="|url\(')([^"')]+)/g), match => match[1]);
            assert.ok(urls.length > 0);
            assert.ok(urls.every(url => url.startsWith(prefix) || url === 'Images/Spacer.png'), urls.join(', '));
            assert.ok(!markup.includes(`World${1 - index}/Images/`));
            assert.doesNotMatch(markup, /null|undefined/);
        }
        assert.ok(viewer.context.getHealthHtml({health: 20}).includes(prefix + 'FullHeart.png'));
        assert.ok(viewer.context.getFoodHtml({food: 20}).includes(prefix + 'FullFood.png'));
        assert.ok(viewer.context.getAirHtml({air: 300}).includes(prefix + 'FullAir.png'));
        viewer.context.refreshSignMarkers(viewer.context.selectedLayer, false);
        assert.equal(viewer.markers.at(-1).options.icon.iconUrl, prefix + 'Sign.png');
        assert.ok(!html.includes(`World${1 - index}/Images/`));
    }
});

test('ender chest markers use world icons, show block coordinates and follow the chest toggle', () => {
    const viewer = createAssetsViewer();
    for (const index of [0, 1, 0]) {
        viewer.selectWorld(index);
        const layer = viewer.context.selectedLayer;
        layer.chests = [
            {enderChest: true, worldPos: {x: -16.5, y: -36, z: -0.5}},
            {name: 'Chest', items: [], worldPos: {x: 8.5, y: 64, z: 8.5}}
        ];
        viewer.context.refreshChestMarkers(layer, false);
        const [enderChest, chest] = viewer.markers.slice(-2);
        assert.equal(enderChest.options.icon.iconUrl, `World${index}/Images/EnderChest.png`);
        assert.equal(chest.options.icon.iconUrl, `World${index}/Images/Chest.png`);
        assert.equal(enderChest.point, layer.chests[0].worldPos);
        assert.match(enderChest.html, /Ender Chest/);
        assert.match(enderChest.html, /position \(-17, -36, -1\)/);
        assert.doesNotMatch(enderChest.html, /inventory_slot|SmallChest.png|undefined/);
        assert.match(chest.html, /SmallChest.png/);
        assert.equal(viewer.evaluate('chestMarkers.length'), 2);
        assert.ok(!enderChest.added && !chest.added);
        viewer.context.refreshChestMarkers(layer, true);
        assert.ok(enderChest.removed && chest.removed);
        assert.ok(viewer.markers.slice(-2).every(marker => marker.added));
    }
});

test('item names, descriptions and enchantments use each world translations after switching back', async () => {
    const viewer = createAssetsViewer(async url => {
        const world = url.includes('World0') ? 'Survival' : 'Creative';
        return {ok: true, json: async () => ({
            'item.minecraft.diamond_sword': world + ' Sword',
            'item.minecraft.diamond_sword.desc': world + ' Description',
            'enchantment.minecraft.sharpness': world + ' Sharpness'
        })};
    });
    await viewer.context.containersMainAsync();
    const item = {id: 'minecraft:diamond_sword', count: 1, slot: 0,
        enchantments: [{id: 'minecraft:sharpness', level: 5}]};
    for (const index of [0, 1, 0]) {
        viewer.selectWorld(index);
        const world = index === 0 ? 'Survival' : 'Creative';
        for (const html of [viewer.context.createItemHtml(item, 1, 1),
            viewer.context.createChestPopup({name: 'Chest', items: [item]}),
            viewer.context.getInventoryHtml({inventory: [item]}),
            viewer.context.getEnderChestHtml({enderChestInventory: [item]})]) {
            assert.ok(html.includes(viewer.context.renderMinecraftText(world + ' Sword', 'name enchanted')));
            assert.ok(html.includes(viewer.context.renderMinecraftText(world + ' Description')));
            assert.ok(html.includes(viewer.context.renderMinecraftText(world + ' Sharpness V')));
            assert.ok(html.includes(`World${index}/Images/Items/diamond_sword.png`));
        }
    }
});

test('trimmed armor, potion overlays and glint masks keep the selected world image paths', () => {
    const viewer = createAssetsViewer();
    const items = [
        {id: 'minecraft:leather_chestplate', count: 1, color: 0x123456,
            trim: {material: 'minecraft:gold', pattern: 'minecraft:spire'},
            enchantments: [{id: 'minecraft:protection', level: 4}]},
        {id: 'minecraft:potion', count: 1, components: {potionContents: {potion: 'minecraft:healing'}}},
        {id: 'minecraft:enchanted_golden_apple', count: 1}
    ];
    for (const index of [0, 1, 0]) {
        viewer.selectWorld(index);
        const prefix = `World${index}/Images/Items/`;
        const [armor, potion, apple] = items.map(item => viewer.context.createItemHtml(item, 1, 1));
        assert.ok(armor.includes('src="' + prefix + 'leather_chestplate_gold_trim.png"'));
        assert.ok(armor.includes('src="' + prefix + 'leather_chestplate_gold_trim_overlay.png"'));
        assert.ok(armor.includes("mask-image: url('" + prefix + "leather_chestplate.png')"));
        assert.ok(armor.includes("mask-image: url('" + prefix + "leather_chestplate_gold_trim.png')"));
        assert.ok(potion.includes('src="' + prefix + 'potion.png"'));
        assert.ok(potion.includes('src="' + prefix + 'potion_overlay.png"'));
        assert.match(potion, /background-color: #f82423/);
        assert.ok(apple.includes("mask-image: url('" + prefix + "enchanted_golden_apple.png')"));
    }
});

test('item rendering retains shared Images paths for older map exports', () => {
    const viewer = createAssetsViewer();
    viewer.evaluate('activeBaseLayer = null;');
    const item = {id: 'minecraft:stone', count: 2, slot: 0};
    for (const html of [viewer.context.createItemHtml(item, 1, 1),
        viewer.context.createChestPopup({name: 'Chest', items: [item]})]) {
        assert.ok(html.includes('src="Images/Items/stone.png"'));
        assert.match(html, /item_count/);
        assert.match(html, /item_description/);
        assert.doesNotMatch(html, /null|undefined/);
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
