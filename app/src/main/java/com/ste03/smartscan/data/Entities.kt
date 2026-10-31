package com.ste03.smartscan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long? = null,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "pages")
data class Page(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val fileName: String,
    val sortOrder: Int = 0,
    val ocrText: String? = null
)

/** Non-entity projection used by the document list: doc + page count + first-page thumbnail. */
data class DocumentSummary(
    val id: Long,
    val folderId: Long?,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pageCount: Int,
    val firstPage: String?
)
