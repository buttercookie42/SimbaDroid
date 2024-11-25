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
    compileSdk = 34

    defaultConfig {
        applicationId = "de.buttercookie.simbadroid"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
        base.archivesName = "$applicationId-$versionName"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resourceConfigurations += listOf("en", "de")
    }

    androidResources {
        generateLocaleConfig = true;
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
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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