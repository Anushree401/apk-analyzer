package com.example.android_manager.data.scanner

import android.content.Context
import android.net.Uri
import java.io.File

class ApkScannerRepository(
    private val context: Context
) {

    private val analyzer =
        ApkAnalyzer(context)

    fun scanApk(uri: Uri): ApkAnalysisResult {

        val apkFile = copyUriToCache(uri)

        return try {
            analyzer.analyze(apkFile)
        } finally {
            apkFile.delete()
        }
    }

    private fun copyUriToCache(
        uri: Uri
    ): File {

        val file = File(
            context.cacheDir,
            "apvm_scan_${System.currentTimeMillis()}.apk"
        )

        val inputStream =
            context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException(
                    "Unable to open selected APK"
                )

        inputStream.use { input ->

            file.outputStream().use { output ->

                input.copyTo(output)
            }
        }

        return file
    }
}