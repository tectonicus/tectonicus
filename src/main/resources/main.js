
let mymap = L.map('map', {crs: L.CRS.Simple, minZoom: 0, maxZoom: maxZoom, attributionControl: false}).setView([0, 0], 0);
let tileLayers = new Map();
let activeBaseLayer = null;

function size(obj) {
	var size = 0, key;
	for (key in obj) {
		if (obj.hasOwnProperty(key)) size++;
	}
	return size;
}

var localizations = null;
var layerControl = null;
var compassControl = null;
var viewToggleControl = null;
var signToggleControl = null;
var playerToggleControl = null;
var bedToggleControl = null;
var respawnAnchorToggleControl = null;
var portalToggleControl = null;
var spawnToggleControl = null;
var chestToggleControl = null;

async function main()
{
        // Set handler to update position of item description tooltip to follow the mouse cursor
        window.onmousemove = function (e) {
            let itemDescriptionTooltip = document.querySelector(".item:hover .item_description");

            if (itemDescriptionTooltip) {
                let x = e.clientX,
                    y = e.clientY;
            
                let rect = itemDescriptionTooltip.parentElement.getBoundingClientRect();

                const scale = 3; // The scale should be set to the same scale as in CSS

                itemDescriptionTooltip.style.top = (y - rect.top - 10) / scale + 'px';
                itemDescriptionTooltip.style.left = (x - rect.left + 20) / scale + 'px';
            }
        };
    
        // Get localizations asynchronously, so that initialization can continue while the file is being downloaded
        const localizationsResponsePromise = fetch('Scripts/localizations.json');
        
	let queryParams = getQueryParams();
	let fragmentParams = getFragmentParams();
	
	L.TileLayer.Tectonicus = L.TileLayer.extend({
        getTileUrl: function(coords) {
            let xBin = coords.x % 16;
            let yBin = coords.y % 16;

            return this.mapId + "/" + this.layerId + "/Zoom"+coords.z
                +"/"+xBin+"/"+yBin+"/tile_"+coords.x+"_"+coords.y+"."+this.imageFormat;
        },
        getAttribution: function() {
            return '<a href="https://github.com/tectonicus/tectonicus">Tectonicus</a> - <a tabindex="0" id="mapInfo">' + this.mapName + '</a>';
        },
        initialize: function(mapId, layerId, dimension, imageFormat, mapName, backgroundColor, signs, players, chests, views, portals, beds, anchors, worldVectors, projection,
            blockStats, worldStats, viewPosition, controlState) {
                this.mapId = mapId;
                this.layerId = layerId;
                this.dimension = dimension;
                this.imageFormat = imageFormat;
                this.mapName = mapName;
                this.backgroundColor = backgroundColor;
                this.signs = signs;
                this.players = players;
                this.chests = chests;
                this.views = views;
                this.portals = portals;
                this.beds = beds;
                this.anchors = anchors;
                this.worldVectors = worldVectors;
                this.projection = projection;
                this.blockStats = blockStats;
                this.worldStats = worldStats;
                this.viewPosition = viewPosition;
                this.controlState = JSON.parse(JSON.stringify(controlState));
            }
        });

        let baseMaps = {}
        for (let i = 0; i < contents.length; i++) {
            let tecMap = contents[i];
            for (let j = 0; j < tecMap.layers.length; j++) {
                let layer = tecMap.layers[j];
                let projection = new MinecraftProjection(tecMap.worldVectors);
                            // 'startPosition' stores view pos for a given layer, so we remember where we were when switching layers
                            let startPosition = new ViewPos(layer.id, tecMap.worldVectors.startView, 0, projection.worldToMap(tecMap.worldVectors.startView));

                let tileLayer = new L.TileLayer.Tectonicus(tecMap.id, layer.id, layer.dimension, layer.imageFormat, tecMap.name, layer.backgroundColor, tecMap.signs, tecMap.players,
                    tecMap.chests, tecMap.views, tecMap.portals, tecMap.beds, tecMap.respawnAnchors, tecMap.worldVectors, projection, tecMap.blockStats, tecMap.worldStats, startPosition, controlState);

                if (baseMaps.hasOwnProperty(tecMap.name + " - " + layer.name)) {
                    baseMaps[tecMap.name + " - " + layer.name + j] = tileLayer;  //A hack to handle duplicate layer names in the layer control
                } else {
                    baseMaps[tecMap.name + " - " + layer.name] = tileLayer;
                }
                tileLayers.set(layer.id, tileLayer);
            }
        }

        // Try and get a starting view from the fragment params, query params, or fall back to default
        let defaultLayer = tileLayers.get("LayerA");
        let startView;
        if (size(fragmentParams) > 0) {
                startView = findStartView(fragmentParams, defaultLayer.layerId, defaultLayer.viewPosition.worldPos);
        } else {
                startView = findStartView(queryParams, defaultLayer.layerId, defaultLayer.viewPosition.worldPos);
        }

        // Set the starting view
        let startLayer = tileLayers.get(startView.layerId);
        activeBaseLayer = startLayer;
        startLayer.addTo(mymap);
        mymap.setView(startView.startPoint, startView.zoom);

        // And store the updated start point in the layer
        startLayer.viewPosition = startView;

        // Create controls
        if (tileLayers.size > 1) {
            layerControl = L.control.layers(baseMaps);
        }
	compassControl = CreateCompassControl(startLayer.mapId + '/Compass.png');
        viewToggleControl = CreateToggleControl('views', 'Images/Picture.png', viewMarkers, viewsInitiallyVisible);
        signToggleControl = CreateToggleControl('signs', 'Images/Sign.png', signMarkers, signsInitiallyVisible);
        playerToggleControl = CreateToggleControl('players', 'Images/PlayerIcons/Tectonicus_Default_Player_Icon.png', playerMarkers, playersInitiallyVisible);
        bedToggleControl = CreateToggleControl('beds', 'Images/Bed.png', bedMarkers, bedsInitiallyVisible);
        respawnAnchorToggleControl = CreateToggleControl('respawn anchors', 'Images/RespawnAnchor.png', respawnAnchorMarkers, respawnAnchorsInitiallyVisible);
        portalToggleControl = CreateToggleControl('portals', 'Images/Portal.png', portalMarkers, portalsInitiallyVisible);
        spawnToggleControl = CreateToggleControl('spawn', 'Images/Spawn.png', spawnMarkers, spawnInitiallyVisible);
        chestToggleControl = CreateToggleControl('chests', 'Images/Chest.png', chestMarkers, false);

	//CreateLinkControl(map);

	// Add attribution control to the map
	L.control.attribution({position: 'bottomleft'}).setPrefix('<a href="http://www.leafletjs.com">Leaflet</a>').addTo(mymap);

        // We have done all we can. Now we have to await the response and parse the JSON
        let localizationsResponse;
        try {
                localizationsResponse = await localizationsResponsePromise;
        } catch (NetworkError) {
                console.error(
                        'There was a network error during localizations fetch. ' +
                        'If the map is open directly from the file system, the problem was probably that the request was denied due to CORS policy.' +
                        'There is no way of overriding the browser CORS policy on Tectonicus side. Please host the map on web server.');
        }
        try {
                localizations = await localizationsResponse.json();
        } catch {
                console.error('Unable to parse localizations');
        }

	// Register these last so that they don't get called while we're still initialising
	mymap.on('baselayerchange', onBaseLayerChange);
	mymap.on('zoomend', onProjectionChanged);
        mymap.on('moveend', onProjectionChanged);

	// Manually fire this event to set the initial state
        mymap.fireEvent('baselayerchange', {layer: startLayer});
}

