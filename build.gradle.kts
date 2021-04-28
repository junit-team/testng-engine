import org.gradle.api.tasks.PathSensitivity.RELATIVE

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
    id("com.diffplug.spotless") version "5.12.0"
    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(16))
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
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
        "6.9.13.6",
        "6.10",
        "6.11",
        "6.13.1",
        "6.14.3",
        "7.0.0",
        "7.1.0",
        "7.3.0",
        "7.4.0"
)

val testRuntimeClasspath: Configuration by configurations.getting
val supportedTestNGConfigurationsByVersion = supportedTestNGVersions.associateWith { version ->
    configurations.create("testng_${version.replace('.', '_')}") {
        extendsFrom(testRuntimeClasspath)
    }
}

dependencies {
    api(platform("org.junit:junit-bom:5.7.1"))
    api("org.junit.platform:junit-platform-engine")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-testkit")
    testImplementation("org.apache.maven:maven-artifact:3.8.1")
    testFixturesCompileOnly("org.testng:testng:${supportedTestNGVersions.last()}")
    implementation("org.testng:testng") {
        version {
            prefer(supportedTestNGVersions.last())
        }
    }
    constraints {
        supportedTestNGConfigurationsByVersion.forEach { (version, configuration) ->
            configuration("org.testng:testng") {
                version {
                    strictly(version)
                }
            }
        }
    }
}

tasks {
    compileJava {
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
            listOf("--patch-module", "org.junit.compat.testng.engine=${files.asPath}")
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
    val testTasks = supportedTestNGConfigurationsByVersion.map { (version, configuration) ->
        register<Test>("test_${version.replace('.', '_')}") {
            classpath -= testRuntimeClasspath
            classpath += configuration
            useJUnitPlatform {
                includeEngines("junit-jupiter")
            }
            systemProperty("testng.version", version)
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
            pom {
                name.set(project.description)
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
