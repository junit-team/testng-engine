plugins {
    id("com.gradle.develocity") version "4.1"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "2.3"
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
