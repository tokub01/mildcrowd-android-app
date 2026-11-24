plugins {
    alias(libs.plugins.mildcrowd.android.feature)
}

android {
    namespace = "de.velwebsolutions.mildcrowd.live"
}

// Hinweise, their settings, the live counters of the app shell and push (MildcrowdLive on iOS). Push comes through Firebase
// Cloud Messaging: with google-services.json in the app the FCM token is registered with the server (PushDeviceController,
// platform android); without it nothing is set up.
dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.firebase.messaging)
    // The screens' Hilt view models and the Firebase service under Robolectric (a test graph replaces :core's module).
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
}
