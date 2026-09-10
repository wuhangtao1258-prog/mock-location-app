pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 百度地图官方 Maven 仓库
        maven { url = java.net.URI("https://api.bmap.baidu.com/sdk/maven") }
    }
}
rootProject.name = "MockLocationApp"
include(":app")
