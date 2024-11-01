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
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal {
            content {
                includeGroup("org.filesys")
            }
        }
        maven("https://jitpack.io") {
            content {
                includeGroup("com.github.topjohnwu.libsu")
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "SimbaDroid"
include(":app")
