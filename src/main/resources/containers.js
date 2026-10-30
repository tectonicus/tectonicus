const localizationTables = new Map();

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

const potionColors = {
        water: '#385dc6',
        mundane: '#385dc6',
        thick: '#385dc6',
        awkward: '#385dc6',
        night_vision: '#c2ff66',
        invisibility: '#f6f6f6',
        leaping: '#fdff84',
        fire_resistance: '#ff9900',
        swiftness: '#33ebff',
        slowness: '#8bafe0',
        water_breathing: '#98dac0',
        healing: '#f82423',
        harming: '#a9656a',
        poison: '#87a363',
        regeneration: '#cd5cab',
        strength: '#ffc700',
        weakness: '#484d48',
        luck: '#f7f8e0',
        turtle_master: '#8e7be8',
        slow_falling: '#f7f8e0'
};

async function containersMainAsync() {
        const urls = new Set(contents.map(dimension => dimension.localizations || 'Scripts/localizations.json'));
        await Promise.all([...urls].map(async url => {
                try {
                        const response = await fetch(url);
                        if (!response.ok) throw new Error('HTTP ' + response.status);
                        localizationTables.set(url, await response.json());
                } catch (error) {
                        localizationTables.set(url, {});
                        console.error('Unable to load localizations from ' + url + '. Please host the map on a web server.', error);
                }
        }));
}

function updateContainerItemTooltipPosition(e) {
            let itemDescriptionTooltip = document.querySelector(".item:hover .item_description");

            if (itemDescriptionTooltip) {
                let x = e.clientX,
                    y = e.clientY;
            
                let rect = itemDescriptionTooltip.parentElement.getBoundingClientRect();

                // Player inventories and container popups use different display scales.
                const scale = rect.width / itemDescriptionTooltip.parentElement.offsetWidth;

                itemDescriptionTooltip.style.top = (y - rect.top - 10) / scale + 'px';
                itemDescriptionTooltip.style.left = (x - rect.left + 20) / scale + 'px';
            }
}

function localize(key, fallbackString) {
        const localizations = localizationTables.get(activeBaseLayer?.localizationsUrl || 'Scripts/localizations.json');
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

function containerMarkerIcon(container) {
	switch (container.containerType) {
		case 'trapped_chest': return 'TrappedChest.png';
		case 'barrel': return 'Barrel.png';
		case 'shulker_box': {
			const colors = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black'];
			return 'ShulkerBox' + (colors.includes(container.shulkerColor) ? '_' + container.shulkerColor : '') + '.png';
		}
		case 'ender_chest': return 'EnderChest.png';
		default: return container.enderChest ? 'EnderChest.png' : 'Chest.png';
	}
}

function createContainerPopup(container) {
	if (container.enderChest || container.containerType === 'ender_chest') {
		const pos = container.worldPos;
		return '<div style="text-align:center">Ender Chest</div><br/> position ('
			+ Math.floor(pos.x) + ', ' + Math.floor(pos.y) + ', ' + Math.floor(pos.z) + ')';
	}

        let markerPopup = container.large
                ? '<div class="container_inventory large"><div class="container_inventory_scaler"><img src="' + worldImageUrl('LargeChest.png') + '" />'
                : '<div class="container_inventory"><div class="container_inventory_scaler"><img src="' + worldImageUrl('SmallChest.png') + '" />';

        markerPopup += renderMinecraftText(container.name, 'container_name');

        const items = new Map(container.items.map(item => [item.slot, item]));
        const slotCount = container.large ? 54 : 27;
        for (let slot = 0; slot < slotCount; slot++) {
                const row = Math.floor(slot/9);
                const col = slot%9;

                const top = 17+18*row;
                const left = 7+18*col;

                markerPopup += createInventorySlotHtml(items, slot, top, left);
        }

        markerPopup += '</div></div>';
        
        return markerPopup;
}

function createItemHtml(item, top, left) {
        const [namespace, itemId] = item.id.split(":");
        const itemDescKey = `item.${namespace}.${itemId}.desc`;
        let description = getItemName(item);
        description += renderMinecraftText(localize(itemDescKey, null)) || '';
        description += getTrimDescription(item);
        description += getEnchantmentsDescription(item);
        description += item.color ? renderMinecraftText(localize('item.dyed'), 'italic') : '';

        const pngName = getPngName(item, itemId);
        let html = '<div class="item" style="top: ' + top + 'px; left: ' + left + 'px;">';
        html += '<img src="' + worldImageUrl('Items/' + pngName + '.png') + '" onload="checkImageSize(this)" />';
        html += getColorLayer(item, itemId);
        html += getOverlay(item, itemId, pngName);
        html += getEnchantmentGlint(item, itemId, pngName);
        if (item.count > 1) {
                html += renderMinecraftText(item.count.toString(), 'item_count');
        }
        return html + '<div class="item_description">' + description + '</div></div>';
}

function getPngName(item, itemId) {
        let pngName = itemId;
        
        if (item.trim) {
                const [namespace, material] = item.trim.material.split(":");
                pngName += '_' + material + '_trim';
        }
        
        return pngName;
}

function getItemName(item) {
        const [namespace, itemId] = item.id.split(":");
        let itemKey = `item.${namespace}.${itemId}`;
        const blockKey = `block.${namespace}.${itemId}`;
        
        if (item.components && item.components.potionContents && item.components.potionContents.potion) {
                const [potionNamespace, potionId] = item.components.potionContents.potion.split(":");
                itemKey = `${itemKey}.effect.${potionId.replace(/^(strong|long)_/, '')}`;
        }
    
        let additionalItemNameCssClass = '';
        if (item.enchantments) {
                additionalItemNameCssClass = ' enchanted';
        }
        if (colorizedNames[itemId]) {
                additionalItemNameCssClass = ' ' + colorizedNames[itemId];
        }
                
        if (item.customName) {
                // Parse custom name. Possible values if matched:
                //      {text:Custom name}
                //      {text:Custom name,color:gold}
                //      {translate:resource_key}
                //      {translate:resource_key,color:gold}
                let formattedCustomNameRegex = /{(text:(?<text>[^,)]*))?,?(translate:(?<translate>[^,)]*))?,?(color:(?<color>[^)]*))?}/;
                let matches = formattedCustomNameRegex.exec(item.customName);
                if (matches) {
                        let { text, translate, color } = matches.groups;
                        return renderMinecraftText(localize(translate, text), 'name italic ' + color + additionalItemNameCssClass);
                }
                return renderMinecraftText(localize(item.customName), 'name italic' + additionalItemNameCssClass);
        }
        return renderMinecraftText(localize(itemKey, localize(blockKey, itemId)), 'name' + additionalItemNameCssClass);
}

