plugins {
    `kotlin-dsl`
}

group = "de.velwebsolutions.mildcrowd.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.kover.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "mildcrowd.android.application"
            implementationClass = "de.velwebsolutions.mildcrowd.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "mildcrowd.android.library"
            implementationClass = "de.velwebsolutions.mildcrowd.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "mildcrowd.android.feature"
            implementationClass = "de.velwebsolutions.mildcrowd.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("jvmLibrary") {
            id = "mildcrowd.jvm.library"
            implementationClass = "de.velwebsolutions.mildcrowd.buildlogic.JvmLibraryConventionPlugin"
        }
    }
}
