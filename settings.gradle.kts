plugins {
    id("com.gradle.develocity") version "3.17.2"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "2.0.1"
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
