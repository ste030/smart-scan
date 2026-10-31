package com.ste03.smartscan.naming

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Smart document naming: derive a human name from OCR text, or fall back to a
 * date-stamped default.
 */
object SmartNamer {

    fun defaultName(): String {
        val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return "Scan ${fmt.format(Date())}"
    }

    /** First meaningful OCR line, cleaned and trimmed to a sane length. */
    fun fromOcr(ocrText: String?): String? {
        if (ocrText.isNullOrBlank()) return null
        val lines = ocrText
            .lineSequence()
            .map { it.trim() }
            .filter { it.length >= 2 }
            .filterNot { it.matches(Regex("[\\W_]+")) }
            .toList()
        if (lines.isEmpty()) return null
        var candidate = lines.first()
        // Cap at ~60 chars and cut at a sentence/word boundary.
        if (candidate.length > 60) {
            candidate = candidate.take(60).substringBeforeLast(' ')
        }
        return candidate.ifBlank { null }
    }
}