spawnMarkers = [];
signMarkers = [];
viewMarkers = [];
playerMarkers = [];
portalMarkers = [];
bedMarkers = [];
respawnAnchorMarkers = [];
chestMarkers = [];

function onBaseLayerChange(e) {
    //Save current control states to previous layer
    if (spawnToggleControl.hasOwnProperty('_container')) // check if control exists on map
        controlState.spawnControlChecked = spawnToggleControl._container.checked;
    if (signToggleControl.hasOwnProperty('_container'))
        controlState.signControlChecked = signToggleControl._container.checked;
    if (viewToggleControl.hasOwnProperty('_container'))
        controlState.viewControlChecked = viewToggleControl._container.checked;
    if (playerToggleControl.hasOwnProperty('_container'))
        controlState.playerControlChecked = playerToggleControl._container.checked;
    if (portalToggleControl.hasOwnProperty('_container'))
        controlState.portalControlChecked = portalToggleControl._container.checked;
    if (bedToggleControl.hasOwnProperty('_container'))
        controlState.bedControlChecked = bedToggleControl._container.checked;
    if (respawnAnchorToggleControl.hasOwnProperty('_container'))
        controlState.respawnAnchorControlChecked = respawnAnchorToggleControl._container.checked;
    if (chestToggleControl.hasOwnProperty('_container'))
        controlState.chestControlChecked = chestToggleControl._container.checked;

    activeBaseLayer.controlState = JSON.parse(JSON.stringify(controlState));

    activeBaseLayer = e.layer;
    controlState = e.layer.controlState;

	mymap.setView(e.layer.viewPosition.startPoint, e.layer.viewPosition.zoom);

	changeBackgroundColor(e.layer.backgroundColor);

    spawnToggleControl.remove();
    if (e.layer.worldVectors.hasOwnProperty('spawnPosition') && e.layer.dimension !== "NETHER") {
        mymap.addControl(spawnToggleControl);
        spawnToggleControl.setChecked(controlState.spawnControlChecked);
    }

    signToggleControl.remove();
    if (e.layer.signs.length != 0) {
        mymap.addControl(signToggleControl);
        signToggleControl.setChecked(controlState.signControlChecked);
    }

    viewToggleControl.remove();
    if (e.layer.views.length != 0) {
        mymap.addControl(viewToggleControl);
        viewToggleControl.setChecked(controlState.viewControlChecked);
    }

    playerToggleControl.remove();
    if (e.layer.players.length != 0) {
        mymap.addControl(playerToggleControl);
        playerToggleControl.setChecked(controlState.playerControlChecked);
    }

    portalToggleControl.remove();
    if (e.layer.portals.length != 0) {
        mymap.addControl(portalToggleControl);
        portalToggleControl.setChecked(controlState.portalControlChecked);
    }

    bedToggleControl.remove();
    if (e.layer.beds.length != 0) {
        mymap.addControl(bedToggleControl);
        bedToggleControl.setChecked(controlState.bedControlChecked);
    }

    respawnAnchorToggleControl.remove();
    if (e.layer.anchors.length != 0) {
        mymap.addControl(respawnAnchorToggleControl);
        respawnAnchorToggleControl.setChecked(controlState.respawnAnchorChecked);
    }

    chestToggleControl.remove();
    if (e.layer.chests.length != 0) {
        mymap.addControl(chestToggleControl);
        chestToggleControl.setChecked(controlState.chestControlChecked);
    }

	if (layerControl != null) {
        mymap.addControl(layerControl);
    }

	// Add tooltips to the toggle buttons
	tippy('.button', {
        content(reference) {
          const title = reference.getAttribute('title');
          reference.removeAttribute('title');
          if (reference.checked) {
            return 'Hide ' + title;
          } else {
              return 'Show ' + title;
          }
        },
        placement: 'left',
        theme: 'light',
    });

	if (e.layer.dimension === "NETHER") {
        destroyMarkers(spawnMarkers);
    } else {
        refreshSpawnMarker(e.layer, controlState.spawnControlChecked);
    }
    refreshSignMarkers(e.layer, controlState.signControlChecked);
    refreshViewMarkers(e.layer, controlState.viewControlChecked);
    refreshPlayerMarkers(e.layer, controlState.playerControlChecked);
    refreshPortalMarkers(e.layer, controlState.portalControlChecked);
    refreshBedMarkers(e.layer, controlState.bedControlChecked);
    refreshRespawnAnchorMarkers(e.layer, controlState.respawnAnchorControlChecked);
    refreshChestMarkers(e.layer, controlState.chestControlChecked);

	compassControl.remove();
	compassControl = CreateCompassControl(e.layer.mapId + '/Compass.png');
	mymap.addControl(compassControl);

	// Add map info popup
    let peakMemoryMb = stats.peakMemoryBytes / 1024 / 1024;
    let statsHtml = '';
    statsHtml += '<h4 class="center">Map Info</h4>';
    statsHtml += '<div class="center">Render Stats</div>';
    statsHtml += 'Tectonicus version: ' + stats.tectonicusVersion + '<br/>';
    statsHtml += 'Render time: ' + stats.renderTime + '<br/>';
    statsHtml += 'Peak memory usage: ' + peakMemoryMb.toFixed(1) + 'Mb<br/>';
    statsHtml += 'Created on ' + stats.renderedOnDate + '<br/>';
    statsHtml += 'Created at ' + stats.renderedOnTime + '<br/>';
    statsHtml += '<br/>';

    statsHtml += '<div class="center">World stats</div>';
    statsHtml += 'Players: ' + e.layer.worldStats.numPlayers + '<br/>';
    statsHtml += 'Chunks: ' + e.layer.worldStats.numChunks + '<br/>';
    statsHtml += 'Portals: ' + e.layer.worldStats.numPortals + '<br/>';
    statsHtml += 'Views: ' + e.layer.views.length + '<br/>';
    statsHtml += 'Signs: ' + e.layer.signs.length + '<br/>';
    statsHtml += 'Player Beds: ' + e.layer.beds.length + '<br/>';

    //Do we care about the block counts anymore?
//        statsHtml += '<div class="center">Blocks</div>';
//        for (i in layer.blockStats) {
//            let stat = layer.blockStats[i];
//            statsHtml += stat.name + ' ' + stat.count + '<br/>';
//        }

    tippy('#mapInfo', {
        content: statsHtml,
        allowHTML: true,
        interactive: true,
        theme: 'light',
        maxWidth: 500
    });
}

