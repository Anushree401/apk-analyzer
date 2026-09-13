package com.example.android_manager.model

data class ApkInfo(
    val packageName: String,
    val versionName: String?,
    val versionCode: Long?,
    val minSdk: Int?,
    val targetSdk: Int?,
    val appName: String?
)