function getTrimDescription(item) {
        let result = '';
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

                result += renderMinecraftText(label);
                result += renderMinecraftText(' ' + pattern, materialId);
                result += renderMinecraftText(' ' + material, materialId);
        }
        return result;
}

function getEnchantmentsDescription(item) {
        let result = '';
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

                        result += renderMinecraftText(enchantmentName + enchantmentLevel, additionalEnchantmentCssClass);
                }
        }
        return result;
}

function getEnchantmentGlint(item, itemId, pngName) {
        if (item.enchantments || itemId === 'enchanted_golden_apple')
        {
                const url = worldImageUrl('Items/' + pngName + '.png');
                return '<div class="enchanted_glint" style="-webkit-mask-image: url(\'' + url + '\'); mask-image: url(\'' + url + '\');"></div>';
        }
        return '';
}

function intToHTMLColor(colorCode) {
        if (!colorCode) {
                return null;
        }
        var hexColor = colorCode.toString(16);
        while (hexColor.length < 6) {
                hexColor = "0" + hexColor;
        }
        return "#" + hexColor;
}

function getColorLayer(item, itemId) {
        let color = intToHTMLColor(item?.color);
        
        if (item.components && item.components.potionContents && item.components.potionContents.potion) {
                const potionId = item.components.potionContents.potion.replace(/^minecraft:((strong|long)_)?/, '');
                if (potionColors[potionId]) {
                        color = potionColors[potionId];
                }
                else {
                        color = '#ff00ff';
                }
        }
        
        if (color || itemId.indexOf('leather_') >= 0) {
                color ??= 'rgb(106, 64, 41)';
                return '<div class="color_layer" style="background-color: ' + color + '; mask-image: url(\'' + worldImageUrl('Items/' + itemId + '.png') + '\');"></div>';
        }
        
        return '';
}

function getOverlay(item, itemId, pngName) {
        let result = '';
        if (itemId === 'leather_boots' || itemId === 'leather_chestplate' || itemId === 'leather_helmet' || itemId === 'leather_leggings' || itemId.indexOf('potion') >= 0 || itemId === 'tipped_arrow') {
                let overlayId = pngName + '_overlay';
                result += '<img src="' + worldImageUrl('Items/' + overlayId + '.png') + '" />';
                result += getEnchantmentGlint(item, overlayId, overlayId);
        }
        return result;
}

function checkImageSize(image) {
        if (image.naturalWidth !== 16 || image.naturalHeight !== 16) {
                image.classList.add('item_3d');
        }
}
