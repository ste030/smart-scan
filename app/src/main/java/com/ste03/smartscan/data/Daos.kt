package com.ste03.smartscan.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE ASC")
    fun all(): Flow<List<Folder>>

    @Insert
    suspend fun insert(folder: Folder): Long

    @Update
    suspend fun update(folder: Folder)

    @Delete
    suspend fun delete(folder: Folder)

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun byId(id: Long): Folder?
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE folderId IS NULL ORDER BY updatedAt DESC")
    fun inRoot(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE folderId = :folderId ORDER BY updatedAt DESC")
    fun inFolder(folderId: Long): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE name LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun search(query: String): Flow<List<Document>>

    @Insert
    suspend fun insert(doc: Document): Long

    @Update
    suspend fun update(doc: Document)

    @Delete
    suspend fun delete(doc: Document)

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun byId(id: Long): Document?

    @Query("SELECT * FROM documents WHERE id != :excludeId ORDER BY updatedAt DESC")
    suspend fun allExcept(excludeId: Long): List<Document>

    @Query("UPDATE documents SET folderId = NULL WHERE folderId = :folderId")
    suspend fun moveToRoot(folderId: Long)

    @Query(
        """
        SELECT d.id AS id, d.folderId AS folderId, d.name AS name,
               d.createdAt AS createdAt, d.updatedAt AS updatedAt,
               (SELECT COUNT(*) FROM pages p WHERE p.documentId = d.id) AS pageCount,
               (SELECT p.fileName FROM pages p WHERE p.documentId = d.id ORDER BY p.sortOrder ASC LIMIT 1) AS firstPage
        FROM documents d
        WHERE (:folderId IS NULL AND d.folderId IS NULL) OR d.folderId = :folderId
        ORDER BY d.updatedAt DESC
        """
    )
    fun summaries(folderId: Long?): Flow<List<DocumentSummary>>
}

@Dao
interface PageDao {
    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY sortOrder ASC")
    fun byDocument(documentId: Long): Flow<List<Page>>

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY sortOrder ASC")
    suspend fun byDocumentOnce(documentId: Long): List<Page>

    @Insert
    suspend fun insert(page: Page): Long

    @Insert
    suspend fun insertAll(pages: List<Page>): List<Long>

    @Update
    suspend fun update(page: Page)

    @Delete
    suspend fun delete(page: Page)

    @Query("DELETE FROM pages WHERE documentId = :documentId")
    suspend fun deleteForDocument(documentId: Long)

    @Query("SELECT COUNT(*) FROM pages WHERE documentId = :documentId")
    suspend fun countForDocument(documentId: Long): Int

    @Query("UPDATE pages SET sortOrder = :order WHERE id = :id")
    suspend fun setOrder(id: Long, order: Int)

    @Query("UPDATE pages SET ocrText = :text WHERE id = :id")
    suspend fun setOcrText(id: Long, text: String?)
}
