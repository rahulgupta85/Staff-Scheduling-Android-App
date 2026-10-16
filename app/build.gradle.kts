plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.edutech"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.edutech"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    buildFeatures {
        viewBinding = true
    }

    // --- NEW BLOCK ADDED to fix the 'mergeDebugJavaResource' error ---
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // This line specifically excludes the duplicate file from the error log
            excludes += "google/firestore/v1/query.proto"
        }
    }
}

// --- THIS DEPENDENCIES BLOCK HAS BEEN COMPLETELY REWRITTEN ---
dependencies {
    // Standard Android Libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase Bill of Materials (BOM) - This manages all Firebase versions for you
    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))

    // Firebase Services (No need to specify versions anymore)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")

    // Other libraries you might be using (kept from your original file)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
