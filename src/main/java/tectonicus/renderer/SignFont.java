/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.renderer;

import lombok.Getter;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.Texture;
import tectonicus.rasteriser.TextureFilter;
import tectonicus.raw.SignEntity;
import tectonicus.texture.SubTexture;
import tectonicus.texture.ZipStack;
import tectonicus.util.MinecraftFont;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/** Shares texture pages across all rendered signs using the same texture pack. */
public final class SignFont implements AutoCloseable {
	private static final int GLYPHS_PER_PAGE = 1024;
	private final Rasteriser rasteriser;
	@Getter
	private final Locale locale;
	private final Function<Set<Integer>, Map<Integer, MinecraftFont.Glyph>> loader;
	private final Map<Integer, Glyph> glyphs = new HashMap<>();
	private final Set<Integer> requested = new HashSet<>();
	private final Set<Integer> pending = new HashSet<>();
	private final Set<Texture> pages = new HashSet<>();

	public SignFont(Rasteriser rasteriser, ZipStack assets, Locale locale) {
		this.rasteriser = rasteriser;
		this.locale = locale;
		loader = MinecraftFont.loader(assets, locale);
	}

	public void collect(SignEntity sign) {
		for (String line : sign.getFront().lines()) collect(line);
		if (sign.getBack() != null) {
			for (String line : sign.getBack().lines()) collect(line);
		}
	}

	private void collect(String line) {
		line.codePoints().filter(code -> !Character.isISOControl(code)).forEach(this::request);
		if (!Normalizer.isNormalized(line, Normalizer.Form.NFC)) {
			Normalizer.normalize(line, Normalizer.Form.NFC).codePoints()
					.filter(code -> !Character.isISOControl(code)).forEach(this::request);
		}
	}

	private void request(int code) {
		if (requested.add(code)) pending.add(code);
	}

	void prepare(SignEntity sign) {
		collect(sign);
		if (pending.isEmpty()) return;
		request('?');
		Map<Integer, MinecraftFont.Glyph> page = new TreeMap<>();
		loader.apply(pending).forEach((code, glyph) -> {
			page.put(code, glyph);
			if (page.size() == GLYPHS_PER_PAGE) {
				upload(page);
				page.clear();
			}
		});
		if (!page.isEmpty()) upload(page);
		pending.clear();
	}

	private void upload(Map<Integer, MinecraftFont.Glyph> page) {
		var atlas = MinecraftFont.pack(page);
		var image = atlas.image();
		var texture = image == null ? null : rasteriser.createTexture(image, TextureFilter.NEAREST);
		if (texture != null) pages.add(texture);
		atlas.metrics().forEach((code, metrics) -> {
			float width = (float) metrics[2];
			float height = (float) metrics[3];
			SubTexture sprite = null;
			if (texture != null && width > 0) {
				float x = (float) metrics[0] * 2;
				float y = (float) metrics[1] * 2;
				sprite = new SubTexture(texture, x / image.getWidth(), y / image.getHeight(),
						(x + width * 2) / image.getWidth(), (y + height * 2) / image.getHeight());
			}
			glyphs.put(code, new Glyph(sprite, width, height, (float) metrics[4], (float) metrics[5]));
		});
	}

	Glyph getGlyph(int code) {
		Glyph glyph = glyphs.get(code);
		return glyph != null || Character.isWhitespace(code) ? glyph : glyphs.get((int) '?');
	}

	@Override
	public void close() {
		pages.forEach(rasteriser::destroyTexture);
		pages.clear();
	}

	record Glyph(SubTexture sprite, float width, float height, float advance, float top) {}
}
