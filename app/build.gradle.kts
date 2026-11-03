plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.english_app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.english_app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"${providers.gradleProperty("apiBaseUrl").orElse("http://10.0.3.2:8080/api/v1/").get()}\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"${providers.gradleProperty("apiBaseUrl").orElse("https://example.invalid/api/v1/").get()}\"")
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
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
