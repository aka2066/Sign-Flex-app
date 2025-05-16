plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.sign_flex"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.sign_flex"
        minSdk = 24
        targetSdk = 35
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
    
    // Resolve duplicate files
    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            excludes.add("server/node_modules/bcryptjs/dist/bcrypt.min.js.gz")
            excludes.add("server/node_modules/bcryptjs/dist/bcrypt.min.js")
        }
    }
    
    // Enable data binding
    buildFeatures {
        viewBinding = true
    }
    
    // Configure lint options
    lint {
        abortOnError = false
        checkReleaseBuilds = false
        // Ignore MissingPermission issues
        disable += "MissingPermission"
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    
    // CameraX dependencies
    implementation("androidx.camera:camera-core:1.3.2")
    implementation("androidx.camera:camera-camera2:1.3.2")
    implementation("androidx.camera:camera-lifecycle:1.3.2")
    implementation("androidx.camera:camera-view:1.3.2")
    
    // TensorFlow Lite dependencies with all ops
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    // Add select TF ops for ASL model
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.14.0")
    // GPU acceleration (optional)
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    
    // Volley for network requests
    implementation("com.android.volley:volley:1.2.1")
    
    // GSON for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // OpenCV - using an open-source repo
    implementation("com.quickbirdstudios:opencv:3.4.1")
}