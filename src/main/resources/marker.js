var maxZoom = {{maxZoom}};

var MAP_COORD_SCALE_FACTOR = {{mapCoordScaleFactor}};

var showSpawn = {{showSpawn}};			
var signsInitiallyVisible = {{signsInitiallyVisible}};
var playersInitiallyVisible = {{playersInitiallyVisible}};
var portalsInitiallyVisible = {{portalsInitiallyVisible}};
var bedsInitiallyVisible = {{bedsInitiallyVisible}};
var respawnAnchorsInitiallyVisible = {{respawnAnchorsInitiallyVisible}};
var spawnInitiallyVisible = {{spawnInitiallyVisible}};
var viewsInitiallyVisible = {{viewsInitiallyVisible}};
var chestsInitiallyVisible = {{chestsInitiallyVisible}};
var beaconsInitiallyVisible = {{beaconsInitiallyVisible}};

let controlState = {
	spawnControlChecked: spawnInitiallyVisible,
	signControlChecked: signsInitiallyVisible,
	viewControlChecked: viewsInitiallyVisible,
	playerControlChecked: playersInitiallyVisible,
	portalControlChecked: portalsInitiallyVisible,
	bedControlChecked: bedsInitiallyVisible,
	respawnAnchorControlChecked: respawnAnchorsInitiallyVisible,
	chestControlChecked: chestsInitiallyVisible,
	beaconControlChecked: beaconsInitiallyVisible,

}

function createPlayerMarker(player, pos) {
	let icon = L.icon({
		iconUrl: player.icon,
		iconSize: [40, 40],
		iconAnchor: [21, 25],
		popupAnchor: [0, -10],
		//shadowUrl: 'my-icon-shadow.png',
		//shadowSize: [68, 95],
		//shadowAnchor: [22, 94]
	});

	return L.marker(pos, { icon: icon }).bindPopup(getPlayerHtml(player), {maxWidth: 380}).bindTooltip(player.name);
}

function getPlayerHtml(player) {
	//The supporters list can't be retrieved from JustGiving anymore, these are the only known ones
	//They may not even care about this anymore, but we'll leave it in because they did donate years ago
	if (player.name === 'griffen8280' || player.name === 'Vyruz' || player.name === 'iamsofa') {
		let html = ''
		html += '<center><table><tr>'

		html += '<td>'
		html += '<img src="' + worldImageUrl('IronIcon.png') + '"/>'
		html += '</td>'

		html += '<td>'
		html += '<a href=\"http://www.justgiving.com/tectonicus\">Iron Supporter</a>'
		html += '</td>'

		html += '</tr></table></center>'

		player.donation = html;
	}

	let html = ''
		+ '<div style=\"text-align:center\">'
			+ '<div class=\"playerName\" style=\"text-align:center; font-size:110%\" >' + player.name + '</div>'
			+ '<div style=\"width:300px; margin:4px auto;\" >'
			+ 	'<img style=\"float:left; margin:4px;\" src=\"' + player.icon + '\" width=\"64\" height=\"64\" />'
			+	'<div>'
			+		'<div>' + getHealthHtml(player) + '</div>'
			+		'<div>' + getFoodHtml(player) + '</div>'
			+		'<div>' + getAirHtml(player) + '</div>'
			+	'</div>'
			+ '</div>'

			+ '<div id=\"xpDiv\" style=\"clear:both; text-align:center\" >'
			+   'Experience level '+player.xpLevel
			+ '</div>'

			+ getInventoryHtml(player)

			+ '<div id=\"donationDiv\" style=\"clear:both; text-align:center\" >'
			+	getDonationHtml(player)
			+ '</div>'
		+ '</div>';
		
	return html;
}

function getHealthHtml(player) {
	var html = '';

	var NUM_ICONS = 10;
	for (var i = 0; i < NUM_ICONS; i++) {
		var image;
		if (i * 2 + 1 < player.health)
			image = 'FullHeart.png';
		else if (i * 2 < player.health)
			image = 'HalfHeart.png';
		else
			image = 'EmptyHeart.png';

		html += '<img style=\"margin:1px\" src=\"' + worldImageUrl(image) + '" width=\"18\" height=\"18\" />';
	}

	return html;
}

function getFoodHtml(player) {
	var html = '';

	var NUM_ICONS = 10;
	for (var i = 0; i < NUM_ICONS; i++) {
		var image;
		if (i * 2 + 1 < player.food)
			image = 'FullFood.png';
		else if (i * 2 < player.food)
			image = 'HalfFood.png';
		else
			image = 'EmptyFood.png';

		html += '<img style=\"margin:1px\" src=\"' + worldImageUrl(image) + '" width=\"18\" height=\"18\" />';
	}

	return html;
}

