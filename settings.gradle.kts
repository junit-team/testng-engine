import com.gradle.enterprise.gradleplugin.internal.extension.BuildScanExtensionWithHiddenFeatures

plugins {
    id("com.gradle.enterprise") version "3.6.1"
}

rootProject.name = "testng-engine"

gradleEnterprise {
    buildScan {
        server = "https://ge.junit.org"
        isCaptureTaskInputFiles = true
        isUploadInBackground = System.getenv("CI") == null
        this as BuildScanExtensionWithHiddenFeatures
        publishIfAuthenticated()
    }
}
