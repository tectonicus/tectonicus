package tectonicus.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.texture.ZipStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class SignBackgroundTest {
	@TempDir Path directory;

	@Test
	void cropsModernBoardsAtPackResolutionAndSharesStandingAndWallTextures() throws Exception {
		Path jar = directory.resolve("game.jar");
		Path pack = directory.resolve("pack.zip");
		String spruce = "assets/minecraft/textures/block/spruce_sign.png";
		try (var zip = new ZipOutputStream(Files.newOutputStream(jar))) {
			addSheet(zip, spruce, 32, 32, 0, 2);
			addSheet(zip, "assets/minecraft/textures/block/warped_hanging_sign.png", 32, 32, 2, 16);
		}
		try (var zip = new ZipOutputStream(Files.newOutputStream(pack))) {
			addSheet(zip, spruce, 64, 64, 0, 4);
		}
		try (var assets = new ZipStack(jar.toFile(), List.of(pack.toFile()), List.of())) {
			var spruceBoard = SignBackground.forBlock("minecraft:spruce_wall_sign");
			assertEquals(spruceBoard, SignBackground.forBlock("minecraft:spruce_sign"));
			assertBoard(spruceBoard, assets, true, 48, 24);
			var warpedBoard = SignBackground.forBlock("minecraft:warped_wall_hanging_sign");
			assertEquals(warpedBoard, SignBackground.forBlock("minecraft:warped_hanging_sign"));
			assertBoard(warpedBoard, assets, true, 14, 10);
		}
	}

	@Test
	void cropsClassicSignSheetsAndLooksUpLegacyOak() throws Exception {
		Path jar = directory.resolve("classic.jar");
		try (var zip = new ZipOutputStream(Files.newOutputStream(jar))) {
			addSheet(zip, "item/sign.png", 64, 32, 2, 2);
			addSheet(zip, "assets/minecraft/textures/entity/signs/hanging/dark_oak.png", 64, 32, 2, 14);
		}
		try (var assets = new ZipStack(jar.toFile(), null, List.of())) {
			assertBoard(SignBackground.forBlock("Sign"), assets, false, 24, 12);
			assertEquals(SignBackground.forBlock("Sign"), SignBackground.forBlock("minecraft:wall_sign"));
			assertBoard(SignBackground.forBlock("minecraft:dark_oak_hanging_sign"), assets, false, 14, 10);
		}
	}

	private void assertBoard(SignBackground background, ZipStack assets, boolean usesBlockTextures, int width, int height) throws Exception {
		assertTrue(background.write(assets, directory, usesBlockTextures));
		var image = ImageIO.read(directory.resolve(background.filename()).toFile());
		assertEquals(width, image.getWidth());
		assertEquals(height, image.getHeight());
		assertEquals(0xff336699, image.getRGB(0, 0));
	}

	private void addSheet(ZipOutputStream zip, String path, int width, int height, int x, int y) throws Exception {
		var sheet = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		sheet.setRGB(x, y, 0xff336699);
		zip.putNextEntry(new ZipEntry(path));
		ImageIO.write(sheet, "png", zip);
		zip.closeEntry();
	}
}
