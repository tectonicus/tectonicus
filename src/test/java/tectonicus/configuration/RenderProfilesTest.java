package tectonicus.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class RenderProfilesTest {
	@TempDir Path directory;

	@Test void alphaProfilesSetMatchingOptionsWithDifferentBiomeColors() throws Exception {
		for (String name : new String[]{"alpha", "alpha1.2-beta1.2_02"}) {
			boolean enabled = !name.equals("alpha");
			var map = parse("<config useBiomeColors=\"" + !enabled + "\"/><map worldDir=\".\"><render profile=\"" + name + "\"/><layer/></map>").getDimensionConfig(0);
			assertEquals(enabled, map.useBiomeColours());
			assertEquals(enabled, map.getLayer(0).useBiomeColours());
			assertEquals(SwampWaterTint.NONE, map.getSwampWaterTint());
			assertTrue(map.isProceduralFluidTextures());
			assertTrue(map.useOldGrayscaleLighting());
			assertFalse(map.isSmoothLit());
			assertTrue(map.isBiomeBlendRadiusConfigured());
			assertEquals(0, map.getBiomeBlendRadius());
		}
	}

	@Test void beta13To173ProfileKeepsAlphaBiomeSettingsAndDefaultsToSmoothLighting() throws Exception {
		var map = parse("<map worldDir=\".\"><render profile=\"beta1.3-beta1.7.3\"/></map>").getDimensionConfig(0);
		assertTrue(map.useBiomeColours());
		assertEquals(SwampWaterTint.NONE, map.getSwampWaterTint());
		assertTrue(map.isProceduralFluidTextures());
		assertTrue(map.useOldGrayscaleLighting());
		assertTrue(map.isSmoothLit());
		assertTrue(map.isBiomeBlendRadiusConfigured());
		assertEquals(0, map.getBiomeBlendRadius());
		var inherited = parse("<config><render useSmoothLighting=\"false\"/></config><map worldDir=\".\"><render profile=\"beta1.3-beta1.7.3\"/></map>").getDimensionConfig(0);
		assertFalse(inherited.isSmoothLit(), "The profile must omit smooth lighting rather than force it on");
	}

	@Test void beta18ProfileUsesModernLightingDefaultsAndPreservesExplicitLighting() throws Exception {
		var map = parse("<map worldDir=\".\"><render profile=\"beta1.8\"/></map>").getDimensionConfig(0);
		assertTrue(map.useBiomeColours());
		assertEquals(SwampWaterTint.NONE, map.getSwampWaterTint());
		assertTrue(map.isProceduralFluidTextures());
		assertFalse(map.useOldGrayscaleLighting());
		assertTrue(map.isSmoothLit());
		assertTrue(map.isBiomeBlendRadiusConfigured());
		assertEquals(0, map.getBiomeBlendRadius());
		var inherited = parse("<config><render useSmoothLighting=\"false\" useOldGrayscaleLighting=\"true\"/></config><map worldDir=\".\"><render profile=\"beta1.8\"/></map>").getDimensionConfig(0);
		assertFalse(inherited.isSmoothLit());
		assertTrue(inherited.useOldGrayscaleLighting(), "The profile must preserve explicit lighting options");
	}

	@Test void minecraft10PresetSetsTheRequestedOptions() throws Exception {
		var map = parse("<map worldDir=\".\"><render profile=\"minecraft1.0\"/></map>").getDimensionConfig(0);
		assertEquals(SwampWaterTint.MINECRAFT10, map.getSwampWaterTint());
		assertTrue(map.isMinecraft10SwampColors());
		assertTrue(map.isProceduralFluidTextures());
		assertTrue(map.isBiomeBlendRadiusConfigured());
		assertEquals(0, map.getBiomeBlendRadius());
		assertTrue(map.isSmoothLit());
	}

	@Test void minecraft11To14PresetSetsTheRequestedOptions() throws Exception {
		var map = parse("<map worldDir=\".\"><render profile=\"minecraft1.1-1.4\"/></map>").getDimensionConfig(0);
		assertEquals(SwampWaterTint.LEGACY, map.getSwampWaterTint());
		assertTrue(map.isProceduralFluidTextures());
		assertFalse(map.useOldGrayscaleLighting());
		assertFalse(map.isMinecraft10SwampColors());
		assertFalse(map.isBiomeBlendRadiusConfigured());
	}

	@Test void localOptionsAndChildrenOverrideProfilesWithoutLeakingToSiblings() throws Exception {
		var config = parse("""
			<config><render profile="minecraft1.0" biomeBlendRadius="3"/></config>
			<world worldDir=".">
			  <dimension biomeBlendRadius="4"><render profile="minecraft1.0"/>
			    <layer/><layer><render swampWaterTint="none" useProceduralFluidTextures="false"/></layer>
			  </dimension>
			  <dimension><layer/></dimension>
			</world>
			""");
		var first = config.getDimensionConfig(0);
		assertEquals(4, first.getBiomeBlendRadius());
		assertEquals(4, first.getLayer(0).getBiomeBlendRadius());
		assertEquals(SwampWaterTint.MINECRAFT10, first.getLayer(0).getSwampWaterTint());
		assertTrue(first.getLayer(0).isProceduralFluidTextures());
		assertEquals(SwampWaterTint.NONE, first.getLayer(1).getSwampWaterTint());
		assertFalse(first.getLayer(1).isProceduralFluidTextures());
		assertEquals(3, config.getDimensionConfig(1).getLayer(0).getBiomeBlendRadius());
	}

	@Test void customProfileOverridesInheritedValuesAndKeepsUnspecifiedOptions() throws Exception {
		var config = parse("""
			<renderProfiles>
			  <profile name="beta" swampWaterTint="none" minecraft10SwampColors="false" biomeBlendRadius="0"/>
			</renderProfiles>
			<config><render profile="minecraft1.0" biomeBlendRadius="3"/></config>
			<map worldDir="."><render profile=" BETA " biomeBlendRadius="1"/><layer/></map>
			<map worldDir="."/>
			""");
		var beta = config.getDimensionConfig(0).getLayer(0);
		assertEquals(SwampWaterTint.NONE, beta.getSwampWaterTint());
		assertFalse(beta.isMinecraft10SwampColors());
		assertTrue(beta.isProceduralFluidTextures());
		assertEquals(1, beta.getBiomeBlendRadius());
		assertEquals(SwampWaterTint.MINECRAFT10, config.getDimensionConfig(1).getSwampWaterTint());
	}

	@Test void profilesDoNotCarryBetweenConfigurationFiles() throws Exception {
		parse("<renderProfiles><profile name=\"custom\" legacyWater=\"true\"/></renderProfiles><map worldDir=\".\"/>");
		assertThrows(IllegalArgumentException.class,
				() -> parse("<map worldDir=\".\"><render profile=\"custom\"/></map>"));
	}

	@Test void rejectsUnknownProfilesDuplicatesAndInvalidDefinitions() {
		for (String xml : new String[]{
				"<map worldDir=\".\"><render profile=\"missing\"/></map>",
				"<renderProfiles><profile name=\"minecraft1.0\"/></renderProfiles>",
				"<renderProfiles><profile name=\"x\"/><profile name=\"X\"/></renderProfiles>",
				"<renderProfiles><profile name=\"x\" biomeBlendRadus=\"1\"/></renderProfiles>",
				"<renderProfiles><profile/></renderProfiles>",
				"<renderProfiles><profile name=\"x\"><render/></profile></renderProfiles>"}) {
			assertThrows(IllegalArgumentException.class, () -> parse(xml), xml);
		}
	}

	private Configuration parse(String xml) throws Exception {
		Path file = directory.resolve("config.xml");
		Files.writeString(file, "<tectonicus version=\"2\">" + xml + "</tectonicus>");
		return XmlConfigurationParser.parseConfiguration(file.toFile());
	}
}
