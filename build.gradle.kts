import com.diffplug.spotless.LineEnding
import java.util.EnumSet
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
    signing
    id("com.diffplug.spotless") version "6.23.3"
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

val javaToolchainVersion = providers.gradleProperty("javaToolchainVersion")
    .map { JavaLanguageVersion.of(it) }
    .orElse(JavaLanguageVersion.of(17))
    .get()

java {
    toolchain.languageVersion.set(javaToolchainVersion)
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/") {
        mavenContent {
            includeModule("org.testng", "testng")
            snapshotsOnly()
        }
    }
}

val moduleSourceSet = sourceSets.create("module") {
    compileClasspath += sourceSets.main.get().output
}

configurations {
    named(moduleSourceSet.compileClasspathConfigurationName) {
        extendsFrom(compileClasspath.get())
    }
}

val supportedTestNGVersions = listOf(
    "6.14.3" to 8,
    "7.0.0" to 8,
    "7.1.0" to 8,
    "7.3.0" to 8,
    "7.4.0" to 8,
    "7.5.1" to 8,
    "7.6.1" to 11,
    "7.7.1" to 11,
    "7.8.0" to 11 // Keep in sync with TestContext.java and README.MD
).associateBy({ Version(it.first) }, { JavaLanguageVersion.of(it.second) })

val lastJdk8CompatibleRelease = supportedTestNGVersions.entries.last { it.value == JavaLanguageVersion.of(8) }.key

val snapshotTestNGVersion = Version("7.9.0-SNAPSHOT")

val allTestNGVersions = supportedTestNGVersions.keys + listOf(snapshotTestNGVersion)

fun versionSuffix(version: String) =
    if (version.endsWith("-SNAPSHOT")) "snapshot" else version.replace('.', '_')

val testRuntimeClasspath: Configuration by configurations.getting
val testNGTestConfigurationsByVersion = allTestNGVersions.associateWith { version ->
    configurations.create("testRuntimeClasspath_${version.suffix}") {
        extendsFrom(testRuntimeClasspath)
    }
}
val testFixturesRuntimeClasspath: Configuration by configurations.getting
val testNGTestFixturesConfigurationsByVersion = allTestNGVersions.associateWith { version ->
    configurations.create("testFixturesRuntimeClasspath_${version.suffix}") {
        extendsFrom(testFixturesRuntimeClasspath)
    }
}

dependencies {
    api(platform("org.junit:junit-bom:5.7.2"))
    api("org.junit.platform:junit-platform-engine")

    implementation("org.testng:testng") {
        version {
            require(supportedTestNGVersions.keys.first().value)
            prefer(supportedTestNGVersions.keys.last().value)
        }
    }

    compileOnly("org.testng:testng:${lastJdk8CompatibleRelease}")
    testCompileOnly("org.testng:testng:${lastJdk8CompatibleRelease}")
    testFixturesCompileOnly("org.testng:testng:${lastJdk8CompatibleRelease}")

    constraints {
        testNGTestConfigurationsByVersion.forEach { (version, configuration) ->
            configuration("org.testng:testng") {
                version {
                    strictly(version.value)
                }
            }
        }
        testNGTestFixturesConfigurationsByVersion.forEach { (version, configuration) ->
            configuration("org.testng:testng") {
                version {
                    strictly(version.value)
                }
            }
        }
    }

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-testkit")
    testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.apache.maven:maven-artifact:3.9.6") {
        because("ComparableVersion is used to reason about tested TestNG version")
    }

    testRuntimeOnly(platform("org.apache.logging.log4j:log4j-bom:2.22.1"))
    testRuntimeOnly("org.apache.logging.log4j:log4j-core")
    testRuntimeOnly("org.apache.logging.log4j:log4j-jul")

    testFixturesImplementation("junit:junit:4.13.2")
    testFixturesRuntimeOnly("org.junit.platform:junit-platform-console")
}

