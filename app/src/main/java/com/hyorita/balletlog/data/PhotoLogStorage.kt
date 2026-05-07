package com.hyorita.balletlog.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/**
 * Local-only photo file storage for the PhotoLog feature.
 *
 * Mirrors iOS `PhotoLogStorage`: photos live as plain JPEGs under a dedicated
 * directory (`filesDir/photolog/`). Originals and filtered outputs are kept
 * as separate files so the unfiltered original can always be recovered.
 */
object PhotoLogStorage {

    private const val DIR_NAME = "photolog"

    fun directory(context: Context): File =
        File(context.filesDir, DIR_NAME).also { it.mkdirs() }

    fun fileFor(context: Context, name: String): File =
        File(directory(context), name)

    fun exists(context: Context, name: String?): Boolean {
        if (name.isNullOrEmpty()) return false
        return fileFor(context, name).exists()
    }

    /** Copy a content URI into PhotoLog storage as a JPEG. Returns the stored file name. */
    fun saveFromUri(context: Context, uri: Uri): String? {
        val name = "${UUID.randomUUID()}.jpg"
        val dest = fileFor(context, name)
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            name
        }.getOrNull()
    }

    /** Persist a bitmap to PhotoLog storage as JPEG (q=85). Returns the stored file name. */
    fun saveBitmap(context: Context, bitmap: Bitmap, suffix: String = ""): String? {
        val name = "${UUID.randomUUID()}$suffix.jpg"
        val dest = fileFor(context, name)
        return runCatching {
            dest.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            name
        }.getOrNull()
    }

    /**
     * Best-effort capture-time lookup for a picked image. Tries MediaStore
     * (`DATE_TAKEN` → `DATE_ADDED`) first, then EXIF on the saved file.
     * Returns null if no reliable timestamp can be recovered.
     */
    fun extractTakenDate(context: Context, uri: Uri, savedFileName: String? = null): Long? {
        // 1) MediaStore — most reliable, includes capture time before any copy
        runCatching {
            val proj = arrayOf(
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATE_ADDED
            )
            context.contentResolver.query(uri, proj, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val takenIdx = c.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                    if (takenIdx != -1 && !c.isNull(takenIdx)) {
                        val t = c.getLong(takenIdx)
                        if (t > 0) return t
                    }
                    val addedIdx = c.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                    if (addedIdx != -1 && !c.isNull(addedIdx)) {
                        val a = c.getLong(addedIdx)
                        if (a > 0) return a * 1000  // seconds → millis
                    }
                }
            }
        }

        // 2) EXIF fallback on the file we just wrote
        if (savedFileName != null) {
            runCatching {
                val file = fileFor(context, savedFileName)
                val exif = ExifInterface(file.absolutePath)
                val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                raw?.let {
                    SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(it)?.time
                }
            }.getOrNull()?.let { return it }
        }

        return null
    }

    fun delete(context: Context, name: String?) {
        if (name.isNullOrEmpty()) return
        fileFor(context, name).delete()
    }

    /** Decode an oriented bitmap from a stored PhotoLog file. */
    fun loadBitmap(context: Context, name: String?): Bitmap? {
        if (name.isNullOrEmpty()) return null
        val file = fileFor(context, name)
        if (!file.exists()) return null
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        return applyExifOrientation(file, bitmap)
    }

    private fun applyExifOrientation(file: File, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        if (orientation == ExifInterface.ORIENTATION_NORMAL) return bitmap
        val matrix = android.graphics.Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
