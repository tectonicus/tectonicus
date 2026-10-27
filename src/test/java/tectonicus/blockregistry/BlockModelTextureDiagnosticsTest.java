/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockregistry;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import tectonicus.configuration.MutableConfiguration;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.Texture;
import tectonicus.texture.TexturePack;
import tectonicus.texture.ZipStack;
import tools.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class BlockModelTextureDiagnosticsTest {
	@TempDir Path directory;
	private TexturePack textures;
	private final ListAppender<ILoggingEvent> messages = new ListAppender<>();
	private final List<Logger> loggers = List.of((Logger) LoggerFactory.getLogger(BlockModel.class),
			(Logger) LoggerFactory.getLogger(TexturePack.class));

	@BeforeEach
	void createTexturePack() throws Exception {
		Path jar = directory.resolve("client.jar");
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
			out.putNextEntry(new ZipEntry("version.json"));
			out.write("{\"name\":\"1.21.11\",\"pack_version\":{\"resource\":75}}".getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
			for (String path : List.of("gui/sprites/hud/heart/container.png", "gui/sprites/hud/heart/half.png",
					"gui/sprites/hud/heart/full.png", "gui/sprites/hud/food_empty.png", "gui/sprites/hud/food_half.png",
					"gui/sprites/hud/food_full.png", "gui/sprites/hud/air_bursting.png", "gui/sprites/hud/air.png",
					"gui/container/generic_54.png", "block/dirt.png")) {
				out.putNextEntry(new ZipEntry("assets/minecraft/textures/" + path));
				ImageIO.write(image(path.contains("generic_54") ? 256 : 16), "png", out);
				out.closeEntry();
			}
		}
		Rasteriser rasteriser = (Rasteriser) Proxy.newProxyInstance(Rasteriser.class.getClassLoader(), new Class<?>[]{Rasteriser.class},
				(proxy, method, args) -> {
					if (method.getName().equals("getMaxTextureSize"))
						return 16384;
					if (!method.getName().equals("createTexture"))
						throw new AssertionError("Unexpected rasteriser call: " + method.getName());
					BufferedImage image = (BufferedImage) args[0];
					return new Texture() {
						@Override public int getWidth() { return image.getWidth(); }
						@Override public int getHeight() { return image.getHeight(); }
					};
				});
		MutableConfiguration config = new MutableConfiguration();
		config.setMinecraftJar(jar.toFile());
		textures = new TexturePack(rasteriser, config, List.of(), List.of());
		messages.start();
		loggers.forEach(logger -> logger.addAppender(messages));
	}

	@AfterEach
	void close() throws Exception {
		loggers.forEach(logger -> logger.detachAppender(messages));
		messages.stop();
		if (textures != null) {
			var field = ZipStack.class.getDeclaredField("base");
			field.setAccessible(true);
			((FileSystem) field.get(textures.getZipStack())).close();
		}
	}

	@Test
	void unresolvedReferencesAreReportedOncePerModelWithoutNullPngLookups() throws Exception {
		BlockModel model = model("block/test", "#atlas", Map.of());

		assertEquals(Set.of("#atlas"), model.getMissingTextures());
		assertEquals(1, count("Unresolved texture reference '#atlas' for model: block/test"));
		assertEquals(0, count("null.png"));
		assertEquals(0, count("could not be found"));
		for (var face : model.getElements().getFirst().getFaces().values())
			assertSame(textures.getPackTexture("missing_texture").getFullTexture().texture, face.getTexture().texture);
	}

	@Test
	void missingFilesAreWarnedOnceAcrossFacesModelsAndFrameRequests() throws Exception {
		model("block/first", "#side", Map.of("side", "block/absent"));
		model("block/second", "#side", Map.of("side", "block/absent"));
		assertNull(textures.findTextureFrameOrDefault("block/absent.png", 2, null));

		assertEquals(1, count("The texture file 'assets/minecraft/textures/block/absent.png' could not be found."));
		assertEquals(1, count("Missing texture: block/absent.png for model: block/first"));
		assertEquals(1, count("Missing texture: block/absent.png for model: block/second"));
	}

	@Test
	void generatedTexturesCanReplaceAnEarlierMissingFile() {
		assertNull(textures.findTexture("block/generated.png"));
		var generated = textures.findTexture(image(16), "assets/minecraft/textures/block/generated.png");

		assertSame(generated.texture, textures.findTexture("block/generated.png").texture);
	}

	@ParameterizedTest
	@ValueSource(strings = {"#side", "side", "minecraft:block/dirt", "block/dirt"})
	void validAliasesAndDirectPathsDoNotProduceMissingTextureWarnings(String reference) throws Exception {
		BlockModel model = model("block/test", reference, Map.of("side", "minecraft:block/dirt"));

		assertTrue(model.getMissingTextures().isEmpty());
		assertTrue(messages.list.isEmpty());
	}

	private BlockModel model(String name, String reference, Map<String, String> textureMap) throws Exception {
		String elements = """
				[{"from":[0,0,0],"to":[16,16,16],"faces":{
				"north":{"texture":"%s"},"south":{"texture":"%s"},"west":{"texture":"%s"}}}]
				""".formatted(reference, reference, reference);
		return new BlockModel(name, true, textureMap, new ObjectMapper().readTree(elements), textures);
	}

	private long count(String text) {
		return messages.list.stream().filter(event -> event.getFormattedMessage().contains(text)).count();
	}

	private BufferedImage image(int size) {
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < size; y++)
			for (int x = 0; x < size; x++)
				image.setRGB(x, y, 0xff885533);
		return image;
	}
}
