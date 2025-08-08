pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        //maven { url = uri("https://jitpack.io") }
        maven { url = uri("./repo") }

    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "activityGuard"
//include(":app")
//include(":model1")

include(":plugin_main")
include(":plugin_common")
include(":plugin_agp_v74")
include(":plugin_agp_v86")
include(":plugin_agp_v87")
