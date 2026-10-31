package com.ste03.smartscan.core

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Persists page images into internal storage and resolves their paths. */
object Storage {
    private const val PAGES_DIR = "pages"

    fun pagesDir(context: Context): File =
        File(context.filesDir, PAGES_DIR).apply { mkdirs() }

    fun pageFile(context: Context, fileName: String): File =
        File(pagesDir(context), fileName)

    /** Save a captured/processed bitmap as a page image; returns the stored File. */
    fun savePageBitmap(context: Context, bitmap: android.graphics.Bitmap): File {
        val file = pageFile(context, "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
        return file
    }

    /** Copy a scanned page URI into internal storage; returns the stored file name. */
    fun importPage(context: Context, uri: Uri): String {
        val name = "${UUID.randomUUID()}.jpg"
        val out = pageFile(context, name)
        context.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Cannot read scanned page: $uri")
        return name
    }

    fun deletePage(context: Context, fileName: String) {
        pageFile(context, fileName).takeIf { it.exists() }?.delete()
    }

    /** Delete every page file for a document (called before deleting the doc row). */
    fun deleteAll(context: Context, fileNames: List<String>) {
        fileNames.forEach { deletePage(context, it) }
    }

    fun newPdfFile(context: Context, name: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safe = name.replace(Regex("[^A-Za-z0-9 _-]"), "").ifBlank { "document" }
        return File(dir, "$safe.pdf")
    }

    fun newZipFile(context: Context, name: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safe = name.replace(Regex("[^A-Za-z0-9 _-]"), "").ifBlank { "document" }
        return File(dir, "$safe-images.zip")
    }

    fun newPngZipFile(context: Context, name: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safe = name.replace(Regex("[^A-Za-z0-9 _-]"), "").ifBlank { "document" }
        return File(dir, "$safe-png.zip")
    }
}
