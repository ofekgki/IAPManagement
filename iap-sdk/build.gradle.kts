plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.iapsdk"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // Kept low so the SDK can be consumed by a wide range of host apps.
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Material Components (View-based) powers the dialog theming, MaterialCardView and MaterialButton.
    implementation(libs.material)
    implementation(libs.androidx.core.ktx)
    // Coroutines back the suspend-based public API (getItem, makePurchase, restorePurchases, ...).
    implementation(libs.kotlinx.coroutines.android)
    // Gson serializes the local cache (items / entitlements / config) into SharedPreferences.
    implementation(libs.gson)
    testImplementation(libs.junit)
}
