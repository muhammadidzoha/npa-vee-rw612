plugins {
    id("com.microej.gradle.veeport") version libs.versions.microej.sdk
}

microej {
    skippedCheckers = "changelog,readme,license"
}

dependencies {
    microejArchitecture(libs.architecture)

    microejPack(libs.pack.ui.architecture)
    microejPack(libs.pack.net.addons)
    microejPack(libs.pack.device)
    microejPack(libs.pack.ecom.wifi)
    microejPack(libs.pack.net)
    microejPack(libs.pack.ecom.network)

    microejFrontPanel(project(":vee-port:front-panel"))

    microejMock(project(":vee-port:mock"))

    microejTool(project(":vee-port:image-generator"))
}
