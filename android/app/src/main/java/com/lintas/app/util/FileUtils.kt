package com.lintas.app.util

import android.webkit.MimeTypeMap

object FileUtils {
    fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes < 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = sizeInBytes.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }
        return if (unitIndex == 0) {
            "${value.toInt()} ${units[unitIndex]}"
        } else {
            "%.1f ${units[unitIndex]}".format(value)
        }
    }

    fun getFileType(mimeType: String?): String {
        if (mimeType == null) return "Unknown"
        return when {
            mimeType.startsWith("image/") -> "Image"
            mimeType.startsWith("video/") -> "Video"
            mimeType.startsWith("audio/") -> "Audio"
            mimeType == "application/pdf" -> "PDF"
            mimeType.startsWith("text/") -> "Text"
            mimeType.contains("zip") || mimeType.contains("rar") || mimeType.contains("tar") -> "Archive"
            mimeType.contains("sheet") || mimeType.contains("excel") -> "Spreadsheet"
            mimeType.contains("document") || mimeType.contains("word") -> "Document"
            mimeType.contains("presentation") || mimeType.contains("powerpoint") -> "Presentation"
            else -> "File"
        }
    }

    fun getMimeTypeFromFileName(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }
}
