import com.android.build.api.dsl.ApkSigningConfig

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.externalproperties)
}

externalProperties {
    propertiesFileResolver(file("signing.properties"))
}

android {
    namespace = "de.buttercookie.simbadroid"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.buttercookie.simbadroid"
        targetSdk = 34
        versionCode = 10
        versionName = "0.8"
        base.archivesName = "$applicationId-$versionName"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += listOf("api")

    productFlavors {
        create("default") {
            dimension = "api"
            minSdk = 24
            versionCode = (defaultConfig.versionCode ?: 0) * 10 + 2
        }

        create("oldApi") {
            dimension = "api"
            minSdk = 23
            versionCode = (defaultConfig.versionCode ?: 0) * 10 + 1
        }
    }

    androidResources {
        generateLocaleConfig = true
        localeFilters += listOf("en", "de", "it")
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // This doesn't support Mercurial right now and also only causes confusion with F-Droid
            // reproducible builds, which will build from the Github repository.
            vcsInfo.include = false
        }
    }
    packaging {
        resources {
            excludes.add("META-INF/AL2.0")
            excludes.add("META-INF/LGPL2.1")
            excludes.add("META-INF/**/constraintlayout-core/LICENSE.txt")
            excludes.add("org/bouncycastle/pqc/crypto/picnic/*")
            excludes.add("com/sun/jna/**")
        }
    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_16
        targetCompatibility = JavaVersion.VERSION_16
    }

    signingConfigs {
        named("debug") {
            if (checkExternalSigningConfig()) {
                applyExternalSigningConfig()
            } else {
                defaultConfig.signingConfig
            }
        }
        create("release") {
            if (checkExternalSigningConfig()) {
                applyExternalSigningConfig()
                android.buildTypes.getByName("release").signingConfig = this
            }
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.corelibdesugar)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.libsu.core)
    implementation(libs.jfileserver) {
        // Hazlecast is only needed for clustering and not compatible with Android anyway
        exclude(group = "com.hazelcast", module = "hazelcast")
        // We specify our own explicit Bouncy Castle dependency
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
    }
    implementation(libs.bouncycastle.bcprov)

    implementation(libs.jmdns)
    implementation(libs.slf4j.provider)

    implementation(libs.apache.commons.lang)
    implementation(libs.guava)
}

fun ApkSigningConfig.checkExternalSigningConfig(): Boolean {
    return props.exists("$name.keyStore") &&
            file(props.get("$name.keyStore")).exists() &&
            props.exists("$name.storePassword") &&
            props.exists("$name.keyAlias") &&
            props.exists("$name.keyPassword")
}

fun ApkSigningConfig.applyExternalSigningConfig() {
    storeFile = file(props.get("$name.keyStore"))
    storePassword = props.get("$name.storePassword")
    keyAlias = props.get("$name.keyAlias")
    keyPassword = props.get("$name.keyPassword")
}