function getAirHtml(player) {
	var html = '';

	var NUM_ICONS = 10;
	for (var i = 0; i < NUM_ICONS; i++) {
		var image;
		if (i * 30 < player.air)
			image = 'FullAir.png';
		else
			image = 'EmptyAir.png';

		html += '<img style=\"margin:1px\" src=\"' + worldImageUrl(image) + '" width=\"18\" height=\"18\" />';
	}

	return html;
}

function getInventoryHtml(player) {
	// Older map exports do not include inventory data.
	const hasEnderChest = Array.isArray(player.enderChestInventory);
	if (!Array.isArray(player.inventory) && !hasEnderChest)
		return '';

	const items = new Map((player.inventory || []).map(item => [item.slot, item]));
	const equipmentTop = hasEnderChest ? 85 : 17;
	const inventoryTop = hasEnderChest ? 117 : 49;
	const background = hasEnderChest ? 'PlayerInventoryWithEnderChest.png' : 'PlayerInventory.png';
	let html = '<div class="player_inventory' + (hasEnderChest ? ' with_ender_chest' : '') + '"><div class="player_inventory_scaler">';
	html += '<img class="player_inventory_background" src="' + worldImageUrl(background) + '" alt="" />';
	html += getEnderChestHtml(player);
	html += renderMinecraftText(localize('attribute.name.armor', localize('attribute.name.generic.armor', 'Armor')), 'inventory_label armor_label');
	html += renderMinecraftText('Offhand', 'inventory_label armor_label offhand_label');
	[103, 102, 101, 100].forEach((slot, index) => {
		html += createInventorySlotHtml(items, slot, equipmentTop, 7 + 18 * index, ['Helmet', 'Chestplate', 'Leggings', 'Boots'][index]);
	});
	html += createInventorySlotHtml(items, -106, equipmentTop, 151, 'Offhand');
	html += '<div class="player_storage">';
	html += renderMinecraftText(localize('container.inventory', 'Inventory'), 'inventory_label');
	for (let slot = 9; slot < 36; slot++)
		html += createInventorySlotHtml(items, slot, inventoryTop + 18 * Math.floor((slot - 9) / 9), 7 + 18 * (slot % 9));
	for (let slot = 0; slot < 9; slot++)
		html += createInventorySlotHtml(items, slot, inventoryTop + 58, 7 + 18 * slot);
	return html + '</div></div></div>';
}

function getEnderChestHtml(player) {
	if (!Array.isArray(player.enderChestInventory))
		return '';

	const items = new Map(player.enderChestInventory.map(item => [item.slot, item]));
	let html = '<div class="ender_chest_inventory">';
	html += renderMinecraftText(localize('container.enderchest', 'Ender Chest'), 'inventory_label');
	for (let slot = 0; slot < 27; slot++)
		html += createInventorySlotHtml(items, slot, 17 + 18 * Math.floor(slot / 9), 7 + 18 * (slot % 9));
	return html + '</div>';
}

function createInventorySlotHtml(items, slot, top, left, label) {
	let html = '<div class="inventory_slot" data-slot="' + slot + '" style="top: ' + top + 'px; left: ' + left + 'px;"';
	html += label ? ' title="' + label + '">' : '>';
	const item = items.get(slot);
	if (item && item.count > 0 && item.id !== 'minecraft:air')
		html += createItemHtml(item, 1, 1);
	return html + '</div>';
}

function getDonationHtml(player) {
	var html = '';

	html += '<div>';

	if (player.donation != '') {
		html += player.donation;
	}
	else {
		html += '<img src=\"Images/Spacer.png\" style=\"height:38px;\" />';
	}

	html += '</div>';

	return html;
}

function getQueryParams() {
	var urlParams = {};
	var e,
	a = /\+/g,  // Regex for replacing addition symbol with a space
	r = /([^&;=]+)=?([^&;]*)/g,
	d = function (s) { return decodeURIComponent(s.replace(a, " ")); },
	q = window.location.search.substring(1);
	
	while (e = r.exec(q))
		urlParams[d(e[1])] = d(e[2]);
	
	return urlParams;
}

function getFragmentParams() {
	return Object.fromEntries(new URLSearchParams(window.location.hash.slice(1)));
}