function changeBackgroundColor(color) {
	let mapContainer = document.querySelector("#map");
	mapContainer.style.backgroundColor = color;
}

function onProjectionChanged(e) {
	// Store the previous coords in the layer
	activeBaseLayer.viewPosition.startPoint = mymap.getCenter();
	activeBaseLayer.viewPosition.zoom = mymap.getZoom();
}

//TODO: refactor these refresh marker methods to use Leaflet LayerGroups
function refreshSpawnMarker(layer, markersVisible) {
	destroyMarkers(spawnMarkers);

	// Spawn marker
	if (showSpawn && layer.worldVectors.hasOwnProperty('spawnPosition')) {
		let spawn = layer.projection.worldVectors.spawnPosition;
		let point = layer.projection.worldToMap(spawn);

		let myIcon = L.icon({
			iconUrl: 'Images/Spawn.png',
			// iconSize: [30, 30],
			iconAnchor: [17, 20],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});

        let marker = L.marker(point, { icon: myIcon }).bindPopup('<p class="center">' + spawn.x + ', ' + spawn.y + ', ' + spawn.z + '</p>').bindTooltip("Spawn Point");

		// Add marker to map if spawn is initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		spawnMarkers.push(marker);
	}
}

function refreshSignMarkers(layer, markersVisible) {
	destroyMarkers(signMarkers);

	// Sign markers
	for (i in layer.signs) {
		let sign = layer.signs[i];
		let point = layer.projection.worldToMap(sign.worldPos);

		let myIcon = L.icon({
			iconUrl: 'Images/Sign.png',
			// iconSize: [30, 30],
			iconAnchor: [17, 20],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});

		let marker = L.marker(point, { icon: myIcon }).bindPopup(
			'<pre><center>' + sign.text1 + '<br/>' + sign.text2 + '<br/>' + sign.text3 + '<br/>' + sign.text4 + '</center></pre>');

		// Add marker to map if signs are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		signMarkers.push(marker);
	}
}

