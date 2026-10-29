package tectonicus.view;

import org.joml.Vector3f;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.configuration.ViewConfig;
import tectonicus.raw.SignEntity;
import tectonicus.world.Sign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewUtilTest {
	@ParameterizedTest
	@ValueSource(strings = {"#view", "#view f0", "#view f29", "#view f111"})
	void cameraFallsBackToConfiguredFov(String settings) {
		ViewConfig config = new ViewConfig();
		config.setFOV(85);
		Vector3f[] actual = ViewUtil.createCamera(null, ViewUtil.findView(sign(settings)), config).getFrustumVertices();
		Vector3f[] expected = ViewUtil.createCamera(null, ViewUtil.findView(sign("#view f85")), config).getFrustumVertices();
		for (int i = 0; i < actual.length; i++) {
			assertTrue(actual[i].isFinite(), "Camera frustum must contain finite coordinates");
			assertEquals(expected[i], actual[i]);
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {30, 70, 110})
	void validSignFovOverridesConfiguration(int fov) {
		ViewConfig config = new ViewConfig();
		ViewUtil.Viewpoint view = ViewUtil.findView(sign("#view f" + fov));
		Vector3f[] actual = ViewUtil.createCamera(null, view, config).getFrustumVertices();
		config.setFOV(fov);
		Vector3f[] expected = ViewUtil.createCamera(null, ViewUtil.findView(sign("#view")), config).getFrustumVertices();
		for (int i = 0; i < actual.length; i++) {
			assertTrue(actual[i].isFinite());
			assertEquals(expected[i], actual[i]);
		}
	}

	private Sign sign(String settings) {
		return new Sign(new SignEntity(0, 64, 0, 0, 64, 0, settings, "", "", "", 0, null, "black"));
	}
}
