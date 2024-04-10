var localizations = null;

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

async function containersMainAsync() {
        let localizationsResponse;
        
        try {
                localizationsResponse = await fetch('Scripts/localizations.json');
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

}

function updateContainerItemTooltipPosition(e) {
            let itemDescriptionTooltip = document.querySelector(".item:hover .item_description");

            if (itemDescriptionTooltip) {
                let x = e.clientX,
                    y = e.clientY;
            
                let rect = itemDescriptionTooltip.parentElement.getBoundingClientRect();

                const scale = 3; // The scale should be set to the same scale as in CSS

                itemDescriptionTooltip.style.top = (y - rect.top - 10) / scale + 'px';
                itemDescriptionTooltip.style.left = (x - rect.left + 20) / scale + 'px';
            }
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

function createChestPopup(chest) {
        let markerPopup = chest.large
                ? '<div class="chest_container large"><div class="chest_scaler"><img class="chest large_chest" src="Images/LargeChest.png"/>'
                : '<div class="chest_container"><div class="chest_scaler"><img class="chest" src="Images/SmallChest.png"/>';

        markerPopup += renderMinecraftText(chest.name, 'chest_name');

        for (j in chest.items) {
                const item = chest.items[j];

                const [namespace, itemId] = item.id.split(":");
                const itemKey = `item.${namespace}.${itemId}`;
                const itemDescKey = `item.${namespace}.${itemId}.desc`;
                const blockKey = `block.${namespace}.${itemId}`;

                let additionalItemNameCssClass = '';
                if (item.enchantments) {
                        additionalItemNameCssClass = ' enchanted';
                }
                if (colorizedNames[itemId]) {
                        additionalItemNameCssClass = ' ' + colorizedNames[itemId];
                }

                let itemNameAndDescription;
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
                                itemNameAndDescription = renderMinecraftText(localize(translate, text), 'name italic ' + color + additionalItemNameCssClass);
                        } else {                                
                                itemNameAndDescription = renderMinecraftText(localize(item.customName), 'name italic' + additionalItemNameCssClass);
                        }
                } else {
                        itemNameAndDescription = localize(itemKey, localize(blockKey, itemId));
                        itemNameAndDescription += renderMinecraftText(itemNameAndDescription, 'name' + additionalItemNameCssClass);
                }

                itemNameAndDescription += renderMinecraftText(localize(itemDescKey, null));
                itemNameAndDescription += getTrimDescription(item);
                itemNameAndDescription += getEnchantmentsDescription(item);
                itemNameAndDescription += item.color ? renderMinecraftText(localize('item.dyed'), 'italic') : '';

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
                markerPopup += '<img src="Images/Items/' + pngName + '.png" />';
                
                markerPopup += getColorLayer(intToHTMLColor(item?.color), itemId);
                markerPopup += getLeatherOverlay(item, itemId);
                markerPopup += getArmorTrimOverlay(item, itemId);
                markerPopup += getEnchantmentGlint(item, itemId, pngName);

                if (item.count > 1) {
                        markerPopup += renderMinecraftText(item.count.toString(), 'item_count');
                }

                markerPopup += '<div class=item_description>' + itemNameAndDescription + '</div></div>';
        }

        markerPopup += '</div></div>';
        
        return markerPopup;
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
                return '<div class="enchanted_glint" style="-webkit-mask-image: url(\'Images/Items/' + pngName + '\.png\'); mask-image: url(\'Images/Items/' + pngName + '.png\');"></div>';
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

function getColorLayer(color, itemId) {
        if (color || itemId.indexOf('leather_') >= 0) {
                color ??= 'rgb(106, 64, 41)';
                return '<div class="color_layer" style="background-color: ' + color + '; mask-image: url(\'Images/Items/' + itemId + '.png\');"></div>';
        }
        return '';
}

function getLeatherOverlay(item, itemId) {
        let result = '';
        if (itemId === 'leather_boots' || itemId === 'leather_chestplate' || itemId === 'leather_helmet' || itemId === 'leather_leggings') {
                let overlayId = itemId + '_overlay';
                result += '<img src="Images/Items/' + overlayId + '.png" />';
                result += getEnchantmentGlint(item, overlayId, true, overlayId);
        }
        return result;
}

function getArmorTrimOverlay(item, itemId) {
        let result = '';
        if (item.trim) {
                const [material, armorPiece] = itemId.split("_");
                if (armorPiece === 'boots' || armorPiece === 'chestplate' || armorPiece === 'helmet' || armorPiece === 'leggings') {
                        let color;
                        
                        switch(item.trim.material) {
                                case 'minecraft:amethyst':
                                        color = '#d393ff';
                                        break;
                                case 'minecraft:copper':
                                        color = '#ff9474';
                                        break;
                                case 'minecraft:diamond':
                                        color = '#71deff';
                                        break;
                                case 'minecraft:emerald':
                                        color = '#43ff83';
                                        break;
                                case 'minecraft:gold':
                                        color = '#ffe300';
                                        break;
                                case 'minecraft:iron':
                                        color = '#d2d2d2';
                                        break;
                                case 'minecraft:lapis':
                                        color = '#3c6bc6';
                                        break;
                                case 'minecraft:netherite':
                                        color = '#666666';
                                        break;
                                case 'minecraft:quartz':
                                        color = '#ffffff';
                                        break;
                                case 'minecraft:redstone':
                                        color = '#ff0000';
                                        break;
                        }
                    
                        result += '<img src="Images/Items/' + armorPiece + '_trim.png" />';
                        result += getColorLayer(color, armorPiece + '_trim');
                }
        }
        return result;
}