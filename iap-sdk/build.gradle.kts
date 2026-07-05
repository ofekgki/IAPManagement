plugins {
    alias(libs.plugins.android.library)
    // Publishes the AAR (+ sources) so JitPack can serve this module as a Maven dependency.
    `maven-publish`
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

    // Expose a single "release" variant to publish, with a matching sources jar.
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

// JitPack picks up this publication. It overrides groupId/version with
// com.github.<user> and the git tag, so we only wire up the release component here.
// Consumers reference it as: com.github.ofekgki.IAPManagement:iap-sdk:<tag>
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.ofekgki"
            artifactId = "iap-sdk"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }
        }
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
