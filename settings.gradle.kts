plugins {
    id("com.gradle.develocity") version "4.2.2"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "2.4.0"
}

rootProject.name = "testng-engine"

develocity {
    buildScan {
        val isCiServer = System.getenv("CI") != null

        server = "https://ge.junit.org"
        uploadInBackground = !isCiServer

        obfuscation {
            if (isCiServer) {
                username { "github" }
            } else {
                hostname { null }
                ipAddresses { emptyList() }
            }
        }

        publishing.onlyIf { it.isAuthenticated }
    }
}

buildCache {
    val isCiServer = System.getenv("CI") != null
    local {
        isEnabled = !isCiServer
    }
    remote(develocity.buildCache) {
        val authenticated = !System.getenv("DEVELOCITY_ACCESS_KEY").isNullOrEmpty()
        isPush = isCiServer && authenticated
    }
}
