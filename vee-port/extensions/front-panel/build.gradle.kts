plugins {
    id("com.microej.gradle.mock-frontpanel")
}

microej {
    skippedCheckers = "changelog,readme,license"
}

dependencies {
    implementation(libs.frontpanel.ui.widget)
    implementation(libs.frontpanel.framework)
    implementation(libs.pack.ui) {
        artifact {
            name = "frontpanel"
        }
    }
}
