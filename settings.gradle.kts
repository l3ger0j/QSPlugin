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

include(
    ":core",
    ":core:utils",
    ":core:dto"
)

include(
    ":features",
    ":features:main:presentation",
    ":features:extra:presentation",
    ":features:object:presentation",
    ":features:input:presentation",
    ":features:dialogs:presentation"
)

include(
    ":services",
    ":services:audio",
    ":services:natives",
    ":services:settings"
)
