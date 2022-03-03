var maxZoom = {{maxZoom}};

var MAP_COORD_SCALE_FACTOR = {{mapCoordScaleFactor}};

var showSpawn = {{showSpawn}};			
var signsInitiallyVisible = {{signsInitiallyVisible}};
var playersInitiallyVisible = {{playersInitiallyVisible}};
var portalsInitiallyVisible = {{portalsInitiallyVisible}};
var bedsInitiallyVisible = {{bedsInitiallyVisible}};
var spawnInitiallyVisible = {{spawnInitiallyVisible}};
var viewsInitiallyVisible = true; // todo!



function createPlayerMarker(player, pos) {
	let icon = L.icon({
		iconUrl: player.icon,
		// iconSize: [30, 30],
		iconAnchor: [17, 20],
		popupAnchor: [0, -10],
		//shadowUrl: 'my-icon-shadow.png',
		//shadowSize: [68, 95],
		//shadowAnchor: [22, 94]
	});

	return L.marker(pos, { icon: icon }).bindPopup(getPlayerHtml(player)).bindTooltip(player.name);
}

function getPlayerHtml(player) {
	//The supporters list can't be retrieved from JustGiving anymore, these are the only known ones
	//They may not even care about this anymore, but we'll leave it in because they did donate years ago
	if (player.name === 'griffen8280' || player.name === 'Vyruz' || player.name === 'iamsofa') {
		let html = ''
		html += '<center><table><tr>'

		html += '<td>'
		html += '<img src=\"Images/IronIcon.png\"/>'
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
			+ '<div style=\"width:300px; margin:4px;\" >'
			+ 	'<img style=\"float:left; margin:4px;\" src=\"' + player.icon + '\" width=\"64\" height=\"64\" />'
			+	'<div>'
			+		'<div>' + getHealthHtml(player) + '</div>'
			+		'<div>' + getFoodHtml(player) + '</div>'
			+		'<div>' + getAirHtml(player) + '</div>'
			+		'<div>' + getInventoryHtml(player) + '</div>'
			+		'<div>' + getItemsHtml(player) + '</div>'
			+	'</div>'
			+ '</div>'

			+ '<div id=\"xpDiv\" style=\"clear:both; text-align:center\" >'
			+   'Experience level '+player.xpLevel
			+ '</div>'

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
			image = 'Images/FullHeart.png';
		else if (i * 2 < player.health)
			image = 'Images/HalfHeart.png';
		else
			image = 'Images/EmptyHeart.png';

		html += '<img style=\"margin:1px\" src=\"' + image + '" width=\"18\" height=\"18\" />';
	}

	return html;
}

function getFoodHtml(player) {
	var html = '';

	var NUM_ICONS = 10;
	for (var i = 0; i < NUM_ICONS; i++) {
		var image;
		if (i * 2 + 1 < player.food)
			image = 'Images/FullFood.png';
		else if (i * 2 < player.food)
			image = 'Images/HalfFood.png';
		else
			image = 'Images/EmptyFood.png';

		html += '<img style=\"margin:1px\" src=\"' + image + '" width=\"18\" height=\"18\" />';
	}

	return html;
}

function getAirHtml(player) {
	var html = '';

	var NUM_ICONS = 10;
	for (var i = 0; i < NUM_ICONS; i++) {
		var image;
		if (i * 30 < player.air)
			image = 'Images/FullAir.png';
		else
			image = 'Images/EmptyAir.png';

		html += '<img style=\"margin:1px\" src=\"' + image + '" width=\"18\" height=\"18\" />';
	}

	return html;
}

function getInventoryHtml(player) {
	var html = '';
	/*
		html += '<table>'

		html += '<tr>'

		for (var x=0; x<9; x++)
		{
			html += '<td>'
			html += 'item'
			html += '</td>'
		}

		html += '</tr>'

		html += '</table>'
	*/
	return html;
}

function getItemsHtml(player) {
	var html = '';

	// ..

	return html;
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

function getUrlWithoutParams()
{
	parts = window.location.href.split('?');
	return parts[0];
}

function getQueryParams()
{
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

function getFragmentParams()
{	
	var hashParams = {};
	var e,
	a = /\+/g,  // Regex for replacing addition symbol with a space
	r = /([^&;=]+)=?([^&;]*)/g,
	d = function (s) { return decodeURIComponent(s.replace(a, " ")); },
	q = window.location.hash.substring(1);
	
	while (e = r.exec(q))
		hashParams[d(e[1])] = d(e[2]);
	
	return hashParams;
}
