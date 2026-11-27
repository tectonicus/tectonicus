const localizationTables = new Map();
const worldLocalizations = new Map();
const worldMasks = new Map();

function registerWorldLocalizations(worldId, assets) {
    worldLocalizations.set(worldId, assets);
}

function registerWorldMasks(worldId, masks) {
    worldMasks.set(worldId, masks);
}

function isLocalMap() {
    return window.location?.protocol === 'file:';
}

function loadViewerScript(url) {
    return new Promise(resolve => {
        const script = document.createElement('script');
        script.async = true;
        script.src = url;
        script.onload = resolve;
        script.onerror = () => {
            console.error('Unable to load viewer asset from ' + url);
            resolve();
        };
        document.head.appendChild(script);
    });
}

function worldMaskUrl(filename, layer = activeBaseLayer) {
    if (isLocalMap()) return worldMasks.get(layer?.worldId)?.[filename];
    return worldImageUrl(filename, layer);
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

// Item rarities as in Minecraft Java Edition 1.21.9+ (items not listed are common).
// The rarity decides the colour of the item name, see rarityCssClasses.
const itemRarities = {
        // Uncommon
        chainmail_boots: 'uncommon',
        chainmail_chestplate: 'uncommon',
        chainmail_helmet: 'uncommon',
        chainmail_leggings: 'uncommon',
        conduit: 'uncommon',
        creeper_banner_pattern: 'uncommon',
        creeper_head: 'uncommon',
        disc_fragment_5: 'uncommon',
        dragon_breath: 'uncommon',
        echo_shard: 'uncommon',
        experience_bottle: 'uncommon',
        goat_horn: 'uncommon',
        heart_of_the_sea: 'uncommon',
        music_disc_5: 'uncommon',
        music_disc_11: 'uncommon',
        music_disc_13: 'uncommon',
        music_disc_blocks: 'uncommon',
        music_disc_bounce: 'uncommon',
        music_disc_cat: 'uncommon',
        music_disc_chirp: 'uncommon',
        music_disc_creator_music_box: 'uncommon',
        music_disc_far: 'uncommon',
        music_disc_flurry: 'uncommon',
        music_disc_mall: 'uncommon',
        music_disc_mellohi: 'uncommon',
        music_disc_precipice: 'uncommon',
        music_disc_relic: 'uncommon',
        music_disc_stal: 'uncommon',
        music_disc_strad: 'uncommon',
        music_disc_tears: 'uncommon',
        music_disc_wait: 'uncommon',
        music_disc_ward: 'uncommon',
        nautilus_shell: 'uncommon',
        netherite_upgrade_smithing_template: 'uncommon',
        ominous_bottle: 'uncommon',
        piglin_banner_pattern: 'uncommon',
        piglin_head: 'uncommon',
        player_head: 'uncommon',
        recovery_compass: 'uncommon',
        skeleton_skull: 'uncommon',
        sniffer_egg: 'uncommon',
        totem_of_undying: 'uncommon',
        zombie_head: 'uncommon',
        bolt_armor_trim_smithing_template: 'uncommon',
        coast_armor_trim_smithing_template: 'uncommon',
        dune_armor_trim_smithing_template: 'uncommon',
        flow_armor_trim_smithing_template: 'uncommon',
        host_armor_trim_smithing_template: 'uncommon',
        raiser_armor_trim_smithing_template: 'uncommon',
        rib_armor_trim_smithing_template: 'uncommon',
        sentry_armor_trim_smithing_template: 'uncommon',
        shaper_armor_trim_smithing_template: 'uncommon',
        snout_armor_trim_smithing_template: 'uncommon',
        tide_armor_trim_smithing_template: 'uncommon',
        wayfinder_armor_trim_smithing_template: 'uncommon',
        wild_armor_trim_smithing_template: 'uncommon',
        // Rare
        beacon: 'rare',
        enchanted_book: 'rare',
        enchanted_golden_apple: 'rare',
        flow_banner_pattern: 'rare',
        guster_banner_pattern: 'rare',
        mojang_banner_pattern: 'rare',
        music_disc_creator: 'rare',
        music_disc_lava_chicken: 'rare',
        music_disc_otherside: 'rare',
        music_disc_pigstep: 'rare',
        nether_star: 'rare',
        skull_banner_pattern: 'rare',
        trident: 'rare',
        wither_skeleton_skull: 'rare',
        eye_armor_trim_smithing_template: 'rare',
        spire_armor_trim_smithing_template: 'rare',
        vex_armor_trim_smithing_template: 'rare',
        ward_armor_trim_smithing_template: 'rare',
        // Epic
        barrier: 'epic',
        chain_command_block: 'epic',
        command_block: 'epic',
        command_block_minecart: 'epic',
        debug_stick: 'epic',
        dragon_egg: 'epic',
        dragon_head: 'epic',
        elytra: 'epic',
        heavy_core: 'epic',
        jigsaw: 'epic',
        knowledge_book: 'epic',
        light: 'epic',
        mace: 'epic',
        repeating_command_block: 'epic',
        silence_armor_trim_smithing_template: 'epic',
        structure_block: 'epic',
        structure_void: 'epic',
        test_block: 'epic',
        test_instance_block: 'epic'
};

const rarityCssClasses = {
        common: '',
        uncommon: 'yellow',
        rare: 'enchanted',
        epic: 'purple'
};

function getItemRarity(item, itemId) {
        let rarity = itemRarities[itemId] ?? (itemId.endsWith('_pottery_sherd') ? 'uncommon' : 'common');
        // Enchanting raises the rarity: common/uncommon -> rare, rare -> epic. Stored enchantments
        // (enchanted books) don't count, and those are merged into item.enchantments by the exporter.
        if (item.enchantments?.length && itemId !== 'enchanted_book') {
                rarity = rarity === 'rare' || rarity === 'epic' ? 'epic' : 'rare';
        }
        return rarity;
}

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
        const scripts = new Set();
        const legacyTables = new Set();
        for (const dimension of contents) {
                if (dimension.localizationsScript) scripts.add(dimension.localizationsScript);
                else if (!isLocalMap()) legacyTables.add(dimension.localizations || 'Scripts/localizations.json');
                if (isLocalMap() && dimension.masksScript) scripts.add(dimension.masksScript);
        }

        await Promise.all([...scripts].map(loadViewerScript).concat([...legacyTables].map(async url => {
                try {
                        const response = await fetch(url);
                        if (!response.ok) throw new Error('HTTP ' + response.status);
                        localizationTables.set(url, await response.json());
                } catch (error) {
                        localizationTables.set(url, {});
                        console.error('Unable to load localizations from ' + url + '. Please host the map on a web server.', error);
                }
        })));
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

function localize(key, fallbackString, layer = activeBaseLayer) {
        const localizations = layer?.localizationsScript
                ? worldLocalizations.get(layer.worldId)?.translations
                : localizationTables.get(layer?.localizationsUrl || 'Scripts/localizations.json');
        if (localizations && Object.hasOwn(localizations, key) && localizations[key]) {
                return localizations[key];
        }
        if (fallbackString === undefined) {
                fallbackString = key;
        }
        return fallbackString;
}

function mapText(key, values = {}, layer = activeBaseLayer, fallback = mapEnglish[key]) {
    return String(localize(key, fallback, layer)).replace(/\{(\w+)\}/g, (match, name) =>
        Object.hasOwn(values, name) ? String(values[name]) : match);
}

function formatMapNumber(value, options) {
    return Number.isFinite(value) ? new Intl.NumberFormat(mapLanguage(), options).format(value) : '—';
}

function formatMapDate(timestamp, options) {
    return new Intl.DateTimeFormat(mapLanguage(), options).format(timestamp);
}

function mapLanguage() {
    const language = globalThis.document?.documentElement?.lang;
    if (!language) return 'en-US';
    try {
        return Intl.NumberFormat.supportedLocalesOf(language)[0] ?? 'en-US';
    } catch (error) {
        if (!(error instanceof RangeError)) throw error;
        // Minecraft's language codes may be invalid locale tags for Intl.
        return 'en-US';
    }
}

function formatMapCoordinates(position, round = value => value) {
    return Object.fromEntries(['x', 'y', 'z'].map(axis => [axis, formatMapNumber(round(position[axis]), {useGrouping: false, maximumFractionDigits: 20})]));
}

function formatMapDuration(milliseconds) {
    let seconds = Math.max(0, Math.floor(milliseconds / 1000));
    const parts = [];
    for (const [unit, divisor] of [['day', 86400], ['hour', 3600], ['minute', 60], ['second', 1]]) {
        const count = Math.floor(seconds / divisor);
        if (count || unit === 'second') parts.push(formatMapNumber(count, {style: 'unit', unit, unitDisplay: 'long'}));
        seconds %= divisor;
    }
    return new Intl.ListFormat(mapLanguage(), {type: 'unit'}).format(parts);
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

function minecraftTextColor(rgb) {
    if (!Number.isInteger(rgb) || rgb < 0 || rgb > 0xffffff) return null;
    return '#' + rgb.toString(16).padStart(6, '0');
}

function minecraftTextRuns(text, colors) {
    const runs = [];
    let offset = 0;
    for (const {start, end, rgb} of colors) {
        const color = minecraftTextColor(rgb);
        if (!color || start < offset || end <= start || end > text.length) continue;
        if (start > offset) runs.push({text: text.slice(offset, start)});
        runs.push({text: text.slice(start, end), color});
        offset = end;
    }
    if (offset < text.length) runs.push({text: text.slice(offset)});
    const palette = ['#000000', '#0000aa', '#00aa00', '#00aaaa', '#aa0000', '#aa00aa', '#ffaa00', '#aaaaaa',
        '#555555', '#5555ff', '#55ff55', '#55ffff', '#ff5555', '#ff55ff', '#ffff55', '#ffffff'];
    let legacyColor;
    const formats = new Set();
    return runs.flatMap(run => {
        const formatted = [];
        let offset = 0;
        const append = end => {
            if (end <= offset) return;
            const plain = run.text.slice(offset, end);
            const visible = formats.has('k')
                ? Array.from(plain, (char, index) => /\s/u.test(char) ? char
                    : 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'[(char.codePointAt(0) + index) % 36]).join('')
                : plain;
            formatted.push({text: visible, plain, color: legacyColor ?? run.color,
                classes: [...formats].filter(format => format !== 'k')});
        };
        for (const match of run.text.matchAll(/\u00a7([0-9a-fk-or])/gi)) {
            append(match.index);
            const code = match[1].toLowerCase();
            if (code === 'r' || /^[0-9a-f]$/.test(code)) {
                formats.clear();
                legacyColor = code === 'r' ? undefined : palette[parseInt(code, 16)];
            } else {
                formats.add(legacyTextClasses[code] ?? code);
            }
            offset = match.index + match[0].length;
        }
        append(run.text.length);
        return formatted;
    });
}

const legacyTextClasses = {
    l: 'mc_text_bold', m: 'mc_text_struck', n: 'mc_text_underlined', o: 'mc_text_italic'
};

// Renders Minecraft text (with § formatting codes and per-range colours) as real text in the
// Minecraft web font; styling lives in the .mc_text rules of tectonicusStyles.css.
function renderMinecraftText(text, className, colors = []) {
    if (!text) return null;

    const runs = minecraftTextRuns(text, colors);
    const content = runs.map(run => {
        const classes = run.classes.length ? ` class="${run.classes.join(' ')}"` : '';
        const color = run.color ? ` style="color:${run.color}"` : '';
        const text = escapeHtml(run.text);
        return classes || color ? `<span${classes}${color}>${text}</span>` : text;
    }).join('');
    // Obfuscated (§k) text shows random characters, so give screen readers the real text.
    const obfuscated = runs.some(run => run.text !== run.plain);
    const label = obfuscated ? ` aria-label="${escapeHtml(runs.map(run => run.plain).join(''))}"` : '';
    return `<div class="mc_text ${escapeHtml(className || '')}"${label}>${content}</div>`;
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
		const coordinates = formatMapCoordinates(pos, Math.floor);
		const name = container.nameKey === null ? container.name : mapText('container.enderchest');
		return '<div style="text-align:center">' + escapeHtml(name)
			+ '</div><br/><div class="center">' + escapeHtml(mapText('tectonicus.marker.coordinates', coordinates)) + '</div>';
	}

        let markerPopup = container.large
                ? '<div class="container_inventory large"><div class="container_inventory_scaler"><img src="' + worldImageUrl('LargeChest.png') + '" />'
                : '<div class="container_inventory"><div class="container_inventory_scaler"><img src="' + worldImageUrl('SmallChest.png') + '" />';

        const name = container.nameKey ? mapText(container.nameKey, {}, activeBaseLayer, container.name) : container.name;
        markerPopup += renderMinecraftText(name, 'container_name');

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
        const itemImage = worldImageUrl('Items/' + pngName + '.png');
        const bannerIcon = typeof item.bannerIcon === 'string' && /^banner_[0-9a-f]{64}$/.test(item.bannerIcon)
                ? worldImageUrl('Items/' + item.bannerIcon + '.png') : null;
        let html = '<div class="item" style="top: ' + top + 'px; left: ' + left + 'px;">';
        html += '<img src="' + (bannerIcon || itemImage) + '" onload="checkImageSize(this)"';
        if (bannerIcon)
                html += ' data-fallback="' + itemImage + '" onerror="this.onerror = null; this.src = this.dataset.fallback"';
        html += ' />';
        html += getColorLayer(item, itemId);
        html += getOverlay(item, itemId, pngName);
        html += getEnchantmentGlint(item, itemId, pngName);
        if (item.count > 1) {
                html += renderMinecraftText(formatMapNumber(item.count), 'item_count');
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
    
        const rarityCssClass = rarityCssClasses[getItemRarity(item, itemId)];
        const additionalItemNameCssClass = rarityCssClass ? ' ' + rarityCssClass : '';
                
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
                        // An explicit colour in the custom name overrides the rarity colour
                        return renderMinecraftText(localize(translate, text), 'name italic ' + (color || additionalItemNameCssClass.trim()));
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
                const url = worldMaskUrl('Items/' + pngName + '.png');
                if (!url) return '';
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
                const url = worldMaskUrl('Items/' + itemId + '.png');
                if (!url) return '';
                color ??= 'rgb(106, 64, 41)';
                return '<div class="color_layer" style="background-color: ' + color + '; mask-image: url(\'' + url + '\');"></div>';
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
