plugins {
    kotlin("jvm")

    // Gradle --scan
    id("com.gradle.build-scan") version "3.12.3" apply false
}

/**
 * This file stores dependencies for tasks run during development that are not easily captured
 * by generateVerificationMetadata. The explicit declarations here allow that task to generate
 * the checksums for these dependencies.
 *
 * The alternative is to ignore these specific groups in verification-metadata.xml, but until this
 * problem becomes too annoying, doing it this way is technically more correct.
 */
dependencies {
    // Android Studio instrumentation testing
    runtimeOnly("com.android.tools.utp:android-device-provider-ddmlib:31.1.0-alpha05")
    runtimeOnly("com.android.tools.utp:android-device-provider-gradle:31.1.0-alpha05")
    runtimeOnly("com.android.tools.utp:android-test-plugin-host-additional-test-output:31.1.0-alpha05")
    runtimeOnly("com.android.tools.utp:android-test-plugin-host-apk-installer:31.1.0-alpha05")
    runtimeOnly("com.android.tools.utp:android-test-plugin-host-coverage:31.1.0-alpha05")
    runtimeOnly("com.android.tools.utp:android-test-plugin-host-device-info:31.1.0-alpha05")
    runtimeOnly("com.android.tools.utp:android-test-plugin-host-logcat:31.1.0-alpha05")
    runtimeOnly("com.android.tools.utp:android-test-plugin-host-retention:31.1.0-alpha05")
    runtimeOnly("com.android.tools.utp:android-test-plugin-result-listener-gradle:31.1.0-alpha05")
    runtimeOnly("com.google.testing.platform:android-device-provider-local:0.0.8-alpha08")
    runtimeOnly("com.google.testing.platform:android-driver-instrumentation:0.0.8-alpha08")
    runtimeOnly("com.google.testing.platform:android-test-plugin:0.0.8-alpha08")
    runtimeOnly("com.google.testing.platform:core:0.0.8-alpha08")
    runtimeOnly("com.google.testing.platform:launcher:0.0.8-alpha08")
}