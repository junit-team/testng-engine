import java.util.EnumSet
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
    signing
    id("com.diffplug.spotless") version "5.12.0"
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(16))
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
    "6.14.3",
    "7.0.0",
    "7.1.0",
    "7.3.0",
    "7.4.0"
).map(::Version)
val snapshotTestNGVersion = Version("7.5-SNAPSHOT")

val allTestNGVersions = supportedTestNGVersions + listOf(snapshotTestNGVersion)

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
            require(supportedTestNGVersions.first().value)
            prefer(supportedTestNGVersions.last().value)
        }
    }

    compileOnly("org.testng:testng:${supportedTestNGVersions.last()}")
    testCompileOnly("org.testng:testng:${supportedTestNGVersions.last()}")
    testFixturesCompileOnly("org.testng:testng:${supportedTestNGVersions.last()}")

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
    testImplementation("org.mockito:mockito-junit-jupiter:3.11.2")
    testImplementation("org.apache.maven:maven-artifact:3.8.1") {
        because("ComparableVersion is used to reason about tested TestNG version")
    }

    testRuntimeOnly(platform("org.apache.logging.log4j:log4j-bom:2.14.1"))
    testRuntimeOnly("org.apache.logging.log4j:log4j-core")
    testRuntimeOnly("org.apache.logging.log4j:log4j-jul")

    testFixturesImplementation("junit:junit:4.13.2")
    testFixturesRuntimeOnly("org.junit.platform:junit-platform-console")
}

tasks {
    compileJava {
        options.release.set(8)
    }
    compileTestFixturesJava {
        options.release.set(8)
    }
    compileTestJava {
        options.release.set(16)
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
        val java8Launcher = project.the<JavaToolchainService>().launcherFor {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
        register<Test>("testFixturesTestNG_${version.suffix}") {
            javaLauncher.set(java8Launcher)
            classpath = configuration + sourceSets.testFixtures.get().output
            testClassesDirs = sourceSets.testFixtures.get().output
            useTestNG {
                listeners.add("example.listeners.SystemPropertyProvidingListener")
            }
        }
        register<Test>("testFixturesJUnitPlatform_${version.suffix}") {
            javaLauncher.set(java8Launcher)
            classpath = configuration + sourceSets.testFixtures.get().output
            testClassesDirs = sourceSets.testFixtures.get().output
            useJUnitPlatform {
                includeEngines("testng")
            }
            systemProperty("testng.listeners", "example.listeners.SystemPropertyProvidingListener")
            testLogging {
                events = EnumSet.allOf(TestLogEvent::class.java)
            }
        }
        register<JavaExec>("testFixturesConsoleLauncher_${version.suffix}") {
            javaLauncher.set(java8Launcher)
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
    sign(publishing.publications)
    isRequired = !project.version.toString().contains("SNAPSHOT")
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
