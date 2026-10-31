package com.ste03.smartscan.export

import android.content.Context
import com.ste03.smartscan.core.Storage
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import java.io.File
import java.io.FileOutputStream

/**
 * Builds a multi-page PDF from stored page images, embedding the JPEGs directly
 * (no re-encode, small output). Optionally password-protects the PDF (AES-128).
 */
object PdfExporter {

    private fun ensureInit(context: Context) {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    fun export(
        context: Context,
        pageFiles: List<File>,
        docName: String,
        password: String? = null
    ): File {
        ensureInit(context)
        val document = PDDocument()
        try {
            for (file in pageFiles) {
                val image = file.inputStream().use {
                    JPEGFactory.createFromStream(document, it)
                }
                val page = PDPage(
                    PDRectangle(image.width.toFloat(), image.height.toFloat())
                )
                document.addPage(page)
                PDPageContentStream(document, page).use { cs ->
                    cs.drawImage(
                        image,
                        0f, 0f,
                        image.width.toFloat(), image.height.toFloat()
                    )
                }
            }

            if (!password.isNullOrBlank()) {
                val policy = StandardProtectionPolicy(password, password, AccessPermission())
                policy.encryptionKeyLength = 128
                document.protect(policy)
            }

            val out = Storage.newPdfFile(context, docName)
            FileOutputStream(out).use { document.save(it) }
            return out
        } finally {
            document.close()
        }
    }
}
