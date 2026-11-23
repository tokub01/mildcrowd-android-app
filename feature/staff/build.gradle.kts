plugins {
    alias(libs.plugins.mildcrowd.android.feature)
}

android {
    namespace = "de.velwebsolutions.mildcrowd.staff"
}

// core-ktx for the FileProvider of the evidence archive.
dependencies {
    implementation(libs.androidx.core.ktx)
}
