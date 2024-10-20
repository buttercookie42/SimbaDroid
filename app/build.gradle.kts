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
        versionName = "0.1a1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidResources {
        generateLocaleConfig = true;
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.libsu.core)
    implementation(libs.libsu.service)
    implementation(libs.libsu.nio)
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