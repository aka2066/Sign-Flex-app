pluginManagement {
    repositories {
        google() // ✅ Ensure Google repository is present
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // ✅ Add this line
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT) // ✅ Change from PREFER_SETTINGS to PREFER_PROJECT
    repositories {
        google() // ✅ Ensures TensorFlow Lite dependencies are found
        mavenCentral()
    }
}

rootProject.name = "ASL_Recognition"
include(":app")
include(":OpenCV") // Ensures OpenCV module is included
