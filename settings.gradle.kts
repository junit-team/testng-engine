import com.gradle.enterprise.gradleplugin.internal.extension.BuildScanExtensionWithHiddenFeatures

plugins {
    id("com.gradle.enterprise") version "3.6.1"
}

rootProject.name = "testng-engine"

gradleEnterprise {
    buildScan {
        val isCiServer = System.getenv("CI") != null

        server = "https://ge.junit.org"

        isCaptureTaskInputFiles = true
        isUploadInBackground = isCiServer

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
