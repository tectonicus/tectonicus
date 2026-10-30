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

function createPlayerMarker(player, pos, layer = activeBaseLayer) {
	let icon = L.icon({
		iconUrl: player.icon,
		iconSize: [40, 40],
		iconAnchor: [21, 25],
		popupAnchor: [0, -10],
		//shadowUrl: 'my-icon-shadow.png',
		//shadowSize: [68, 95],
		//shadowAnchor: [22, 94]
	});

	return bindMarkerPopup(L.marker(pos, { icon: icon }), getPlayerHtml(player), player.worldPos, layer, {maxWidth: 380}).bindTooltip(player.name);
}

function bindMarkerPopup(marker, html, worldPos, layer, options) {
	return marker.bindPopup(html + markerCopyControls(worldPos, layer), options);
}

function markerTeleportCommand(worldPos, layer) {
	const x = Math.floor(worldPos.x) + 0.5;
	const y = Math.floor(worldPos.y) + 1;
	const z = Math.floor(worldPos.z) + 0.5;
	const destination = `${x} ${y} ${z}`;
	const dimension = layer.dimensionId;
	return layer.worldStats?.minecraftVersion >= 13 && /^[a-z0-9_.-]+:[a-z0-9_./-]+$/.test(dimension || '')
		? `/execute in ${dimension} run tp @s ${destination}`
		: `/tp ${destination}`;
}

function markerShareLink(worldPos, layer) {
	const url = new URL(window.location.href);
	// Links use the map's reference height. Project the marker onto that plane
	// so elevated markers land at the same screen position after opening a link.
	const point = layer.projection.worldToMap(worldPos);
	const position = layer.projection.mapToWorld({lat: point[0], lng: point[1]});
	url.hash = new URLSearchParams({
		layer: layer.urlId,
		x: Math.round(position.x), z: Math.round(position.z),
		zoom: Math.min(mymap.getZoom(), layer.maxZoom ?? maxZoom)
	}).toString();
	return url.href;
}

function markerCopyControls(worldPos, layer) {
	if (!layer || !worldPos || !['x', 'y', 'z'].every(axis => Number.isFinite(worldPos[axis]))) return '';
	const coords = ['x', 'y', 'z'].map(axis => Math.floor(worldPos[axis])).join(' ');
	const command = markerTeleportCommand(worldPos, layer);
	const icon = paths => '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">' + paths + '</svg>';
	const coordsIcon = icon('<circle cx="12" cy="12" r="7"/><path d="M12 2v4m0 12v4M2 12h4m12 0h4"/><circle cx="12" cy="12" r="1"/>');
	const teleportIcon = icon('<path d="m5 7 5 5-5 5m8 0h6"/>');
	const linkIcon = icon('<path d="m10 13 4-4m-6 1-2 2a4 4 0 0 0 6 6l2-2m2-2 2-2a4 4 0 0 0-6-6l-2 2"/>');
	return '<div class="marker_copy_controls">'
		+ `<button type="button" data-copy-text="${escapeHtml(coords)}" title="Copy coords" aria-label="Copy coords" onclick="copyMarkerValue(this)">${coordsIcon}</button>`
		+ `<button type="button" data-copy-text="${escapeHtml(command)}" title="Copy teleport command (above this position)" aria-label="Copy teleport command" onclick="copyMarkerValue(this)">${teleportIcon}</button>`
		+ `<button type="button" data-copy-layer="${escapeHtml(layer.layerId)}" data-copy-position="${escapeHtml(JSON.stringify(worldPos))}" title="Copy map link" aria-label="Copy map link" onclick="copyMarkerValue(this)">${linkIcon}</button>`
		+ '<span class="marker_copy_status" role="status" aria-live="polite"></span></div>';
}

async function copyMarkerValue(button) {
	const status = button.parentElement.querySelector('.marker_copy_status');
	try {
		const text = button.dataset.copyText ?? markerShareLink(
			JSON.parse(button.dataset.copyPosition), tileLayers.get(button.dataset.copyLayer));
		let copied = false;
		try {
			if (window.navigator?.clipboard?.writeText) {
				await window.navigator.clipboard.writeText(text);
				copied = true;
			}
		} catch (error) {
			// Local files and HTTP maps may deny Clipboard API access.
		}
		if (!copied) {
			const field = document.createElement('textarea');
			field.value = text;
			field.style.cssText = 'position:fixed;left:-9999px;top:0';
			document.body.appendChild(field);
			try {
				field.focus();
				field.select();
				copied = !!document.execCommand?.('copy');
			} catch (error) {
				copied = false;
			} finally {
				field.remove();
				button.focus();
			}
		}
		if (!copied) {
			window.prompt('Copy this text:', text);
			status.textContent = 'Text ready to copy';
		} else {
			status.textContent = 'Copied!';
		}
	} catch (error) {
		status.textContent = 'Could not copy';
	}
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