function refreshViewMarkers(layer, markersVisible) {
	destroyMarkers(viewMarkers);

	// View markers
	for (i in layer.views) {
		let view = layer.views[i];

		let point = layer.projection.worldToMap(view.worldPos);

		let icon = L.icon({
			iconUrl: 'Images/Picture.png',
			// iconSize: [30, 30],
			iconAnchor: [17, 20],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});

		let html = '';
		html += '<div>';
		html += '<a href="' + view.imageFile + '">';
		html += '<img width="512" height="288" src="' + view.imageFile + '"/>';
		html += '</a>';
		html += '</div>';
		html += '';
		html += '<center>';
		html += view.text;
		html += '</center>';
		html += '';

		let marker = L.marker(point, { icon: icon }).bindPopup(html, {maxWidth: "auto"});

		// Add marker to map if chests are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		viewMarkers.push(marker);
	}
}

function loadIcon(src, player, pos, markersVisible) {
	var icon = new Image();
	icon.onload = function () {
		player.icon = src;
		var marker = createPlayerMarker(player, pos);

		// Add marker to map if players are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		playerMarkers.push(marker);
	};
	icon.onerror = function () {
		player.icon = player.icon = "Images/PlayerIcons/Tectonicus_Default_Player_Icon.png";
		var marker = createPlayerMarker(player, pos);

		// Add marker to map if players are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		playerMarkers.push(marker);
	};
	icon.src = src;
}

