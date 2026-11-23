plugins {
    alias(libs.plugins.mildcrowd.android.feature)
}

android {
    namespace = "de.velwebsolutions.mildcrowd.network"
}

// Posts, Crowds, profiles, discovery and search (MildcrowdNetwork on iOS). The photo picker and the camera need the
// activity result APIs; the camera shares its file through androidx.core's FileProvider.
dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    // The router test renders every route with the real Hilt view models over a stubbed API.
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
}
