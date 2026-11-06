import java.util.Properties
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "org.sandhimitra.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.sandhimitra.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 17
        versionName = "0.1"
        resourceConfigurations += listOf("en", "kn", "sa")
    }

    signingConfigs {
        create("release") {
            // Upload key for Google Play. release/keystore.properties and the .jks are kept out of version control.
            val propsFile = file("${rootDir}/release/keystore.properties")
            if (propsFile.exists()) {
                val props = Properties().apply { propsFile.inputStream().use { load(it) } }
                storeFile = file("${rootDir}/release/" + props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val rel = signingConfigs.getByName("release")
            if (rel.storeFile != null) signingConfig = rel
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    androidResources { noCompress += listOf("db") }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { t ->
                // Robolectric: optional offline artifact directory (set ROBOLECTRIC_DEPS_DIR when Maven Central is unreachable)
                System.getenv("ROBOLECTRIC_DEPS_DIR")?.let { d -> t.systemProperty("robolectric.offline", "true"); t.systemProperty("robolectric.dependency.dir", d) }
                t.maxHeapSize = "2g"
            }
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.xerial:sqlite-jdbc:3.46.1.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
