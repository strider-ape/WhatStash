import java.util.Properties

plugins {
    // AGP 9 compiles Kotlin itself (built-in Kotlin), so the kotlin-android plugin is not applied here.
    alias(libs.plugins.agp)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.androidx.safeargs)
}

sealed class Version(
    private val versionMajor: Int,
    private val versionMinor: Int,
    private val versionPatch: Int,
    private val versionBuild: Int = 0,
    private val versionType: String = ""
) {
    class Alpha(versionMajor: Int, versionMinor: Int, versionPatch: Int, versionBuild: Int) :
        Version(versionMajor, versionMinor, versionPatch, versionBuild, "alpha")

    class Beta(versionMajor: Int, versionMinor: Int, versionPatch: Int, versionBuild: Int) :
        Version(versionMajor, versionMinor, versionPatch, versionBuild, "beta")

    class RC(versionMajor: Int, versionMinor: Int, versionPatch: Int, versionBuild: Int) :
        Version(versionMajor, versionMinor, versionPatch, versionBuild, "rc")

    class Stable(versionMajor: Int, versionMinor: Int, versionPatch: Int) :
        Version(versionMajor, versionMinor, versionPatch)

    fun toVersionName(): String {
        val versionName = "${versionMajor}.${versionMinor}.${versionPatch}"
        if (versionType.isNotEmpty()) {
            return "$versionName-${versionType}.$versionBuild"
        }
        return versionName
    }

    fun toVersionCode(): Int =
        versionMajor * 1000 + versionMinor * 100 + versionPatch * 10 + versionBuild
}

val currentVersion: Version = Version.Stable(
    versionMajor = 1,
    versionMinor = 0,
    versionPatch = 0
)

android {
    compileSdk = 37
    namespace = "com.simplified.wsstatussaver"

    defaultConfig {
        minSdk = 24
        targetSdk = 36

        applicationId = "io.github.striderape.whatstash"
        versionCode = currentVersion.toVersionCode()
        versionName = currentVersion.toVersionName()
    }

    val signingProperties = getProperties("keystore.properties")
    val releaseSigning = if (signingProperties != null) {
        signingConfigs.create("release") {
            keyAlias = signingProperties.property("keyAlias")
            keyPassword = signingProperties.property("keyPassword")
            storePassword = signingProperties.property("storePassword")
            storeFile = file(signingProperties.property("storeFile"))
        }
    } else {
        signingConfigs.getByName("debug")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = releaseSigning
        }
        debug {
            versionNameSuffix = " DEBUG"
            applicationIdSuffix = ".debug"
            signingConfig = releaseSigning
        }
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    androidResources {
        generateLocaleConfig = true
    }
    packaging {
        resources {
            excludes += listOf("META-INF/LICENSE", "META-INF/NOTICE", "META-INF/java.properties")
        }
    }
    lint {
        abortOnError = true
        warning += listOf("ImpliedQuantity", "Instantiatable", "MissingQuantity", "MissingTranslation")
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

// APKs are named WhatStash-v<version>-<buildType>.apk
base {
    archivesName = "WhatStash-v${currentVersion.toVersionName()}"
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.add("kotlin.RequiresOptIn")
    }
}

fun getProperties(fileName: String): Properties? {
    val file = rootProject.file(fileName)
    return if (file.exists()) {
        Properties().also { properties ->
            file.inputStream().use { properties.load(it) }
        }
    } else null
}

fun Properties.property(key: String) =
    this.getProperty(key) ?: "$key missing"

dependencies {
    // Google/JetPack
    //https://developer.android.com/jetpack/androidx/versions
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)

    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.common.java8)

    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.loader)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.material.components)

    implementation(libs.koin.core)
    implementation(libs.koin.android)

    implementation(libs.coil)
    implementation(libs.coil.video)

    implementation(libs.photoview)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.libphonenumber)
    implementation(libs.prettytime)
    implementation(libs.advrecyclerview)

    implementation(libs.markdown.core)
    implementation(libs.markdown.html)

    // Kotlin
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}