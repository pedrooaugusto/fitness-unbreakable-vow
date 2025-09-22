import java.util.Properties

val addresses = Properties().apply { load(file(".addresses").inputStream()) }
val envVars = Properties().apply { load(file(".env").inputStream()) }

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt")
}

android {
    namespace = "com.august.fitnessvowsync"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.august.fitnessvowsync"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val Network: String = addresses["LastUsedNetwork"] as String
        val PhysicalActivityOracleAddress: String = addresses["$Network.PhysicalActivityOracle"] as String
        val FitnessUnbreakableVowAddress: String = addresses["$Network.FitnessUnbreakableVow"] as String
        val WalletPrivateKey: String = envVars["$Network.WALLET_PRIVATE_KEY"] as String // Idk, Rick...
        val RpcUrl: String = envVars["$Network.RPC_URL"] as String

        buildConfigField("String", "PHYSICAL_ACTIVITY_ORACLE_ADDRESS", "\"${PhysicalActivityOracleAddress}\"")
        buildConfigField("String", "FITNESS_UNBREAKABLE_VOW_ADDRESS", "\"${FitnessUnbreakableVowAddress}\"")
        buildConfigField("String", "NETWORK", "\"${Network}\"")
        buildConfigField("String", "WALLET_PRIVATE_KEY", "\"${WalletPrivateKey}\"")
        buildConfigField("String", "RPC_URL", "\"${RpcUrl}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/DISCLAIMER",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/FastDoubleParser-LICENSE",
                "META-INF/FastDoubleParser-NOTICE",
                "META-INF/io.netty.versions.properties"
            )
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    implementation("com.google.dagger:dagger:2.50")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    kapt("com.google.dagger:dagger-compiler:2.50")

    // Something to do with records not being supported
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")

    // Helath connect client
    implementation("androidx.health.connect:connect-client:1.0.0-alpha11")

    // EncryptedSharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Geofencing
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Client for interacting with the blockchain ethereum
    implementation("org.web3j:core:4.13.0")

    // String to JSON
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.assertj:assertj-core:3.24.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}