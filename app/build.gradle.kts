plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
}
android {
 namespace = "com.scanly.documents"
 compileSdk = 35
 defaultConfig { applicationId = "com.scanly.documents"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0.0" }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2024.12.01"))
 implementation("androidx.activity:activity-compose:1.10.0")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")
 implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")
 debugImplementation("androidx.compose.ui:ui-tooling")
}
