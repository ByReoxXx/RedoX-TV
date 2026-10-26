import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ---------------------------------------------------------------------------
// İmzalama: keystore.properties dosyası (yerel) veya ortam değişkenleri (GitHub Actions).
// Hiçbiri yoksa release APK de debug anahtarıyla imzalanır (yine kurulabilir, ama
// güncellemeler için sabit bir anahtar kullanmanız önerilir — README'ye bakın).
// ---------------------------------------------------------------------------
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

fun signingSecret(propertyName: String, envName: String): String? {
    val fromFile = keystoreProperties.getProperty(propertyName)
    val fromEnv = System.getenv(envName)
    val value = if (!fromFile.isNullOrBlank()) fromFile else fromEnv
    return if (value.isNullOrBlank()) null else value
}

android {
    namespace = "com.example.chromecasttv"
    compileSdk = 34

    defaultConfig {
        // "RedoX TV 2": eski RedoX TV (com.redoxtv.app) uygulamasından BAĞIMSIZ, ayrı bir uygulamadır.
        // Farklı paket kimliği sayesinde ikisi aynı cihazda yan yana kurulabilir; veriler paylaşılmaz.
        applicationId = "com.redoxtv.two"
        minSdk = 28
        targetSdk = 34
        versionCode = 210
        versionName = "2.1.0"
        // Arayüz yalnızca Türkçe/İngilizce: kitaplıkların diğer dillerdeki metinleri pakete girmez
        resourceConfigurations += listOf("tr", "en")
        buildConfigField("String", "APP_DISPLAY_NAME", "\"RedoX TV 2\"")
        // Güncelleme denetimi bu depodaki "latest release"e bakar. GitHub Actions'ta depo adı otomatik gelir.
        // REDOX_REPO_OVERRIDE: yalnızca ekran görüntüsü derlemeleri; gerçek listeyi indirmesin diye boş bir depoya işaret ettirilir
        val githubRepo = System.getenv("REDOX_REPO_OVERRIDE")?.takeIf { it.isNotBlank() }
            ?: System.getenv("GITHUB_REPOSITORY")?.takeIf { it.isNotBlank() } ?: "ByReoxXx/RedoX-TV-2"
        buildConfigField("String", "GITHUB_REPO", "\"$githubRepo\"")
    }

    // -----------------------------------------------------------------------
    // Üç cihaz sınıfı = üç ayrı APK, üç ayrı arayüz:
    //   tv     -> src/tv      (kumanda / D-Pad odaklı "lean-back" arayüz)
    //   tablet -> src/tablet  (+ ortak src/mobile)  gezinti rayı + çift panel
    //   phone  -> src/phone   (+ ortak src/mobile)  alt gezinti + mini oynatıcı
    // Ortak çekirdek (veri, oynatıcı fabrikası, ViewModel, tasarım sistemi) src/main içindedir.
    // -----------------------------------------------------------------------
    flavorDimensions += "device"
    productFlavors {
        create("tv") {
            dimension = "device"
            buildConfigField("String", "DEVICE_KIND", "\"tv\"")
        }
        create("tablet") {
            dimension = "device"
            applicationIdSuffix = ".tablet"
            buildConfigField("String", "DEVICE_KIND", "\"tablet\"")
        }
        create("phone") {
            dimension = "device"
            applicationIdSuffix = ".phone"
            buildConfigField("String", "DEVICE_KIND", "\"phone\"")
        }
    }

    sourceSets {
        getByName("phone") {
            java.srcDir("src/mobile/java")
        }
        getByName("tablet") {
            java.srcDir("src/mobile/java")
        }
    }

    signingConfigs {
        create("release") {
            val storePath = signingSecret("storeFile", "REDOX_KEYSTORE_FILE")
            if (storePath != null && file(storePath).exists()) {
                storeFile = file(storePath)
                storePassword = signingSecret("storePassword", "REDOX_KEYSTORE_PASSWORD")
                keyAlias = signingSecret("keyAlias", "REDOX_KEY_ALIAS")
                keyPassword = signingSecret("keyPassword", "REDOX_KEY_PASSWORD")
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        getByName("release") {
            // Kod küçültme (R8) KAPALI: R8 (8.5 ve 8.7 denendi), büyük Compose işlevlerinde (ör. TV M3uPlaylistManagerScreen)
            // geçersiz DEX üretip uygulamayı VerifyError ile çökertiyor (optimizasyon kapalıyken de; CI emülatöründe görüldü).
            // Kararlılık APK boyutundan önemli; boyut için material-icons-extended kaldırıldı ve yalnızca tr/en dil kaynakları tutulur.
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = false
            val releaseSigning = signingConfigs.getByName("release")
            signingConfig = if (releaseSigning.storeFile != null) {
                releaseSigning
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.tv.material3.ExperimentalTvMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.media3.common.util.UnstableApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/INDEX.LIST"
            )
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")

    // Android TV arayüzü (yalnızca tv flavor'ı kullanır ama ortak kod derlenebilsin diye hepsinde var)
    implementation("androidx.tv:tv-foundation:1.0.0-alpha11")
    implementation("androidx.tv:tv-material:1.0.0")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")

    val media3Version = "1.3.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-datasource:$media3Version")
    implementation("androidx.media3:media3-datasource-okhttp:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("io.coil-kt:coil-compose:2.6.0")

    // Birim testleri (JVM): ayrıştırıcılar, rehber eşleştirme, radyo durum yardımcıları
    testImplementation("junit:junit:4.13.2")

    // Android TV ana ekran entegrasyonu (Watch Next / önizleme kanalı)
    "tvImplementation"("androidx.tvprovider:tvprovider:1.0.0")
}