function refreshPlayerMarkers(layer, markersVisible) {
	destroyMarkers(playerMarkers);

	// Player markers
	for (i in layer.players) {
		let player = layer.players[i];

		player.donation = '';

		let point = layer.projection.worldToMap(player.worldPos);

		loadIcon("Images/PlayerIcons/" + player.name + ".png", player, point, markersVisible);
	}
}

function refreshBedMarkers(layer, markersVisible) {
	destroyMarkers(bedMarkers);

	// Bed markers
	for (i in layer.beds) {
		let bed = layer.beds[i];
		let point = layer.projection.worldToMap(bed.worldPos);

		let icon = L.icon({
			iconUrl: 'Images/Bed.png',
			// iconSize: [30, 30],
			iconAnchor: [17, 20],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});

		let marker = L.marker(point, { icon: icon }).bindPopup('<p class="center">' + bed.playerName + "'s bed</p>");

		// Add marker to map if beds are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		bedMarkers.push(marker);
	}
}

function refreshRespawnAnchorMarkers(layer, markersVisible) {
	destroyMarkers(respawnAnchorMarkers);

	// Respawn anchor markers
	for (i in layer.anchors) {
		let anchor = layer.anchors[i];
		let point = layer.projection.worldToMap(anchor.worldPos);

		let icon = L.icon({
			iconUrl: 'Images/RespawnAnchor.png',
			// iconSize: [30, 30],
			iconAnchor: [17, 20],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});

		let marker = L.marker(point, { icon: icon }).bindPopup('<p class="center">' + anchor.playerName + "'s respawn anchor</p>");

		// Add marker to map if beds are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		respawnAnchorMarkers.push(marker);
	}
}

function refreshPortalMarkers(layer, markersVisible) {
	destroyMarkers(portalMarkers);

	// Portal markers
	for (i in layer.portals) {
		let portal = layer.portals[i];

		let point = layer.projection.worldToMap(portal.worldPos);

		let icon = L.icon({
			iconUrl: 'Images/Portal.png',
			// iconSize: [30, 30],
			iconAnchor: [17, 20],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});

		let marker = L.marker(point, { icon: icon }).bindPopup(
			'<div style=\"text-align:center\">Portal</div><br/> position (' + portal.worldPos.x + ', ' + portal.worldPos.y + ', ' + portal.worldPos.z + ')');

		// Add marker to map if chests are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		portalMarkers.push(marker);
	}
}

