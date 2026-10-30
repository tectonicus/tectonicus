const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

function viewer() {
    const context = vm.createContext({
        contents: [], window: new EventTarget(),
        L: {map: () => ({}), CRS: {Simple: {}}}
    });
    for (const file of ['containers.js', 'marker.js', 'main.js']) {
        const source = fs.readFileSync(path.join(__dirname, '../../main/resources', file), 'utf8')
            .replace(/\{\{\w+\}\}/g, 'false');
        vm.runInContext(source, context);
    }
    return context;
}

function slotHtml(html, slot) {
    return html.split('data-slot="' + slot + '"')[1].split('<div class="inventory_slot"')[0];
}

test('inventory lays out storage, hotbar, armor and signed offhand slots', () => {
    const context = viewer();
    const inventory = [
        {id: 'minecraft:diamond', slot: 0, count: 64},
        {id: 'minecraft:stone', slot: 9, count: 32},
        {id: 'minecraft:diamond_helmet', slot: 103, count: 1},
        {id: 'minecraft:shield', slot: -106, count: 1},
        {id: 'minecraft:dirt', slot: 99, count: 1}
    ];
    const html = context.getInventoryHtml({inventory});
    assert.equal((html.match(/class="inventory_slot"/g) || []).length, 41);
    assert.match(html, /Images\/PlayerInventory.png/);
    assert.match(slotHtml(html, 0), /top: 107px; left: 7px;.*Images\/Items\/diamond.png/s);
    assert.match(slotHtml(html, 9), /top: 49px; left: 7px;.*Images\/Items\/stone.png/s);
    assert.match(slotHtml(html, 103), /top: 17px; left: 7px;.*diamond_helmet.png/s);
    assert.match(slotHtml(html, -106), /top: 17px; left: 151px;.*shield.png/s);
    assert.ok(html.indexOf('data-slot="35"') < html.indexOf('data-slot="0"'));
    assert.doesNotMatch(html, /Items\/dirt.png/);
    assert.match(slotHtml(html, 0), /class="mc_text_container item_count"/);
    assert.match(slotHtml(html, 0), /class="item_description"/);
});

test('old exports omit inventory, empty inventories show all empty slots', () => {
    const context = viewer();
    assert.equal(context.getInventoryHtml({}), '');
    const html = context.getInventoryHtml({inventory: []});
    assert.equal((html.match(/class="inventory_slot"/g) || []).length, 41);
    assert.doesNotMatch(html, /class="item"/);
});

test('player popup and chest share item details, colors, trims and enchantments', () => {
    const context = viewer();
    const item = {id: 'minecraft:leather_chestplate', count: 1, slot: 102,
        customName: 'Favorite armor', color: 0x123456,
        trim: {material: 'minecraft:gold', pattern: 'minecraft:spire'},
        enchantments: [{id: 'minecraft:protection', level: 4}]};
    const html = context.getPlayerHtml({name: 'Alex', icon: 'Alex.png', xpLevel: 12,
        health: 20, food: 20, air: 300, donation: '', inventory: [item]});
    assert.match(html, /Experience level 12/);
    assert.match(html, /leather_chestplate_gold_trim.png/);
    assert.match(html, /background-color: #123456/);
    assert.match(html, /enchanted_glint/);
    assert.match(html, /item_description/);
    assert.doesNotMatch(html, /null|undefined/);
    const chest = context.createContainerPopup({name: 'Chest', items: [{...item, slot: 0}]});
    assert.match(chest, /leather_chestplate_gold_trim.png/);
    assert.match(slotHtml(chest, 0), /top: 17px; left: 7px;.*class="item" style="top: 1px; left: 1px;/s);
    assert.doesNotMatch(chest, /null|undefined/);
});

test('player marker allows enough room for the inventory', () => {
    const context = viewer();
    let options;
    context.L = {icon: value => value, marker: () => ({
        bindPopup(html, popupOptions) { options = popupOptions; return this; },
        bindTooltip() { return this; }
    })};
    context.createPlayerMarker({name: 'Alex', inventory: [], donation: ''}, [0, 0]);
    assert.ok(options.maxWidth >= 352);
});

test('Ender Chest has 27 slots with counts and item details at the saved positions', () => {
    const context = viewer();
    vm.runInContext("localizationTables.set('Scripts/localizations.json', {'container.enderchest': 'Ender Storage'});", context);
    const html = context.getEnderChestHtml({enderChestInventory: [
        {id: 'minecraft:ender_pearl', slot: 0, count: 16},
        {id: 'minecraft:diamond_sword', slot: 26, count: 1,
            customName: 'Treasure', enchantments: [{id: 'minecraft:sharpness', level: 5}]},
        {id: 'minecraft:dirt', slot: 27, count: 64}
    ]});
    assert.match(html, /class="ender_chest_inventory"/);
    assert.ok(html.includes(context.renderMinecraftText('Ender Storage', 'inventory_label')));
    assert.equal((html.match(/class="inventory_slot"/g) || []).length, 27);
    assert.match(slotHtml(html, 0), /top: 17px; left: 7px;.*ender_pearl.png/s);
    assert.match(slotHtml(html, 0), /class="mc_text_container item_count"/);
    assert.match(slotHtml(html, 26), /top: 53px; left: 151px;.*diamond_sword.png/s);
    assert.match(slotHtml(html, 26), /enchanted_glint/);
    assert.match(slotHtml(html, 26), /item_description/);
    assert.doesNotMatch(html, /Items\/dirt.png|null|undefined/);
});

test('missing Ender Chest data is omitted and empty Ender Chests show empty slots', () => {
    const context = viewer();
    assert.equal(context.getEnderChestHtml({}), '');
    const html = context.getEnderChestHtml({enderChestInventory: []});
    assert.equal((html.match(/class="inventory_slot"/g) || []).length, 27);
    assert.doesNotMatch(html, /class="item"/);
});

test('popup combines Ender Chest, equipment, storage and hotbar on one textured panel', () => {
    const context = viewer();
    const html = context.getPlayerHtml({name: 'Alex', icon: 'Alex.png', health: 20, food: 20,
        air: 300, xpLevel: 12, donation: '',
        inventory: [{id: 'minecraft:stone', slot: 0, count: 64},
            {id: 'minecraft:diamond_helmet', slot: 103, count: 1},
            {id: 'minecraft:shield', slot: -106, count: 1}],
        enderChestInventory: [{id: 'minecraft:diamond', slot: 0, count: 32}]});
    const [enderChestAndEquipment, inventory] = html.split('<div class="player_storage">');
    assert.equal((html.match(/class="inventory_slot"/g) || []).length, 68);
    assert.equal((html.match(/class="player_inventory_background"/g) || []).length, 1);
    assert.match(html, /Images\/PlayerInventoryWithEnderChest.png/);
    assert.match(slotHtml(enderChestAndEquipment, 103), /top: 85px; left: 7px;.*diamond_helmet.png/s);
    assert.match(slotHtml(enderChestAndEquipment, -106), /top: 85px; left: 151px;.*shield.png/s);
    assert.match(slotHtml(inventory, 0), /top: 175px; left: 7px;.*stone.png/s);
    assert.match(slotHtml(inventory, 9), /top: 117px; left: 7px;/);
    assert.match(inventory, /Items\/stone.png/);
    assert.doesNotMatch(inventory, /Items\/diamond.png/);
    assert.match(enderChestAndEquipment, /Items\/diamond.png/);
    assert.doesNotMatch(enderChestAndEquipment, /Items\/stone.png/);
});
