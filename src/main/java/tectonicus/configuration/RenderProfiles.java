/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.configuration;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Expands local profile defaults before the ordinary scope inheritance pass. */
final class RenderProfiles {
	private static final String RESOURCE = "/renderProfiles.xml";

	private RenderProfiles() {}

	static void expand(Element root, List<String> renderAttributes) {
		Map<String, Element> profiles = new HashMap<>();
		readProfiles(loadBuiltIns(), renderAttributes, profiles);
		for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element container && container.getTagName().equals("renderProfiles"))
				readProfiles(container, renderAttributes, profiles);
		}
		var renders = root.getElementsByTagName("render");
		for (int i = 0; i < renders.getLength(); i++) {
			Element render = (Element)renders.item(i);
			String name = render.getAttribute("profile").trim().toLowerCase(Locale.ROOT);
			if (name.isEmpty()) continue;
			Element definition = profiles.get(name);
			if (definition == null) throw new IllegalArgumentException("Unknown render profile '" + name + "'");
			Element scope = (Element)render.getParentNode();
			for (int j = 0; j < definition.getAttributes().getLength(); j++) {
				Node attribute = definition.getAttributes().item(j);
				String key = attribute.getNodeName();
				if (!key.equals("name") && !render.hasAttribute(key) && !scope.hasAttribute(key))
					render.setAttribute(key, attribute.getNodeValue());
			}
		}
	}

	private static Element loadBuiltIns() {
		try (var stream = RenderProfiles.class.getResourceAsStream(RESOURCE)) {
			if (stream == null) throw new IllegalStateException("Missing built-in render profile resource " + RESOURCE);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			Element container = factory.newDocumentBuilder().parse(stream).getDocumentElement();
			if (!container.getTagName().equals("renderProfiles"))
				throw new IllegalStateException("Expected <renderProfiles> in " + RESOURCE);
			return container;
		} catch (Exception e) {
			throw new IllegalStateException("Could not load built-in render profiles from " + RESOURCE, e);
		}
	}

	private static void readProfiles(Element container, List<String> renderAttributes, Map<String, Element> profiles) {
		for (Node child = container.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (!(child instanceof Element definition)) continue;
			if (!definition.getTagName().equals("profile"))
				throw new IllegalArgumentException("Expected <profile> inside <renderProfiles>");
			String name = definition.getAttribute("name").trim().toLowerCase(Locale.ROOT);
			if (name.isEmpty()) throw new IllegalArgumentException("Render profile requires a name");
			// Accept the same alias in bundled and user-defined profiles.
			if (!definition.hasAttribute("useBiomeColours") && definition.hasAttribute("useBiomeColors"))
				definition.setAttribute("useBiomeColours", definition.getAttribute("useBiomeColors"));
			definition.removeAttribute("useBiomeColors");
			for (int i = 0; i < definition.getAttributes().getLength(); i++) {
				String key = definition.getAttributes().item(i).getNodeName();
				if (!key.equals("name") && !renderAttributes.contains(key))
					throw new IllegalArgumentException("Unknown render option '" + key + "' in profile '" + name + "'");
			}
			for (Node nested = definition.getFirstChild(); nested != null; nested = nested.getNextSibling()) {
				if (nested instanceof Element)
					throw new IllegalArgumentException("Render profile '" + name + "' takes render attributes, not child elements");
			}
			if (profiles.putIfAbsent(name, definition) != null)
				throw new IllegalArgumentException("Duplicate or reserved render profile '" + name + "'");
		}
	}
}
