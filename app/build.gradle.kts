import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 签名方式与原 Flutter 工程 android/app/build.gradle 一致：
//   CI(workflow) 用 secrets 生成项目根 key.properties，亦可走同名环境变量兜底。
//   secrets: KEYSTORE_BASE64 / STORE_PASSWORD / KEY_PASSWORD / KEY_ALIAS
val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(FileInputStream(keystorePropertiesFile))
}
val hasReleaseKeystore = keystorePropertiesFile.exists() ||
    (System.getenv("KEY_ALIAS") != null && System.getenv("STORE_FILE") != null)

android {
    namespace = "com.lan.scanner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lan.scanner"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // 仅保留中/英文资源，裁掉依赖库内其余语言
        resourceConfigurations.addAll(listOf("zh", "en"))
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties.getProperty("keyAlias") ?: System.getenv("KEY_ALIAS")
            keyPassword = keystoreProperties.getProperty("keyPassword") ?: System.getenv("KEY_PASSWORD")
            storePassword = keystoreProperties.getProperty("storePassword") ?: System.getenv("STORE_PASSWORD")
            val sf = keystoreProperties.getProperty("storeFile") ?: System.getenv("STORE_FILE")
            if (sf != null) storeFile = file(sf)   // 相对 app/ 模块，即 app/release.jks
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ""
        }
        release {
            // 精简核心：R8 移除未引用代码（material-icons-extended 等）与无用资源
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // CI 注入 key.properties 时用正式签名；本地无 keystore 时回退 debug 签名，
            // 保证 assembleRelease 在任何环境都能构建（不改变 workflow 的签名方式）
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    // 与原工程一致：只产出 arm64-v8a（对应原 --target-platform android-arm64）
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*"
            )
        }
    }
}

dependencies {
    // BOM >= 2024.09.00 (Compose UI 1.7+)：LocalLifecycleOwner 迁移到 lifecycle 库后，
    // 旧版 UI 1.6.x 不会提供新的 LocalLifecycleOwner，配合 lifecycle-runtime-compose 2.8.x 会闪退
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ui-tooling / ui-tooling-preview 只用于 IDE 预览，不进入 release（工程内无 @Preview）
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
