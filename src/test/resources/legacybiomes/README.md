# Legacy biome integration tests

`level.dat` is the small seed-only fixture from
MinecraftBiomeExtractor's `testdata/World/world/level.dat`. Tests synthesize empty
chunks around it, or ask the extractor directly for climate coordinates. Minecraft
JARs and textures are supplied locally and are not redistributed with these tests.

To run the optional tests, set these environment variables to local client JARs:

- `MINECRAFT_ALPHA_120_JAR`: Alpha v1.2.0_02 (launcher entry `a1.2.0`).
- `MINECRAFT_BETA_13_JAR`: Beta 1.3_01.
- `MINECRAFT_BETA_181_JAR`: Beta 1.8.1.
- `MINECRAFT_1_0_JAR`: release 1.0.

Then run `./gradlew.bat test`. Cases without a configured JAR are skipped. Gradle
tracks the configured paths and JAR contents as test inputs. Tests never download
Minecraft. Keep the JARs outside the checkout (for example in your launcher cache),
or in the ignored `testdata` directory. Do not add them to CI artifacts.

Coverage includes generator parity at negative coordinates, persisted-cache reuse,
corrupt-cache recovery, seed invalidation, unsigned color coordinates, grass and
foliage tint lookup, blending across chunk edges, saved-biome precedence, and
disabling extraction for modern saves and other dimensions.
