import com.gradle.enterprise.gradleplugin.internal.extension.BuildScanExtensionWithHiddenFeatures

plugins {
    id("com.gradle.enterprise") version "3.16.2"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "1.13"
}

rootProject.name = "testng-engine"

gradleEnterprise {
    buildScan {
        val isCiServer = System.getenv("CI") != null

        server = "https://ge.junit.org"
        isUploadInBackground = !isCiServer

        capture {
            isTaskInputFiles = true
        }

        obfuscation {
            if (isCiServer) {
                username { "github" }
            } else {
                hostname { null }
                ipAddresses { emptyList() }
            }
        }

        this as BuildScanExtensionWithHiddenFeatures
        publishIfAuthenticated()
    }
}
