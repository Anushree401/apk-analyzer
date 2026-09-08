package com.example.android_manager.model

data class ProcessInfo(
    val processName: String,
    val pid: Int,
    val memoryMb: Long,
    val importance: Int,
    val isForeground: Boolean
)