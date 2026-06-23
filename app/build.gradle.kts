import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  kotlin("plugin.serialization") version "2.2.10"
}

fun getEnvOrProperty(key: String): String {
    val systemEnv = System.getenv(key)
    if (!systemEnv.isNullOrBlank()) {
        return systemEnv
    }
    val envFileDef = file("${project.rootDir}/.env")
    if (envFileDef.exists()) {
        val props = Properties()
        val stream = envFileDef.inputStream()
        try {
            props.load(stream)
            val value = props.getProperty(key)
            if (!value.isNullOrBlank()) {
                return value
            }
        } finally {
            stream.close()
        }
    }
    val localFileDef = file("${project.rootDir}/local.properties")
    if (localFileDef.exists()) {
        val props = Properties()
        val stream = localFileDef.inputStream()
        try {
            props.load(stream)
            val value = props.getProperty(key)
            if (!value.isNullOrBlank()) {
                return value
            }
        } finally {
            stream.close()
        }
    }
    return ""
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.aiknowledge.brxqlz"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

   buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
      buildConfigField("String", "FIREBASE_API_KEY", "\"${getEnvOrProperty("FIREBASE_API_KEY")}\"")
      buildConfigField("String", "FIREBASE_APP_ID", "\"${getEnvOrProperty("FIREBASE_APP_ID")}\"")
      buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${getEnvOrProperty("FIREBASE_PROJECT_ID")}\"")
      buildConfigField("String", "FIREBASE_STORAGE_BUCKET", "\"${getEnvOrProperty("FIREBASE_STORAGE_BUCKET")}\"")
      buildConfigField("String", "FIREBASE_AUTH_DOMAIN", "\"${getEnvOrProperty("FIREBASE_AUTH_DOMAIN")}\"")
      buildConfigField("String", "FIREBASE_MESSAGING_SENDER_ID", "\"${getEnvOrProperty("FIREBASE_MESSAGING_SENDER_ID")}\"")
      buildConfigField("String", "OPENROUTER_API_KEY", "\"${getEnvOrProperty("OPENROUTER_API_KEY")}\"")
      buildConfigField("String", "GROQ_API_KEY", "\"${getEnvOrProperty("GROQ_API_KEY")}\"")
      buildConfigField("String", "GEMINI_API_KEY", "\"${getEnvOrProperty("GEMINI_API_KEY")}\"")
      buildConfigField("String", "GITHUB_TOKEN", "\"${getEnvOrProperty("GITHUB_TOKEN")}\"")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
      buildConfigField("String", "FIREBASE_API_KEY", "\"${getEnvOrProperty("FIREBASE_API_KEY")}\"")
      buildConfigField("String", "FIREBASE_APP_ID", "\"${getEnvOrProperty("FIREBASE_APP_ID")}\"")
      buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${getEnvOrProperty("FIREBASE_PROJECT_ID")}\"")
      buildConfigField("String", "FIREBASE_STORAGE_BUCKET", "\"${getEnvOrProperty("FIREBASE_STORAGE_BUCKET")}\"")
      buildConfigField("String", "FIREBASE_AUTH_DOMAIN", "\"${getEnvOrProperty("FIREBASE_AUTH_DOMAIN")}\"")
      buildConfigField("String", "FIREBASE_MESSAGING_SENDER_ID", "\"${getEnvOrProperty("FIREBASE_MESSAGING_SENDER_ID")}\"")
      buildConfigField("String", "OPENROUTER_API_KEY", "\"${getEnvOrProperty("OPENROUTER_API_KEY")}\"")
      buildConfigField("String", "GROQ_API_KEY", "\"${getEnvOrProperty("GROQ_API_KEY")}\"")
      buildConfigField("String", "GEMINI_API_KEY", "\"${getEnvOrProperty("GEMINI_API_KEY")}\"")
      buildConfigField("String", "GITHUB_TOKEN", "\"${getEnvOrProperty("GITHUB_TOKEN")}\"")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation("com.google.firebase:firebase-auth")
  implementation("com.google.firebase:firebase-firestore")
  implementation("com.google.firebase:firebase-database")
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
