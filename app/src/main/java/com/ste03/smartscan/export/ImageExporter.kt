package com.ste03.smartscan.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.ste03.smartscan.core.Storage
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Bundles page images into a zip for share/export. */
object ImageExporter {

    fun exportAsZip(context: Context, pageFiles: List<File>, docName: String): File {
        val zip = Storage.newZipFile(context, docName)
        ZipOutputStream(FileOutputStream(zip)).use { zos ->
            pageFiles.forEachIndexed { i, file ->
                zos.putNextEntry(ZipEntry("page_${i + 1}.jpg"))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        return zip
    }

    fun exportAsPngZip(context: Context, pageFiles: List<File>, docName: String): File {
        val zip = Storage.newPngZipFile(context, docName)
        ZipOutputStream(FileOutputStream(zip)).use { zos ->
            pageFiles.forEachIndexed { i, file ->
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    zos.putNextEntry(ZipEntry("page_${i + 1}.png"))
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, zos)
                    zos.closeEntry()
                    bitmap.recycle()
                }
            }
        }
        return zip
    }

    /** A FileProvider URI for sharing an exported file. */
    fun shareUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
}
