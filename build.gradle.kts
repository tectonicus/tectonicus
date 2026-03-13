import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.time.ZonedDateTime
import java.util.Properties

plugins {
	application
	id("com.gradleup.shadow") version "9.3.2"
	`maven-publish`
}

version = "2.30.2"

// Exclude unneeded tasks.
gradle.startParameter.excludedTaskNames.addAll(listOf("jar", "distTar", "distZip", "shadowDistTar"))

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}
}

application {
	mainClass.set("tectonicus.TectonicusApp")
}

repositories {
	mavenCentral()
}

dependencies {
	val lwjglVersion = "3.4.1"
	val picocliVersion = "4.7.7"
	val lombokVersion = "1.18.42"
	val junitVersion = "6.0.3"
	val caffeineVersion = "3.2.3"
	val commonsTextVersion = "1.15.0"
	val jomlVersion = "1.10.8"
	val joglVersion = "2.6.0"
	val jacksonVersion = "2.21.1"
	val logbackVersion = "1.5.32"
	val h2Version = "2.4.240"
	val webpImageIoVersion = "0.1.6"
	val hamcrestVersion = "3.0"

	implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

	implementation("org.lwjgl:lwjgl")
	implementation("org.lwjgl:lwjgl-opengl")
	implementation("org.lwjgl:lwjgl-egl")
	implementation("org.lwjgl:lwjgl-glfw")
	implementation("org.apache.commons:commons-text:$commonsTextVersion")
	implementation("org.joml:joml:$jomlVersion")
	implementation("info.picocli:picocli:$picocliVersion@jar")
	implementation("org.jogamp.jogl:jogl-all:$joglVersion")
	implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
	implementation("ch.qos.logback:logback-classic:$logbackVersion")
	implementation("com.h2database:h2-mvstore:$h2Version")
	implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")
	implementation("org.sejda.imageio:webp-imageio:$webpImageIoVersion")
	implementation(files("libs/jnbt.jar", "libs/BiomeExtractor.jar"))

	runtimeOnly("org.lwjgl:lwjgl::natives-windows")
	runtimeOnly("org.lwjgl:lwjgl-glfw::natives-windows")
	runtimeOnly("org.lwjgl:lwjgl-opengl::natives-windows")
    runtimeOnly("org.lwjgl:lwjgl::natives-windows-arm64")
    runtimeOnly("org.lwjgl:lwjgl-glfw::natives-windows-arm64")
    runtimeOnly("org.lwjgl:lwjgl-opengl::natives-windows-arm64")
	runtimeOnly("org.lwjgl:lwjgl::natives-macos")
	runtimeOnly("org.lwjgl:lwjgl-glfw::natives-macos")
	runtimeOnly("org.lwjgl:lwjgl-opengl::natives-macos")
    runtimeOnly("org.lwjgl:lwjgl::natives-macos-arm64")
    runtimeOnly("org.lwjgl:lwjgl-glfw::natives-macos-arm64")
    runtimeOnly("org.lwjgl:lwjgl-opengl::natives-macos-arm64")
	runtimeOnly("org.lwjgl:lwjgl::natives-linux")
	runtimeOnly("org.lwjgl:lwjgl-glfw::natives-linux")
	runtimeOnly("org.lwjgl:lwjgl-opengl::natives-linux")
    runtimeOnly("org.lwjgl:lwjgl::natives-linux-arm64")
    runtimeOnly("org.lwjgl:lwjgl-glfw::natives-linux-arm64")
    runtimeOnly("org.lwjgl:lwjgl-opengl::natives-linux-arm64")

	compileOnly("org.projectlombok:lombok:$lombokVersion")
	annotationProcessor("org.projectlombok:lombok:$lombokVersion")
	annotationProcessor("info.picocli:picocli-codegen:$picocliVersion")

	testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
	testImplementation("org.hamcrest:hamcrest:$hamcrestVersion")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testCompileOnly("org.projectlombok:lombok:$lombokVersion")
	testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")
}

tasks.named<ShadowJar>("shadowJar") {
	archiveClassifier.set("")

	doFirst {
		val gitDescribe = providers.exec {
			commandLine("git", "describe", "--tags", "--always")
		}.standardOutput.asText.get().trim()

		val props = Properties().apply {
			setProperty("buildDateTime", ZonedDateTime.now().toString())
			setProperty("buildNumber", gitDescribe)
			setProperty("version", version.toString())
		}

		file("build/resources/main/tectonicus.buildInfo").writer().use { writer ->
			props.store(writer, "Tectonicus build info")
		}
	}

	minimize {
		exclude(dependency("com.fasterxml.jackson.core:jackson-databind:.*"))
		exclude(dependency("com.github.ben-manes.caffeine:caffeine:.*"))
		exclude(dependency("org.apache.logging.log4j:log4j-core:.*"))
		exclude(dependency("ch.qos.logback:logback-classic:.*"))
		exclude(dependency("org.sejda.imageio:webp-imageio:.*"))
	}
}

distributions {
	named("shadow") {
		distributionBaseName.set(project.name)
		contents {
			from(files("src/main/resources/defaultBlockConfig.xml"))
			from("Docs") {
				include("*.xml", "*.txt")
				exclude("notes.txt")
			}
		}
	}
}

// This enables generatePomFileForReleasePublication for the GitHub dependency graph.
publishing {
	publications {
		create<MavenPublication>("release") {
			from(components["java"])
		}
	}
}

tasks.test {
	useJUnitPlatform()
}