const hiddenEnchantmentLevels = [
        'aqua_affinity',
        'binding_curse',
        'channeling',
        'flame',
        'infinity',
        'mending',
        'multishot',
        'silk_touch',
        'vanishing_curse'
];

const colorizedNames = {
        beacon: 'enchanted',
        conduit: 'enchanted',
        creeper_head: 'yellow',
        dragon_breath: 'yellow',
        dragon_egg: 'purple',
        dragon_head: 'yellow',
        enchanted_book: 'yellow',
        enchanted_golden_apple: 'purple',
        end_crystal: 'enchanted',
        experience_bottle: 'yellow',
        golden_apple: 'enchanted',
        heart_of_the_sea: 'yellow',
        music_disc_11: 'enchanted',
        music_disc_13: 'enchanted',
        music_disc_5: 'enchanted',
        music_disc_blocks: 'enchanted',
        music_disc_cat: 'enchanted',
        music_disc_chirp: 'enchanted',
        music_disc_far: 'enchanted',
        music_disc_mall: 'enchanted',
        music_disc_mellohi: 'enchanted',
        music_disc_otherside: 'enchanted',
        music_disc_pigstep: 'enchanted',
        music_disc_relic: 'enchanted',
        music_disc_stal: 'enchanted',
        music_disc_strad: 'enchanted',
        music_disc_wait: 'enchanted',
        music_disc_ward: 'enchanted',
        nether_star: 'yellow',
        piglin_head: 'yellow',
        player_head: 'yellow',
        skeleton_skull: 'yellow',
        totem_of_undying: 'yellow',
        wither_skeleton_skull: 'yellow',
        zombie_head: 'yellow'
};