tasks {
    listOf(compileJava, compileTestFixturesJava).forEach { task ->
        task.configure {
            options.release.set(8)
            if (javaToolchainVersion >= JavaLanguageVersion.of(20)) {
                // `--release=8` is deprecated on JDK 20 and later
                options.compilerArgs.add("-Xlint:-options")
            }
        }
    }
    compileTestJava {
        options.release.set(17)
    }
    named<JavaCompile>(moduleSourceSet.compileJavaTaskName) {
        options.release.set(9)
        options.compilerArgs.addAll(listOf("--module-version", "${project.version}"))
        val files = files(sourceSets.main.map { it.java.srcDirs })
        inputs.files(files).withPropertyName("mainSrcDirs").withPathSensitivity(RELATIVE)
        options.compilerArgumentProviders += CommandLineArgumentProvider {
            listOf("--patch-module", "org.junit.support.testng.engine=${files.asPath}")
        }
    }
    withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf("-Xlint:all,-requires-automatic", "-Werror"))
    }
    jar {
        from(moduleSourceSet.output) {
            include("module-info.class")
        }
    }
    withType<Jar>().configureEach {
        from(rootDir) {
            include("LICENSE.md")
            into("META-INF")
        }
    }
    testNGTestFixturesConfigurationsByVersion.forEach { (version, configuration) ->
        val javaMinVersionLauncher = project.the<JavaToolchainService>().launcherFor {
            languageVersion.set(supportedTestNGVersions[version])
        }
        register<Test>("testFixturesTestNG_${version.suffix}") {
            javaLauncher.set(javaMinVersionLauncher)
            classpath = configuration + sourceSets.testFixtures.get().output
            testClassesDirs = sourceSets.testFixtures.get().output
            useTestNG {
                listeners.add("example.configuration.parameters.SystemPropertyProvidingListener")
            }
        }
        register<Test>("testFixturesJUnitPlatform_${version.suffix}") {
            javaLauncher.set(javaMinVersionLauncher)
            classpath = configuration + sourceSets.testFixtures.get().output
            testClassesDirs = sourceSets.testFixtures.get().output
            useJUnitPlatform {
                includeEngines("testng")
            }
            systemProperty("testng.listeners", "example.configuration.parameters.SystemPropertyProvidingListener")
            testLogging {
                events = EnumSet.allOf(TestLogEvent::class.java)
            }
        }
        register<JavaExec>("testFixturesConsoleLauncher_${version.suffix}") {
            javaLauncher.set(javaMinVersionLauncher)
            classpath = configuration + sourceSets.testFixtures.get().output
            mainClass.set("org.junit.platform.console.ConsoleLauncher")
            args(
                "--scan-classpath", sourceSets.testFixtures.get().output.asPath,
                "--fail-if-no-tests",
                "--disable-banner",
                "--include-classname", ".*TestCase",
                "--exclude-package", "example.configuration",
                "--include-engine", "testng"
            )
            isIgnoreExitValue = true
        }
    }
    val testTasks = testNGTestConfigurationsByVersion.map { (version, configuration) ->
        register<Test>("test_${version.suffix}") {
            classpath = configuration + sourceSets.test.get().output
            group = JavaBasePlugin.VERIFICATION_GROUP
            useJUnitPlatform {
                includeEngines("junit-jupiter")
            }
            systemProperty("testng.version", version.value)
            systemProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
        }
    }
    test {
        enabled = false
        dependsOn(testTasks)
    }
}

spotless {
    val licenseHeaderFile = file("gradle/spotless/eclipse-public-license-2.0.java")
    java {
        licenseHeaderFile(licenseHeaderFile)
        importOrderFile(rootProject.file("gradle/spotless/junit-eclipse.importorder"))
        eclipse().configFile(rootProject.file("gradle/spotless/junit-eclipse-formatter-settings.xml"))
        if (!JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_15)) {
            // Doesn't work with Java 15 text blocks, see https://github.com/diffplug/spotless/issues/713
            removeUnusedImports()
        }
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("javaMisc") {
        target("src/**/package-info.java", "src/**/module-info.java")
        licenseHeaderFile(licenseHeaderFile, "/\\*\\*")
    }
    // https://github.com/diffplug/spotless/issues/1644
    lineEndings = LineEnding.PLATFORM_NATIVE
}

nexusPublishing {
    packageGroup.set("org.junit")
    repositories {
        sonatype()
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"].apply {
                this as AdhocComponentWithVariants
                withVariantsFromConfiguration(configurations.testFixturesApiElements.get()) { skip() }
                withVariantsFromConfiguration(configurations.testFixturesRuntimeElements.get()) { skip() }
            })
            pom {
                name.set("TestNG Engine for the JUnit Platform")
                description.set(project.description)
                url.set("https://junit.org/junit5/")
                scm {
                    connection.set("scm:git:git://github.com/junit-team/testng-engine.git")
                    developerConnection.set("scm:git:git://github.com/junit-team/testng-engine.git")
                    url.set("https://github.com/junit-team/testng-engine")
                }
                licenses {
                    license {
                        name.set("Eclipse Public License v2.0")
                        url.set("https://www.eclipse.org/legal/epl-v20.html")
                    }
                }
                developers {
                    developer {
                        id.set("junit-team")
                        name.set("JUnit team")
                        email.set("team@junit.org")
                    }
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

tasks.withType<Sign>().configureEach {
    enabled = !project.version.toString().contains("SNAPSHOT")
}

data class Version(val value: String) {

    companion object {
        private val pattern = "[^\\w]".toRegex()
    }

    val suffix: String by lazy {
        value.replace(pattern, "_")
    }

    override fun toString() = value
}
