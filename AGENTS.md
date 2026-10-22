# Repository Guidelines

## Project Description
Tectonicus renders Minecraft Java worlds into detailed, zoomable maps that can be viewed in a browser. It is a Java application built with Gradle, uses LWJGL for OpenGL rendering, reads XML configuration files, and includes a JavaScript/Leaflet-based map viewer. It reads block-state and model JSON and textures directly from the Minecraft JAR and any configured resource pack. Tectonicus uses GLFW to create rendering windows and OpenGL contexts and to handle input and events. The renderer requests compatibility contexts: OpenGL 2.1 on macOS, OpenGL 3.2 on other operating systems, and OpenGL 3.0 for EGL offscreen rendering. Rendering still relies on legacy fixed-function OpenGL.

## Project Structure & Module Organization
Main code lives in `src/main/java/tectonicus`, organized by domain packages such as `renderer`, `world`, `raw`, and `configuration`. The application entry point is `tectonicus.TectonicusApp`. Runtime assets and web map resources are in `src/main/resources` (for example `defaultBlockConfig*.xml`, block-state/model JSON, JS/CSS, and images). Configuration examples and the usage guide are in `Docs/`.

Tests are in `src/test/java/tectonicus` with matching package structure, and test fixtures are in `src/test/resources`. Generated or build outputs go to `build/` and should not be committed. Use the Gradle wrapper (`gradlew`/`gradlew.bat`) for builds. The project configures a Java toolchain; install a compatible JDK to build locally.

## Build, Test, and Development Commands
- `.\gradlew.bat clean test` runs unit tests on JUnit Jupiter.
- `.\gradlew.bat build` runs the full Gradle build, matching the build task used in CI. This will also run the shadowJar task which builds the distributable fat JAR in `build/libs/`.
- `.\gradlew.bat run --args="-c tectonicus.xml"` runs the app from source with a config file.
- `java -jar build/libs/Tectonicus-<version>.jar -c tectonicus.xml` runs the packaged artifact.

Use the Gradle wrapper (`gradlew`/`gradlew.bat`) instead of a system Gradle install.

## Coding Style & Naming Conventions
Refer to `.editorconfig` for basic formatting conventions, and follow existing Java style in this repository:
- Braces on the same line for class/method declarations, K&R style.
- `UpperCamelCase` for classes, `lowerCamelCase` for fields/methods, `UPPER_SNAKE_CASE` for constants.
- Keep packages under `tectonicus.*` and place new code in the closest domain package.

Use Lombok where possible to reduce boilerplate, following existing usage patterns.

## Testing Guidelines
Use JUnit Jupiter (`org.junit.jupiter`). Add tests under mirrored package paths in `src/test/java`. Name test classes `*Test` or `*Tests` (both patterns exist here). Keep fixtures in `src/test/resources` and prefer deterministic canned inputs over external dependencies.

Run `.\gradlew.bat test` for focused verification, or `.\gradlew.bat build` to match CI before opening a PR.

## Commit & Pull Request Guidelines
Recent commits favor short, imperative summaries (for example: `Fix ...`, `Add support for ...`) without strict prefixing. Keep commit messages specific to behavior changes.

PRs should include:
- Clear description of what changed and why.
- Linked issue(s) when applicable.
- Notes on Minecraft/version compatibility impacts.
- Evidence of verification (`.\gradlew.bat test`, and screenshots/map output samples for rendering/UI changes).
