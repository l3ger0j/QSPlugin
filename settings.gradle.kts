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
        google()
        mavenCentral()
    }
}

rootProject.name = "QSPlugin"

include(":app")

include(":core")
include(":core:utils")
include(":core:dto")

include(":features")
include(":features:main:presentation")
include(":features:extra:presentation")
include(":features:object:presentation")
include(":features:input:presentation")
include(":features:dialogs:presentation")

include(":services")
include(":services:native-lib")
include(":services:native-lib:byte")
include(":services:native-lib:sonnix")
include(":services:native-lib:qsengo")
include(":services:native-lib:supervisor")
include(":services:audio")
include(":services:settings")
