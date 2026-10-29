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
import tectonicus.BlockContext;
import tectonicus.Minecraft;
import tectonicus.configuration.Configuration;
import tectonicus.configuration.LightStyle;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.Texture;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.renderer.Geometry.MeshType;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class BlockModelTextureDiagnosticsTest {
	@TempDir Path directory;
	private TexturePack textures;
	private int previousHeight;
	private final ListAppender<ILoggingEvent> messages = new ListAppender<>();
	private final List<Logger> loggers = List.of((Logger) LoggerFactory.getLogger(BlockModel.class),
			(Logger) LoggerFactory.getLogger(TexturePack.class));

	@BeforeEach
	void createTexturePack() throws Exception {
		previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(16);
		Path jar = directory.resolve("client.jar");
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
			out.putNextEntry(new ZipEntry("version.json"));
			out.write("{\"name\":\"1.21.11\",\"pack_version\":{\"resource\":75}}".getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
			for (String path : List.of("gui/sprites/hud/heart/container.png", "gui/sprites/hud/heart/half.png",
					"gui/sprites/hud/heart/full.png", "gui/sprites/hud/food_empty.png", "gui/sprites/hud/food_half.png",
					"gui/sprites/hud/food_full.png", "gui/sprites/hud/air_bursting.png", "gui/sprites/hud/air.png",
					"gui/container/generic_54.png", "block/dirt.png", "block/cutout.png", "block/translucent.png", "item/mace.png")) {
				out.putNextEntry(new ZipEntry("assets/minecraft/textures/" + path));
				BufferedImage image = image(path.contains("generic_54") ? 256 : 16);
				if (path.equals("block/cutout.png"))
					image.setRGB(0, 0, 0);
				if (path.equals("block/translucent.png"))
					image.setRGB(0, 0, 0x80885533);
				ImageIO.write(image, "png", out);
				out.closeEntry();
			}
			Map<String, String> models = Map.of(
					"parent", """
							{"textures":{"all":{"sprite":"block/dirt","force_translucent":true},"side":"#all"},
							"elements":[{"from":[0,0,0],"to":[16,16,16],"faces":{"north":{"texture":"#side"}}}]}
							""",
					"inherited", "{\"parent\":\"block/parent\"}",
					"overridden", "{\"parent\":\"block/parent\",\"textures\":{\"all\":\"block/dirt\"}}");
			for (var entry : models.entrySet()) {
				out.putNextEntry(new ZipEntry("assets/minecraft/models/block/" + entry.getKey() + ".json"));
				out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
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
		Configuration config = new Configuration();
		config.setMinecraftJar(jar.toFile());
		textures = new TexturePack(rasteriser, config, List.of(), List.of());
		messages.start();
		loggers.forEach(logger -> logger.addAppender(messages));
	}

	@AfterEach
	void close() throws Exception {
		Minecraft.setChunkHeight(previousHeight);
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

	@ParameterizedTest
	@ValueSource(strings = {"#all", "all", "item/mace", "minecraft:item/mace"})
	void blockModelsCanUseItemTextures(String reference) throws Exception {
		BlockModel model = model("block/heavy_core", reference, Map.of("all", "item/mace"));
		var expected = textures.findTexture("assets/minecraft/textures/item/mace.png");

		assertTrue(model.getMissingTextures().isEmpty());
		assertTrue(messages.list.isEmpty());
		for (var face : model.getElements().getFirst().getFaces().values())
			assertSame(expected.texture, face.getTexture().texture);
	}

	@ParameterizedTest
	@ValueSource(strings = {"dirt", "cutout", "translucent"})
	void forcedTranslucencyAppliesPerEntryWithoutChangingSharedSprite(String sprite) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		var textureMap = BlockRegistry.resolveTextureMap(BlockRegistry.populateTextureMap(Map.of(), mapper.readTree("""
				{"forced":{"sprite":"block/%s","force_translucent":true}, "normal":"block/%s", "alias":"#forced"}
				""".formatted(sprite, sprite))));
		BlockModel model = new BlockModel("block/test", true, textureMap, mapper.readTree("""
				[{"from":[0,0,0],"to":[16,16,16],"faces":{
				"north":{"texture":"#alias"}, "south":{"texture":"#normal"}, "west":{"texture":"block/%s"}}}]
				""".formatted(sprite)), textures);
		var faces = model.getElements().getFirst().getFaces();
		boolean naturallyTranslucent = sprite.equals("translucent");
		assertTrue(faces.get("north").isTranslucent());
		assertEquals(naturallyTranslucent, faces.get("south").isTranslucent());
		assertEquals(naturallyTranslucent, faces.get("west").isTranslucent());
		assertSame(faces.get("north").getTexture().texture, faces.get("south").getTexture().texture);
		assertEquals(naturallyTranslucent, textures.getPackTexture("assets/minecraft/textures/block/" + sprite + ".png").isTranslucent());
		assertTrue(model.isTranslucent());
		assertFalse(model.isSolid());
		Map<MeshType, Integer> expected = naturallyTranslucent ? Map.of(MeshType.Transparent, 12)
				: Map.of(MeshType.Transparent, 4, sprite.equals("cutout") ? MeshType.AlphaTest : MeshType.Solid, 8);
		assertEquals(expected, renderVertices(model));
	}

	@ParameterizedTest
	@ValueSource(strings = {"\"block/%s\"", "{\"sprite\":\"block/%s\"}",
			"{\"sprite\":\"block/%s\",\"force_translucent\":false}"})
	void defaultAndFalseFlagsKeepPixelBasedPassSelection(String entry) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		for (String sprite : List.of("dirt", "cutout", "translucent")) {
			var textureMap = BlockRegistry.populateTextureMap(Map.of(), mapper.readTree("{\"all\":" + entry.formatted(sprite) + "}"));
			BlockModel model = new BlockModel("block/test", true, textureMap, mapper.readTree("""
					[{"from":[0,0,0],"to":[16,16,16],"faces":{"north":{"texture":"#all"}}}]
					"""), textures);
			MeshType expected = switch (sprite) {
				case "cutout" -> MeshType.AlphaTest;
				case "translucent" -> MeshType.Transparent;
				default -> MeshType.Solid;
			};
			assertEquals(Map.of(expected, 4), renderVertices(model));
			assertEquals(sprite.equals("dirt"), model.isSolid());
			assertEquals(sprite.equals("translucent"), model.isTranslucent());
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"parent", "inherited", "overridden"})
	void modelInheritancePreservesOrReplacesTranslucency(String name) throws Exception {
		BlockRegistry registry = new BlockRegistry();
		for (var entry : Map.of("zips", textures.getZipStack(), "texturePack", textures).entrySet()) {
			var field = BlockRegistry.class.getDeclaredField(entry.getKey());
			field.setAccessible(true);
			field.set(registry, entry.getValue());
		}
		BlockModel model = registry.loadModel("block/" + name);
		assertNotNull(model);
		assertEquals(Map.of(name.equals("overridden") ? MeshType.Solid : MeshType.Transparent, 4), renderVertices(model));
		assertEquals(name.equals("overridden"), model.isSolid());
		assertEquals(!name.equals("overridden"), model.isTranslucent());
	}

	private Map<MeshType, Integer> renderVertices(BlockModel model) {
		Map<MeshType, Integer> vertices = new EnumMap<>(MeshType.class);
		Mesh mesh = (Mesh) Proxy.newProxyInstance(Mesh.class.getClassLoader(), new Class<?>[]{Mesh.class},
				(proxy, method, args) -> {
					if (!method.getName().equals("addVertex"))
						throw new AssertionError("Unexpected mesh call: " + method.getName());
					return null;
				});
		Rasteriser rasteriser = (Rasteriser) Proxy.newProxyInstance(Rasteriser.class.getClassLoader(), new Class<?>[]{Rasteriser.class},
				(proxy, method, args) -> mesh);
		Geometry geometry = new Geometry(rasteriser) {
			@Override
			public Mesh getMesh(Texture texture, MeshType type) {
				vertices.merge(type, 4, Integer::sum);
				return mesh;
			}
		};
		BlockStateWrapper self = new BlockStateWrapper("minecraft:test");
		BlockStateWrapper air = new BlockStateWrapper("minecraft:air");
		air.setFullBlock(false);
		air.setFullOpaqueBlock(false);
		BlockContext world = (BlockContext) Proxy.newProxyInstance(BlockContext.class.getClassLoader(), new Class<?>[]{BlockContext.class},
				(proxy, method, args) -> switch (method.getName()) {
					case "getBlock" -> (int) args[1] == 0 && (int) args[2] == 0 && (int) args[3] == 0 ? self : air;
					case "getLightStyle" -> LightStyle.Day;
					case "isSmoothLit", "shouldCullSameBlockFaces" -> false;
					case "getNightLightAdjustment" -> 0.0f;
					case "getLight" -> 1.0f;
					default -> throw new AssertionError("Unexpected world call: " + method.getName());
				});
		new BakedBlockStateModel(model, 0, 0, 0, false).createGeometry(0, 0, 0, world, new RawChunk(), geometry);
		return vertices;
	}

	private BlockModel model(String name, String reference, Map<String, String> textureMap) throws Exception {
		String elements = """
				[{"from":[0,0,0],"to":[16,16,16],"faces":{
				"north":{"texture":"%s"},"south":{"texture":"%s"},"west":{"texture":"%s"}}}]
				""".formatted(reference, reference, reference);
		Map<String, ModelTexture> modelTextures = new HashMap<>();
		textureMap.forEach((key, value) -> modelTextures.put(key, new ModelTexture(value)));
		return new BlockModel(name, true, modelTextures, new ObjectMapper().readTree(elements), textures);
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
