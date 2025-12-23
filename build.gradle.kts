import com.diffplug.spotless.LineEnding
import com.gradle.develocity.agent.gradle.test.PredictiveTestSelectionMode.RELEVANT_TESTS
import com.gradle.develocity.agent.gradle.test.PredictiveTestSelectionMode.REMAINING_TESTS
import java.util.EnumSet
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
    signing
    id("com.diffplug.spotless") version "8.1.0"
    id("com.gradleup.nmcp") version "1.4.0"
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
    maven(url = "https://jitpack.io") {
        mavenContent {
            includeModule("com.github.testng-team", "testng")
        }
    }
    maven(url = "https://central.sonatype.com/repository/maven-snapshots") {
        mavenContent {
            includeGroupByRegex("org\\.junit.*")
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
    "7.8.0" to 11,
    "7.9.0" to 11,
    "7.10.2" to 11,
    libs.versions.latestTestNG.get() to 11,
).associateBy({ Version(it.first) }, { JavaLanguageVersion.of(it.second) })

val lastJdk8CompatibleRelease = supportedTestNGVersions.entries.last { it.value == JavaLanguageVersion.of(8) }.key

val snapshotTestNGVersion = Version(libs.versions.snapshotTestNG.get())

val allTestNGVersions = supportedTestNGVersions.keys + listOf(snapshotTestNGVersion)

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
    api(platform(libs.junit.bom))
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
        (testNGTestConfigurationsByVersion.asSequence() + testNGTestFixturesConfigurationsByVersion.asSequence()).forEach { (version, configuration) ->
            if (version.isSnapshot()) {
                configuration.resolutionStrategy.dependencySubstitution {
                    substitute(module("org.testng:testng"))
                        .using(module("com.github.testng-team:testng:master-SNAPSHOT"))
                }
            } else {
                configuration("org.testng:testng") {
                    version {
                        strictly(version.value)
                    }
                }
            }
        }
    }

    testImplementation(libs.versions.latestJUnit.map { "org.junit.jupiter:junit-jupiter:$it" })
    testImplementation("org.junit.platform:junit-platform-testkit")
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.maven.artifact) {
        because("ComparableVersion is used to reason about tested TestNG version")
    }
    testImplementation(libs.commons.lang3)

    testRuntimeOnly(platform(libs.log4j.bom))
    testRuntimeOnly("org.apache.logging.log4j:log4j-core")
    testRuntimeOnly("org.apache.logging.log4j:log4j-jul")

    testFixturesImplementation(libs.junit4)
    testFixturesImplementation(libs.versions.latestJUnit.map { "org.junit.platform:junit-platform-engine:$it" })
    testFixturesRuntimeOnly("org.junit.platform:junit-platform-console")
}

tasks {
    compileJava {
        options.release.set(8)
        if (javaToolchainVersion >= JavaLanguageVersion.of(20)) {
            // `--release=8` is deprecated on JDK 20 and later
            options.compilerArgs.add("-Xlint:-options")
        }
    }
    listOf(compileTestJava, compileTestFixturesJava).forEach { task ->
        task.configure {
            options.release.set(17)
        }
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
            testClassesDirs = files(testing.suites.named<JvmTestSuite>("test").map { it.sources.output.classesDirs })
            group = JavaBasePlugin.VERIFICATION_GROUP
            useJUnitPlatform {
                includeEngines("junit-jupiter")
            }
            systemProperty("testng.version", version.value)
            systemProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
        }
    }
    withType<Test>().configureEach {
        develocity {
            predictiveTestSelection {
                enabled = true
                mode = providers.gradleProperty("junit.develocity.predictiveTestSelection.selectRemainingTests")
                    .map { it.toBoolean() }
                    .orElse(false)
                    .map { if (it) REMAINING_TESTS else RELEVANT_TESTS }
            }
        }
    }
    test {
        enabled = false
        develocity.predictiveTestSelection.enabled = false
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
    repositories {
        maven {
            name = "mavenCentralSnapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            credentials {
                username = providers.gradleProperty("mavenCentralUsername").orNull
                password = providers.gradleProperty("mavenCentralPassword").orNull
            }
        }
    }
}

nmcp {
    publishAllPublicationsToCentralPortal {
        username = providers.gradleProperty("mavenCentralUsername")
        password = providers.gradleProperty("mavenCentralPassword")
        publishingType = providers.gradleProperty("mavenCentralPublishingType").orElse("USER_MANAGED")
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

    fun isSnapshot() : Boolean = value.endsWith("-SNAPSHOT")

    override fun toString() = value
}
