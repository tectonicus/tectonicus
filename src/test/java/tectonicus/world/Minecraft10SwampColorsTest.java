package tectonicus.world;

import org.junit.jupiter.api.Test;
import tectonicus.util.Colour4f;
import static org.junit.jupiter.api.Assertions.*;

class Minecraft10SwampColorsTest {
	@Test void mixesPaletteWithSwampTintUsingIntegerRounding() {
		assertEquals(0x270727, Minecraft10SwampColors.plant(new Colour4f(0, 0, 0)).toRgb());
		assertEquals(0xA686A6, Minecraft10SwampColors.plant(new Colour4f(255, 255, 255)).toRgb());
	}
}