function refreshChestMarkers(layer, markersVisible) {
	destroyMarkers(chestMarkers);
        
 	// Chest markers
	for (i in layer.chests) {
		let point = layer.projection.worldToMap(layer.chests[i].worldPos);

		let icon = L.icon({
			iconUrl: 'Images/Chest.png',
			// iconSize: [30, 30],
			iconAnchor: [17, 20],
			popupAnchor: [0, -10],
			//shadowUrl: 'my-icon-shadow.png',
			//shadowSize: [68, 95],
			//shadowAnchor: [22, 94]
		});
                
                let chest = layer.chests[i];
                let markerPopup = chest.large
                        ? '<div class="chest_container large"><div class="chest_scaler"><img class="chest large_chest" src="Images/LargeChest.png"/>'
                        : '<div class="chest_container"><div class="chest_scaler"><img class="chest" src="Images/SmallChest.png"/>';

                markerPopup += renderMinecraftText(chest.name, 'chest_name');
                
                for (j in chest.items) {
                        const item = layer.chests[i].items[j];
                        
                        const [namespace, itemId] = item.id.split(":");
                        const itemKey = `item.${namespace}.${itemId}`;
                        const itemDescKey = `item.${namespace}.${itemId}.desc`;
                        const blockKey = `block.${namespace}.${itemId}`;
                        
                        let itemNameAndDescription = itemId;
                        let isItem = true; // Assume we have an item
                        if (localizations) {
                                if (localizations[itemKey]) {
                                        itemNameAndDescription = localizations[itemKey];
                                } else if (localizations[blockKey]) {
                                        itemNameAndDescription = localizations[blockKey];
                                        isItem = false;
                                } else {
                                        isItem = false;
                                }
                        }
                        
                        let additionalItemNameCssClass = '';
                        if (item.enchantments) {
                                additionalItemNameCssClass = ' enchanted';
                        }
                        if (colorizedNames[itemId]) {
                                additionalItemNameCssClass = ' ' + colorizedNames[itemId];
                        }
                        
                        if (item.customName) {
                                let colouredNameRegex = /\{translate:(.*?),color:(.*?)\}/;
                                let matches = colouredNameRegex.exec(item.customName);
                                if (matches) {
                                        let resourceKey = matches[1];
                                        let color = matches[2];
                                        itemNameAndDescription = renderMinecraftText(localize(resourceKey), 'name renamed ' + color + additionalItemNameCssClass);
                                } else {                                
                                        itemNameAndDescription = renderMinecraftText(localize(item.customName), 'name renamed' + additionalItemNameCssClass);
                                }
                        } else {
                                itemNameAndDescription = renderMinecraftText(localize(itemNameAndDescription), 'name' + additionalItemNameCssClass);
                        }
                        
                        itemNameAndDescription += renderMinecraftText(localize(itemDescKey, null));
                        
                        if (item.trim) {
                                const labelKey = 'item.minecraft.smithing_template.upgrade';
                                const [patternNamespace, patternId] = item.trim.pattern.split(":");
                                const [materialNamespace, materialId] = item.trim.material.split(":");
                                
                                let label = 'Upgrade: ';
                                let pattern = `trim_pattern.${patternNamespace}.${patternId}`;
                                let material = `trim_material.${materialNamespace}.${materialId}`;
                                
                                label = localize(labelKey, label);
                                pattern = localize(pattern, patternId);
                                material = localize(material, materialId);
                                
                                itemNameAndDescription += renderMinecraftText(label);
                                itemNameAndDescription += renderMinecraftText(' ' + pattern, materialId);
                                itemNameAndDescription += renderMinecraftText(' ' + material, materialId);
                        }
                        
                        if (item.enchantments) {
                                for (const enchantment of item.enchantments) {
                                        const additionalEnchantmentCssClass = enchantment.id.indexOf('curse') >= 0 ? 'curse' : '';
                                        
                                        const [enchantmentNamespace, enchantmentId] = enchantment.id.split(":");
                                        const enchantmentKey = `enchantment.${enchantmentNamespace}.${enchantmentId}`;
                                        const enchantmentName = localize(enchantmentKey, enchantmentId);
                                        
                                        let enchantmentLevel = '';
                                        if (hiddenEnchantmentLevels.indexOf(enchantmentId) < 0) {
                                                enchantmentLevel = ' ' + intToRoman(enchantment.level);
                                        }
                                        
                                        itemNameAndDescription += renderMinecraftText(enchantmentName + enchantmentLevel, additionalEnchantmentCssClass);
                                }
                        }
                        
                        let pngName = itemId;
                        if (itemId === 'compass' || itemId === 'clock' || itemId === 'recovery_compass') {
                                // Choose 1st frame for animated items
                                pngName += '_00';
                        }
                        if (itemId === 'enchanted_golden_apple') {
                                pngName = 'golden_apple';
                        }
                                                
                        const row = Math.floor(item.slot/9);
                        const col = item.slot%9;
                        
                        const top = 18+18*row;
                        const left = 8+18*col;
                        
                        markerPopup += '<div class="item" style="top: ' + top + 'px; left: ' + left + 'px;">';
                        markerPopup += '<img src="Images/Items/' + (isItem ? pngName : 'barrier') + '.png" />';
                        
                        if (item.enchantments || itemId === 'enchanted_golden_apple')
                        {
                                markerPopup += '<div class="enchanted_glint" style="-webkit-mask-image: url(\'Images/Items/' + (isItem ? pngName : 'barrier') + '\.png\'); mask-image: url(\'Images/Items/' + (isItem ? pngName : 'barrier') + '.png\');"></div>';
                        }
                        
                        if (item.count > 1) {
                                markerPopup += renderMinecraftText(item.count.toString(), 'item_count');
                        }
                        if (!isItem) {
                                markerPopup += renderMinecraftText(itemId, 'item_name');
                        }
                        
                        markerPopup += '<div class=item_description>' + itemNameAndDescription + '</div></div>';
                }
                
                markerPopup += '</div></div>';

		let marker = L.marker(point, { icon: icon }).bindPopup(
                        markerPopup,
                        { maxWidth: "auto" } // Fix for incorrect sizing of the popup
                );

		// Add marker to map if chests are initially visible
		if (markersVisible) {
			marker.addTo(mymap);
		}

		chestMarkers.push(marker);
	}
}

