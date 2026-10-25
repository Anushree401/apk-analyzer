package com.example.android_manager.model

data class AppInfo(
    val name: String,
    val packageName: String,
    val versionName: String?,
    val isSystemApp: Boolean,
    val installSource: String? = null,
    val firstInstallTime: Long = 0L,
    val lastUpdateTime: Long = 0L,
    val apkSizeBytes: Long = 0L,
    val certificateSha256: String? = null
)