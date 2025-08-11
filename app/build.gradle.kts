plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.scram.systems.privacyprotection"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.scram.systems.privacyprotection"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"

    }

    buildTypes {
        release {
            isMinifyEnabled = true

            // Enables resource shrinking.
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.google.accompanist:accompanist-drawablepainter:0.37.3")
    implementation("androidx.compose.material:material-icons-extended")
// Or the latest stable version
    // implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
// Or the latest stable version
    // implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2")
// Or the latest stable version
    // Use a single version variable for all Room artifacts
    val roomVersion = "2.7.2"

    // Room runtime & API
    implementation("androidx.room:room-runtime:$roomVersion")
    // Kotlin extensions (Coroutines, Flow, etc.)
    implementation("androidx.room:room-ktx:$roomVersion")
    // Annotation processor
    ksp("androidx.room:room-compiler:$roomVersion")

    // For SupportSQLiteDatabase + Kotlin extensions
    implementation("androidx.sqlite:sqlite-ktx:2.5.2")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
}
