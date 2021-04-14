plugins {
    `java-library`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(16))
}

repositories {
    mavenCentral()
}

val moduleSourceSet = sourceSets.create("module")

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
    }
    jar {
        from(moduleSourceSet.output)
    }
    val testTasks = supportedTestNGConfigurationsByVersion.map { (version, configuration) ->
        register<Test>("test_${version.replace('.', '_')}") {
            classpath -= testRuntimeClasspath
            classpath += configuration
            useJUnitPlatform()
            systemProperty("testng.version", version)
        }
    }
    test {
        enabled = false
        dependsOn(testTasks)
    }
}
