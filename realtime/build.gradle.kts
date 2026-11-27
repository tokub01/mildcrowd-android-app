plugins {
    alias(libs.plugins.mildcrowd.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "de.velwebsolutions.mildcrowd.realtime"
}

// Reverb (Pusher protocol) client implementing `:core`'s RealtimeService (MildcrowdRealtime on iOS).
dependencies {
    implementation(project(":core"))
    implementation(libs.hilt.android)
    implementation(libs.androidx.lifecycle.process)
    ksp(libs.hilt.compiler)
    testImplementation(project(":core-testing"))
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
}
