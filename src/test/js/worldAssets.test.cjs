const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');
const layerGroup = require('./leafletLayerGroup.cjs');
const mapEnglish = require('../../main/resources/map_en_us.json');

function createAssetsViewer(fetch, {protocol, contents, loadScript} = {}) {
    const markers = [];
    const images = [];
    const map = {layers: new Set()};
    const context = vm.createContext({
        mapEnglish, URL,
        fetch, console: {error() {}},
        contents: contents ?? [
            {localizations: 'Scripts/World0/localizations.json'},
            {localizations: 'Scripts/World0/localizations.json'},
            {localizations: 'Scripts/World1/localizations.json'}
        ],
        window: Object.assign(new EventTarget(), {location: {protocol}}),
        document: {
            createElement: () => ({}),
            head: {appendChild(script) {
                if (loadScript(script.src, context)) script.onload();
                else script.onerror();
            }}
        },
        Image: function() { images.push(this); },
        L: {
            map: () => map, layerGroup, CRS: {Simple: {}}, icon: options => options,
            marker(point, options) {
                const marker = {point, options,
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
            worldId: `World${index}`,
            layerId: `World${index}.day`,
            images: `World${index}/Images/`, localizationsUrl: `Scripts/World${index}/localizations.json`,
            localizationsScript: contents?.find(dimension => dimension.worldId === `World${index}`)?.localizationsScript,
            projection: {worldToMap: point => point},
            signs: [{worldPos: {x: 0, y: 64, z: 0}, text1: 'Sign'}]
        };
        vm.runInContext('activeBaseLayer = selectedLayer;', context);
    }
    selectWorld(0);
    return {context, map, markers, images, selectWorld, evaluate: code => vm.runInContext(code, context)};
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

test('local maps load each world script once, keep masks isolated and skip legacy fetches', async () => {
    const requested = [];
    const first = {worldId: 'World0', localizationsScript: 'Scripts/World0/localizations.js', masksScript: 'Scripts/World0/masks.js'};
    const second = {worldId: 'World1', localizationsScript: 'Scripts/World1/localizations.js', masksScript: 'Scripts/World1/masks.js'};
    const mask = 'data:image/png;base64,AAAA';
    const viewer = createAssetsViewer(() => assert.fail('Offline exports must not use fetch'), {
        protocol: 'file:', contents: [first, first, second, {localizations: 'Scripts/localizations.json'}],
        loadScript(url, context) {
            requested.push(url);
            if (url === first.localizationsScript) {
                context.registerWorldLocalizations('World0', {translations: {'item.minecraft.leather_helmet': 'First Helmet'}, hasFont: true});
            } else if (url === first.masksScript) {
                context.registerWorldMasks('World0', {'Font.png': mask, 'Items/leather_helmet.png': mask});
            } else if (url === second.localizationsScript) {
                context.registerWorldLocalizations('World1', {translations: {'item.minecraft.leather_helmet': 'Casque é<&>'}, hasFont: false});
            } else {
                return false;
            }
            return true;
        }
    });
    await viewer.context.containersMainAsync();
    assert.deepEqual(requested, [first.localizationsScript, first.masksScript, second.localizationsScript, second.masksScript]);
    const item = {id: 'minecraft:leather_helmet', count: 1, color: 0x123456, enchantments: []};
    assert.equal(viewer.context.localize('item.minecraft.leather_helmet'), 'First Helmet');
    const firstHtml = viewer.context.createItemHtml(item, 1, 1);
    assert.match(firstHtml, /class="mc_char"/);
    assert.ok(firstHtml.includes(`mask-image: url('${mask}')`));
    assert.match(firstHtml, /class="enchanted_glint"/);
    assert.match(viewer.context.renderMinecraftText('漢字<&>'), /漢字&lt;&amp;&gt;/);

    viewer.selectWorld(1);
    const secondHtml = viewer.context.createItemHtml(item, 1, 1);
    assert.match(secondHtml, /Casque é&lt;&amp;&gt;/);
    assert.match(secondHtml, /World1\/Images\/Items\/leather_helmet\.png/);
    assert.doesNotMatch(secondHtml, /mask-image|data:image/);
    assert.match(viewer.context.renderMinecraftText('ASCII'), /mc_text_fallback.*ASCII/);
    viewer.selectWorld(0);
    assert.equal(viewer.context.worldMaskUrl('Font.png'), mask);
});

test('HTTP maps load translations as scripts and use regular PNGs for masks', async () => {
    const requested = [];
    const contents = [{worldId: 'World0', localizationsScript: 'Scripts/World0/localizations.js', masksScript: 'Scripts/World0/masks.js'}];
    const viewer = createAssetsViewer(() => assert.fail('New exports must not fetch JSON'), {
        protocol: 'http:', contents,
        loadScript(url, context) {
            requested.push(url);
            context.registerWorldLocalizations('World0', {translations: {stone: 'Stone'}, hasFont: false});
            return true;
        }
    });
    await viewer.context.containersMainAsync();
    assert.deepEqual(requested, [contents[0].localizationsScript]);
    assert.equal(viewer.context.localize('stone'), 'Stone');
    assert.equal(viewer.context.worldMaskUrl('Items/stone.png'), 'World0/Images/Items/stone.png');
    assert.match(viewer.context.renderMinecraftText('ASCII'), /mc_text_fallback.*ASCII/);
});

test('partial map translations localize full phrases and keep custom names', () => {
    const viewer = createAssetsViewer(undefined, {
        contents: [{worldId: 'World0', localizationsScript: 'Scripts/World0/localizations.js'}]
    });
    const {context} = viewer;
    context.document.documentElement = {lang: 'ja-JP'};
    context.registerWorldLocalizations('World0', {translations: {
        'tectonicus.marker.bed': '{player}のベッド',
        'narration.slot.name.offhand': '利き手と反対の手',
        'entity.minecraft.player': 'プレイヤー',
        'container.chest': 'チェスト',
        'gui.copy_link_to_clipboard': 'リンクをコピー'
    }, hasFont: false});
    assert.equal(context.mapText('tectonicus.marker.bed', {player: 'Alex'}), 'Alexのベッド');
    assert.equal(context.mapText('tectonicus.controls.reset'), 'Reset Map View');
    assert.equal(context.playerDisplayName({name: 'Player'}), 'Player');
    assert.equal(context.playerDisplayName({name: 'Player', defaultPlayerName: true}), 'プレイヤー');
    const inventory = context.getInventoryHtml({inventory: []});
    assert.match(inventory, /offhand_label[^>]*>利き手と反対の手/);
    assert.match(inventory, /data-slot="-106"[^>]*aria-label="利き手と反対の手"/);
    assert.match(context.createContainerPopup({name: 'Chest', nameKey: 'container.chest', items: []}), /チェスト/);
    assert.doesNotMatch(context.createContainerPopup({name: 'Chest', items: []}), /チェスト/);
    assert.match(context.formatMapDuration(63000), /1 分.*3 秒/);
});

test('Minecraft language codes unsupported by Intl keep their text and use English formatting', () => {
    const {context} = createAssetsViewer(undefined, {
        contents: [{worldId: 'World0', localizationsScript: 'Scripts/World0/localizations.js'}]
    });
    context.registerWorldLocalizations('World0', {translations: {'tectonicus.title': 'Localized map'}});
    const dateOptions = {dateStyle: 'medium', timeZone: 'UTC'};
    for (const lang of ['enws', 'esan']) {
        context.document.documentElement = {lang};
        assert.equal(context.formatMapNumber(1234), '1,234');
        assert.equal(context.formatMapDate(0, dateOptions), new Intl.DateTimeFormat('en-US', dateOptions).format(0));
        assert.equal(context.formatMapDuration(63000), '1 minute, 3 seconds');
        assert.equal(context.mapText('tectonicus.title'), 'Localized map');
    }
});

test('inventory glyphs use world font assets offline and preserve shaping fallback', () => {
    const {context} = createAssetsViewer(undefined, {protocol: 'file:'});
    const font = {0: [64, 16], 32: [0, 0, 0, 0, 4, 0], 97: [0, 0, 5, 8, 6, 0],
        227: [16, 0, 5, 8, 6, 0], 26085: [32, 0, 8, 8, 9, 0], 1575: [48, 0, 5, 8, 6, 0]};
    context.registerWorldLocalizations('World0', {translations: {}, inventoryFont: font});
    context.registerWorldMasks('World0', {'InventoryFont.png': 'data:image/png;base64,AAAA'});
    const html = context.renderMinecraftText('a a\u0303日', 'name');
    assert.match(html, /mask-position:-16px -0px/);
    assert.match(html, /mask-position:-32px -0px/);
    assert.match(html, /mc_space/);
    assert.match(html, /data:image\/png;base64,AAAA/);
    assert.doesNotMatch(html, /mc_text_fallback/);
    assert.match(context.renderMinecraftText('ا'), /mc_text_fallback/);
    assert.match(context.renderMinecraftText('ã<&>'), /ã&lt;&amp;&gt;/);
    assert.match(context.getInventoryHtml({inventory: []}), /equipment_tooltip/);
    assert.doesNotMatch(context.getInventoryHtml({inventory: []}), /title="Helmet"/);
    context.window.location.protocol = 'http:';
    context.document.baseURI = 'http://localhost/maps/map.html';
    assert.match(context.renderMinecraftText('ã'), /http:\/\/localhost\/maps\/World0\/Images\/InventoryFont\.png/);
});

test('chest images, item masks, overlays, player status and markers use the selected world assets', () => {
    const viewer = createAssetsViewer();
    const item = {id: 'minecraft:leather_helmet', count: 2, slot: 0, color: 0x123456,
        enchantments: [{id: 'minecraft:protection', level: 1}]};
    for (const index of [0, 1, 0]) {
        viewer.selectWorld(index);
        const prefix = `World${index}/Images/`;
        const html = viewer.context.createContainerPopup({name: 'Chest', items: [item]});
        assert.ok(html.includes(prefix + 'SmallChest.png'));
        assert.ok(viewer.context.createContainerPopup({name: 'Large Chest', large: true, items: []}).includes(prefix + 'LargeChest.png'));
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
        viewer.context.refreshSignMarkers(viewer.context.selectedLayer);
        assert.equal(viewer.markers.at(-1).options.icon.iconUrl, prefix + 'Sign.png');
        assert.ok(!html.includes(`World${1 - index}/Images/`));
    }
});

test('all container kinds use world-specific icons and retain copy actions and inventory popups', () => {
    const viewer = createAssetsViewer();
    const containers = [
        {containerType: 'chest', icon: 'Chest'},
        {containerType: 'trapped_chest', icon: 'TrappedChest'},
        {containerType: 'ender_chest', icon: 'EnderChest'},
        {containerType: 'barrel', icon: 'Barrel'},
        {containerType: 'shulker_box', icon: 'ShulkerBox'},
        ...['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray',
            'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']
            .map(color => ({containerType: 'shulker_box', shulkerColor: color, icon: 'ShulkerBox_' + color}))
    ];
    for (const index of [0, 1, 0]) {
        viewer.selectWorld(index);
        const layer = viewer.context.selectedLayer;
        layer.containers = containers.map(container => ({...container, name: 'Storage', items: [], worldPos: {x: 0.5, y: 64, z: 0.5}}));
        viewer.context.refreshContainerMarkers(layer);
        viewer.markers.slice(-containers.length).forEach((marker, i) => {
            assert.equal(marker.options.icon.iconUrl, `World${index}/Images/${containers[i].icon}.png`);
            assert.equal((marker.html.match(/<button /g) || []).length, 3);
            assert.equal((marker.html.match(/class="inventory_slot"/g) || []).length,
                containers[i].containerType === 'ender_chest' ? 0 : 27);
        });
    }
    assert.equal(viewer.context.containerMarkerIcon({}), 'Chest.png');
    assert.equal(viewer.context.containerMarkerIcon({enderChest: true}), 'EnderChest.png');
});

test('ender chest markers use world icons, show block coordinates and follow the container toggle', () => {
    const viewer = createAssetsViewer();
    const group = viewer.evaluate('markerGroups.containerControlChecked');
    for (const index of [0, 1, 0]) {
        viewer.selectWorld(index);
        const layer = viewer.context.selectedLayer;
        layer.containers = [
            {enderChest: true, worldPos: {x: -16.5, y: -36, z: -0.5}},
            {name: 'Chest', items: [], worldPos: {x: 8.5, y: 64, z: 8.5}}
        ];
        viewer.context.setMarkerVisibility('containerControlChecked', false);
        viewer.context.refreshContainerMarkers(layer);
        const [enderChest, chest] = viewer.markers.slice(-2);
        assert.equal(enderChest.options.icon.iconUrl, `World${index}/Images/EnderChest.png`);
        assert.equal(chest.options.icon.iconUrl, `World${index}/Images/Chest.png`);
        assert.equal(enderChest.point, layer.containers[0].worldPos);
        assert.match(enderChest.html, /Ender Chest<\/div><br\/> -17, -36, -1/);
        assert.doesNotMatch(enderChest.html, /inventory_slot|SmallChest.png|undefined/);
        assert.match(chest.html, /SmallChest.png/);
        assert.deepEqual(group.getLayers(), [enderChest, chest]);
        assert.equal(viewer.map.layers.size, 0);
        viewer.context.setMarkerVisibility('containerControlChecked', true);
        assert.deepEqual(viewer.map.layers, new Set([group, enderChest, chest]));
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
            viewer.context.createContainerPopup({name: 'Chest', items: [item]}),
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
        viewer.context.createContainerPopup({name: 'Chest', items: [item]})]) {
        assert.ok(html.includes('src="Images/Items/stone.png"'));
        assert.match(html, /item_count/);
        assert.match(html, /item_description/);
        assert.doesNotMatch(html, /null|undefined/);
    }
});

test('only the latest player icon requests populate the group after switching away and back', () => {
    const viewer = createAssetsViewer();
    const {context, map, images, markers} = viewer;
    const group = viewer.evaluate('markerGroups.playerControlChecked');
    const firstLayer = context.selectedLayer;
    firstLayer.players = [{name: 'Explorer', worldPos: {x: 0, y: 64, z: 0}},
        {name: 'Builder', worldPos: {x: 10, y: 64, z: 10}}];
    context.setMarkerVisibility('playerControlChecked', true);
    context.refreshPlayerMarkers(firstLayer);
    const [oldExplorer, oldBuilder] = images;

    viewer.selectWorld(1);
    context.selectedLayer.players = [{name: 'Other', worldPos: {x: 20, y: 64, z: 20}}];
    context.refreshPlayerMarkers(context.selectedLayer);
    const other = images[2];
    other.onerror();
    assert.equal(markers[0].options.icon.iconUrl, 'World1/Images/PlayerIcons/Tectonicus_Default_Player_Icon.png');
    assert.equal(map.layers.has(markers[0]), true);

    context.selectedLayer = firstLayer;
    viewer.evaluate('activeBaseLayer = selectedLayer;');
    context.refreshPlayerMarkers(firstLayer);
    assert.equal(map.layers.has(markers[0]), false);
    context.setMarkerVisibility('playerControlChecked', false);
    oldExplorer.onload();
    oldBuilder.onerror();
    assert.equal(markers.length, 1);

    const [explorer, builder] = images.slice(3);
    explorer.onload();
    builder.onerror();
    const currentMarkers = markers.slice(1);
    assert.deepEqual(group.getLayers(), currentMarkers);
    assert.deepEqual(currentMarkers.map(marker => marker.options.icon.iconUrl), [
        'World0/Images/PlayerIcons/Explorer.png',
        'World0/Images/PlayerIcons/Tectonicus_Default_Player_Icon.png'
    ]);
    assert.equal(map.layers.size, 0);
    context.setMarkerVisibility('playerControlChecked', true);
    assert.deepEqual(map.layers, new Set([group, ...currentMarkers]));
});

test('players without a resolved profile appear immediately with a localized name and default icon', () => {
    const viewer = createAssetsViewer(undefined, {
        contents: [{worldId: 'World0', localizationsScript: 'Scripts/World0/localizations.js'}]
    });
    const {context, images, markers} = viewer;
    context.registerWorldLocalizations('World0', {translations: {'entity.minecraft.player': 'Jogador'}});
    const layer = context.selectedLayer;
    layer.players = [{name: 'null', worldPos: {x: 0, y: 64, z: 0}},
        {name: 'Player', defaultPlayerName: true, worldPos: {x: 1, y: 64, z: 0}}];
    context.refreshPlayerMarkers(layer);
    assert.equal(images.length, 0);
    assert.equal(markers.length, 2);
    for (const marker of markers) {
        assert.match(marker.html, />Jogador</);
        assert.match(marker.options.icon.iconUrl, /Tectonicus_Default_Player_Icon\.png$/);
    }
});
