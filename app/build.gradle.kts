plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.applicationstage"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.applicationstage"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.maps)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.play.services.maps)



    implementation("com.firebaseui:firebase-ui-auth:7.2.0")

    //implementation("com.google.android.gms:play-services-places:2.4.0")

    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.android.libraries.places:places:3.4.0")
    implementation("org.json:json:20220320")
    implementation("com.android.volley:volley:1.2.1")

    implementation("com.google.android.material:material:1.3.0-alpha03")







}