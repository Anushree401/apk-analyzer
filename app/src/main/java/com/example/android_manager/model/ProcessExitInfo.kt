package com.example.android_manager.model

data class ProcessExitInfo(
    val packageName: String,
    val processName: String,
    val timestamp: Long,
    val reason: String,
    val description: String?
)