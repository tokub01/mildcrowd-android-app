plugins {
    alias(libs.plugins.mildcrowd.android.feature)
}

android {
    namespace = "de.velwebsolutions.mildcrowd.account"
}

// Sign-in, registration, settings, privacy, safety and the member pages (MildcrowdAccount on iOS).
dependencies {
    implementation(libs.androidx.activity.compose)
    // Passkeys through Credential Manager (Google Play services below Android 14) and SMS User Consent for codes.
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.play.services.auth.api.phone)
    // The screens' Hilt view models under Robolectric (a test graph replaces :core's module).
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
}
