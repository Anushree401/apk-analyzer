package com.example.android_manager.model

data class AppInfo(
    val name: String,
    val packageName: String,
    val versionName: String?,
    val isSystemApp: Boolean
)