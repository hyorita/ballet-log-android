package com.hyorita.balletlog.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

object PhotoManager {

    private fun photosDir(context: Context): File {
        return File(context.filesDir, "photos").also { it.mkdirs() }
    }

    // URI → 앱 내부 저장소에 복사, 파일명 반환
    fun savePhoto(context: Context, uri: Uri): String? {
        return try {
            val fileName = "photo_${UUID.randomUUID()}.jpg"
            val dest = File(photosDir(context), fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            fileName
        } catch (e: Exception) {
            null
        }
    }

    // 파일명 → File 객체
    fun getPhotoFile(context: Context, fileName: String): File {
        return File(photosDir(context), fileName)
    }

    // 파일 삭제
    fun deletePhoto(context: Context, fileName: String) {
        File(photosDir(context), fileName).delete()
    }
}
