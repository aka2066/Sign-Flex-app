plugins {
    id("com.android.application") version "8.8.0"
}

android {
    namespace = "com.example.asl_recognition"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.asl_recognition"
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
}

dependencies {


    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion("1.8.22") // ✅ Set this to match your Kotlin version
            }
        }
    }



    // ✅ Latest Stable TensorFlow Lite Dependencies
    implementation ("org.tensorflow:tensorflow-lite:2.17.0")


    // ✅ CameraX (Latest Stable Versions for real-time image processing)
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // ✅ CameraX for real-time image processing
  //  implementation("androidx.camera:camera-core:1.3.0")
    //implementation("androidx.camera:camera-camera2:1.3.0")
    //implementation("androidx.camera:camera-lifecycle:1.3.0")
   // implementation("androidx.camera:camera-view:1.3.0")
    //


    // ✅ Android UI and Utility Dependencies
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ✅ OpenCV (Ensure the OpenCV module exists or replace with the official OpenCV library)
    implementation(project(":OpenCV"))
    // Or use:
    //implementation("org.opencv:opencv-android:4.5.3")

    // ✅ Testing Dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
