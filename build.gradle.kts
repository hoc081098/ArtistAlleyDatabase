buildscript {
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.42")
    }
}

plugins {
    id("com.android.application")version "7.4.0-alpha03" apply false
    id("com.android.library") version "7.4.0-alpha03" apply false
    id("org.jetbrains.kotlin.android") version "1.7.0" apply false
}