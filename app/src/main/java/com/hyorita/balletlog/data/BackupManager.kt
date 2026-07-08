package com.hyorita.balletlog.data

import android.content.Context
import android.net.Uri
import com.hyorita.balletlog.data.db.BalletLogDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * ZIP-based backup / restore for the Room database (`ballet_log_db` + WAL/SHM)
 * and the two photo directories (ClassLog `photos/`, PhotoLog `photolog/`).
 *
 * The output file uses a `.bblbackup` extension so it round-trips with iOS
 * naming. Internally it's a standard ZIP, so users can also rename to .zip
 * and inspect contents if needed.
 *
 * Layout:
 *   manifest.json     — version + creation timestamp
 *   db/<files>        — Room SQLite + WAL + SHM
 *   photos/<files>    — ClassLog/Note attached photos
 *   photolog/<files>  — PhotoLog photos
 */
object BackupManager {

    private const val DB_NAME = "ballet_log_db"
    private const val MANIFEST = "manifest.json"
    private const val BACKUP_VERSION = 1

    class InvalidArchiveException : IOException("Selected file is not a valid Ballet Log backup")

    /**
     * Suggested filename — `BalletLog_2026-05-07T17-30-00.bblbackup` style.
     * iOS uses ISO8601 with `:` replaced by `-`; we mirror that.
     */
    fun suggestedFileName(): String {
        val ts = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", java.util.Locale.US)
            .format(java.util.Date())
        return "BalletLog_$ts.bblbackup"
    }

    suspend fun exportToUri(context: Context, dest: Uri) {
        withContext(Dispatchers.IO) {
            val db = BalletLogDatabase.getInstance(context)

            // Best-effort: fold WAL frames into the main file. A FULL checkpoint
            // can't complete while the running app holds read connections open
            // (Room's Flow observers), so this alone does NOT guarantee the main
            // file is self-contained — hence the snapshot below.
            runCatching {
                db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
            }

            // Preferred path: VACUUM INTO writes a single, fully self-contained
            // copy of the database — including committed rows still living only
            // in the -wal — into one file, without closing the live connection.
            // This is what keeps a class log added right before export from being
            // missing from the backup. VACUUM INTO needs SQLite 3.27+ (Android
            // API 29+); older devices fall back to copying db + wal + shm together
            // (also loss-free, since restore copies every db file back).
            val snapshot = File(context.cacheDir, "backup_snapshot_${UUID.randomUUID()}.db")
            val snapshotOk = runCatching {
                db.openHelper.writableDatabase.execSQL("VACUUM INTO ?", arrayOf(snapshot.absolutePath))
                snapshot.exists() && snapshot.length() > 0
            }.getOrDefault(false)

            try {
                val out = context.contentResolver.openOutputStream(dest)
                    ?: throw IOException("Failed to open destination")

                ZipOutputStream(out).use { zip ->
                    // Manifest
                    zip.putEntryAndWrite(MANIFEST) {
                        write(
                            ("{\"version\":$BACKUP_VERSION," +
                                "\"timestamp\":${System.currentTimeMillis()}," +
                                "\"app\":\"balletlog-android\"}").toByteArray()
                        )
                    }

                    // Database — a self-contained snapshot when available,
                    // otherwise the live files (main + -wal + -shm) together.
                    if (snapshotOk) {
                        zip.copyFile(snapshot, "db/$DB_NAME")
                    } else {
                        val dbFile = context.getDatabasePath(DB_NAME)
                        dbFile.parentFile?.listFiles()
                            ?.filter { it.name.startsWith(DB_NAME) }
                            ?.forEach { file -> zip.copyFile(file, "db/${file.name}") }
                    }

                    // ClassLog / Note photos
                    File(context.filesDir, "photos").listFiles()
                        ?.filter { it.isFile }
                        ?.forEach { file -> zip.copyFile(file, "photos/${file.name}") }

                    // PhotoLog photos
                    File(context.filesDir, "photolog").listFiles()
                        ?.filter { it.isFile }
                        ?.forEach { file -> zip.copyFile(file, "photolog/${file.name}") }
                }
            } finally {
                snapshot.delete()
            }
        }
    }

    suspend fun importFromUri(context: Context, src: Uri) {
        withContext(Dispatchers.IO) {
            val staging = File(context.cacheDir, "BalletLogRestore_${UUID.randomUUID()}")
            staging.mkdirs()
            try {
                // 1. Extract to staging
                val input = context.contentResolver.openInputStream(src)
                    ?: throw IOException("Failed to open source")
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val outFile = File(staging, entry.name)
                            // Defense against zip-slip
                            if (!outFile.canonicalPath.startsWith(staging.canonicalPath)) {
                                throw InvalidArchiveException()
                            }
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { os -> zip.copyTo(os) }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }

                // 2. Validate — the archive must contain at least the main DB file
                val stagedDb = File(staging, "db/$DB_NAME")
                if (!stagedDb.exists()) throw InvalidArchiveException()

                // 3. Close Room so files can be safely replaced
                BalletLogDatabase.closeAndReset()

                // 4. Wipe existing on-disk state
                val dbDir = context.getDatabasePath(DB_NAME).parentFile
                dbDir?.listFiles()
                    ?.filter { it.name.startsWith(DB_NAME) }
                    ?.forEach { it.delete() }
                File(context.filesDir, "photos").deleteAllFiles()
                File(context.filesDir, "photolog").deleteAllFiles()

                // 5. Restore from staging
                File(staging, "db").listFiles()?.forEach { srcFile ->
                    val dst = File(dbDir, srcFile.name)
                    srcFile.copyTo(dst, overwrite = true)
                }
                copyDirInto(File(staging, "photos"), File(context.filesDir, "photos"))
                copyDirInto(File(staging, "photolog"), File(context.filesDir, "photolog"))
            } finally {
                staging.deleteRecursively()
            }
        }
    }

    // ----- helpers -----

    private fun ZipOutputStream.copyFile(file: File, entryName: String) {
        putEntryAndWrite(entryName) {
            file.inputStream().use { input -> input.copyTo(this) }
        }
    }

    private inline fun ZipOutputStream.putEntryAndWrite(
        name: String,
        block: ZipOutputStream.() -> Unit
    ) {
        putNextEntry(ZipEntry(name))
        block()
        closeEntry()
    }

    private fun File.deleteAllFiles() {
        if (!exists()) return
        listFiles()?.filter { it.isFile }?.forEach { it.delete() }
    }

    private fun copyDirInto(src: File, dst: File) {
        if (!src.exists()) return
        dst.mkdirs()
        src.listFiles()?.filter { it.isFile }?.forEach { f ->
            f.copyTo(File(dst, f.name), overwrite = true)
        }
    }
}
