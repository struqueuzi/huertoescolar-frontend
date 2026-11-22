plugins {
    jacoco
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}
android {
    namespace = "cl.huerto"
    compileSdk = 35
    defaultConfig {
        applicationId = "cl.huerto.escolar"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
    }
    testOptions.unitTests.isIncludeAndroidResources = true
    signingConfigs {
        create("academica") {
            val ruta = System.getenv("HUERTO_JKS")
            if (ruta != null) {
                storeFile = file(ruta)
                storePassword = System.getenv("HUERTO_CLAVE")
                keyAlias = "huerto"
                keyPassword = storePassword
            }
        }
    }
    buildTypes { getByName("release") { signingConfig = signingConfigs.getByName("academica") } }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    testImplementation(platform("androidx.compose:compose-bom:2024.09.03"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
}

jacoco { toolVersion = "0.8.12" }
tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> { isIncludeNoLocationClasses = true; excludes = listOf("jdk.internal.*") }
}
val businessClasses = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
    include("cl/huerto/domain/**", "cl/huerto/data/Repository*.class", "cl/huerto/ui/HuertoViewModel*.class",
        "cl/huerto/ui/Garden*.class", "cl/huerto/remote/TallerViewModel*.class", "cl/huerto/remote/ServicesKt*.class")
}
tasks.register<JacocoReport>("coberturaLogica") {
    dependsOn("testDebugUnitTest")
    executionData(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))
    sourceDirectories.setFrom("src/main/java")
    classDirectories.setFrom(businessClasses)
    reports { html.required.set(true); xml.required.set(true) }
}
tasks.register<JacocoCoverageVerification>("verificarCobertura") {
    dependsOn("coberturaLogica")
    executionData(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))
    classDirectories.setFrom(businessClasses)
    violationRules { rule { limit { counter = "LINE"; minimum = "0.80".toBigDecimal() } } }
}