function destroyMarkers(markers) {
	for (i in markers) {
		let marker = markers[i];
		marker.remove();
	}

	markers.length = 0;
}

function localize(key, fallbackString) {
        if (localizations && localizations[key]) {
                return localizations[key];
        }
        if (fallbackString === undefined) {
                fallbackString = key;
        }
        return fallbackString;
}

function intToRoman(num) {
    const romanNumerals = {
        M: 1000,
        CM: 900,
        D: 500,
        CD: 400,
        C: 100,
        XC: 90,
        L: 50,
        XL: 40,
        X: 10,
        IX: 9,
        V: 5,
        IV: 4,
        I: 1
    };

    let roman = '';

    for (let key in romanNumerals) {
        while (num >= romanNumerals[key]) {
            roman += key;
            num -= romanNumerals[key];
        }
    }

    return roman;
}

const charMap = ' !"#$%&\'()*+,-./' +
                '0123456789:;<=>?' +
                '@ABCDEFGHIJKLMNO' +
                'PQRSTUVWXYZ[\\]^_' +
                '\'abcdefghijklmno' +
                'pqrstuvwxyz{|}~';
const charWidths = ['', '!\',.:;i|', '`l', '"()*I[]t()', '<>fk'];

function findCharacterRowAndColumn(char) {
    const index = charMap.indexOf(char);
    
    if (index === -1) {
            return [15, 3]; // ? character
    }
    
    const col = index % 16;
    const row = Math.floor(index / 16 + 2); // +2 because 1st 2 lines are empty
    
    return [col, row];
}

function renderMinecraftText(text, className) {
    if (!text)
    {
            return null;
    }
    
    const characterWidth = 8;
    const characterHeight = 8;
    
    if (!className) {
            className = '';
    }

    let html = '<div class="mc_text_container ' + className + '">';

    for (let i = 0; i < text.length; i++) {
            const position = findCharacterRowAndColumn(text[i]);

            const left = position[0] * characterWidth;
            const top = position[1] * characterHeight;
            
            let widthOverride = '';
            
            for (let j = 1; j <= 4; j++) {
                    if (charWidths[j].indexOf(text[i])>=0) {
                            widthOverride = ` width: ${j+1}px;`;
                            break;
                    }
            }

            html += `<div class="mc_char" style="mask-position: -${left}px -${top}px;${widthOverride}"></div>`;
    }

    html += '</div>';
    
    return html;
}

function ViewPos(layerId, worldPos, zoom, startPoint) {
	this.layerId = layerId;
	this.worldPos = worldPos;
	this.zoom = zoom;
	this.startPoint = startPoint;
}

function findStartView(params, defaultLayerId, defaultSpawnPos) {
	let queryLayerId = defaultLayerId;
	let queryPos = new WorldCoord(defaultSpawnPos.x, defaultSpawnPos.y, defaultSpawnPos.z);
	let queryZoom = 0;

	if (params.hasOwnProperty('layerId')) {
		queryLayerId = params['layerId'];
	}

	if (params.hasOwnProperty('worldX')
		&& params.hasOwnProperty('worldY')
		&& params.hasOwnProperty('worldZ')) {
		queryPos.x = parseInt(params['worldX']);
		queryPos.y = parseInt(params['worldY']);
		queryPos.z = parseInt(params['worldZ']);
	}

	if (params.hasOwnProperty('zoom')) {
		queryZoom = parseInt(params['zoom']);
	}

	if (queryZoom < 0)
		queryZoom = 0;
	if (queryZoom > maxZoom)
		queryZoom = maxZoom;

	let layer = tileLayers.get(queryLayerId);
	let startPoint = layer.projection.worldToMap(queryPos);

	return new ViewPos(queryLayerId, queryPos, queryZoom, startPoint);
}

window.onload = main;
