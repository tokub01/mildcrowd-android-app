plugins {
    alias(libs.plugins.mildcrowd.android.feature)
}

android {
    namespace = "de.velwebsolutions.mildcrowd.membership"
}

// Google Play Billing for the in-app membership (PlayBillingGateway); core-ktx for the FileProvider of invoices.
dependencies {
    implementation(libs.billing.ktx)
    implementation(libs.androidx.core.ktx)
}
