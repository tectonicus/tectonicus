package tectonicus.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class BiomeColorConfigurationTest {
	@TempDir Path directory;

	@ParameterizedTest @ValueSource(strings = {"useBiomeColours", "useBiomeColors"})
	void oldConfigAndMapAttributesStillWork(String attribute) throws Exception {
		var config = parse("<config " + attribute + "=\"false\"/><map worldDir=\".\"><layer/></map><map worldDir=\".\" " + attribute + "=\"true\"/>");
		assertFalse(config.getDimensionConfig(0).useBiomeColours());
		assertFalse(config.getDimensionConfig(0).getLayer(0).useBiomeColours());
		assertTrue(config.getDimensionConfig(1).useBiomeColours());
	}

	@Test void inheritsBothSpellingsAndOverridesPerLayer() throws Exception {
		var config = parse("""
			<config useBiomeColours="false"/>
			<world worldDir="."><render useBiomeColors="true"/>
			  <dimension><render useBiomeColours="false"/>
			    <layer/><layer><render useBiomeColors="true"/></layer>
			  </dimension>
			  <dimension><layer/></dimension>
			</world>
			<map worldDir="."><layer/></map>
			""");
		assertTrue(config.getWorlds().getFirst().useBiomeColours());
		assertFalse(config.getDimensionConfig(0).getLayer(0).useBiomeColours());
		assertTrue(config.getDimensionConfig(0).getLayer(1).useBiomeColours());
		assertTrue(config.getDimensionConfig(1).getLayer(0).useBiomeColours());
		assertFalse(config.getDimensionConfig(2).getLayer(0).useBiomeColours());
	}

	@Test void renderBeatsOldScopeAttributeAndCanonicalSpellingWinsWithinOneElement() throws Exception {
		var config = parse("""
			<map worldDir="." useBiomeColours="false"><render useBiomeColors="true"/></map>
			<map worldDir="."><render useBiomeColours="false" useBiomeColors="true"/></map>
			""");
		assertTrue(config.getDimensionConfig(0).useBiomeColours());
		assertFalse(config.getDimensionConfig(1).useBiomeColours());
	}

	@Test void profilesAcceptAliasAndExplicitFalseOverridesProfile() throws Exception {
		var config = parse("""
			<renderProfiles><profile name="plain" useBiomeColors="false"/></renderProfiles>
			<map worldDir="."><render profile="plain"/></map>
			<map worldDir="."><render profile="plain" useBiomeColours="true"/></map>
			<map worldDir="."/>
			""");
		assertFalse(config.getDimensionConfig(0).useBiomeColours());
		assertTrue(config.getDimensionConfig(1).useBiomeColours());
		assertTrue(config.getDimensionConfig(2).useBiomeColours());
	}

	private Configuration parse(String xml) throws Exception {
		Path file = directory.resolve("config.xml");
		Files.writeString(file, "<tectonicus version=\"2\">" + xml + "</tectonicus>");
		return XmlConfigurationParser.parseConfiguration(file.toFile());
	}
}
