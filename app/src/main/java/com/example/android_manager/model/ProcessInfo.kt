package com.example.android_manager.model

enum class ProcessState {
    FOREGROUND,
    BACKGROUND,
    RECENTLY_ACTIVE
}

data class ProcessInfo(
    val processName: String,
    val packageName: String,
    val pid: Int,
    val memoryMb: Long,
    val importance: Int,
    val isForeground: Boolean,
    val state: ProcessState,
    val lastActiveTime: Long = 0L
)