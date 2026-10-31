package com.ste03.smartscan.vm

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ste03.smartscan.core.Storage
import com.ste03.smartscan.data.AppDatabase
import com.ste03.smartscan.data.Document
import com.ste03.smartscan.data.DocumentSummary
import com.ste03.smartscan.data.Folder
import com.ste03.smartscan.data.Page
import com.ste03.smartscan.export.ImageExporter
import com.ste03.smartscan.export.PdfExporter
import com.ste03.smartscan.lock.LockManager
import com.ste03.smartscan.naming.SmartNamer
import com.ste03.smartscan.ocr.BarcodeScanner
import com.ste03.smartscan.ocr.OcrEngine
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.get(application)
    private val context = application
    val lockManager = LockManager(application)

    private val themePrefs = application.getSharedPreferences("smartscan_theme", android.content.Context.MODE_PRIVATE)
    private val _themeName = MutableStateFlow(
        themePrefs.getString("theme_name", "Ability") ?: "Ability"
    )
    val themeName: StateFlow<String> = _themeName

    fun setTheme(name: String) {
        themePrefs.edit().putString("theme_name", name).apply()
        _themeName.value = name
    }

    val folders: StateFlow<List<Folder>> = db.folderDao().all()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val rootSummaries: StateFlow<List<DocumentSummary>> = db.documentDao().summaries(null)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _folderSummaries = MutableStateFlow<List<DocumentSummary>>(emptyList())
    val folderSummaries: StateFlow<List<DocumentSummary>> = _folderSummaries

    private val _pages = MutableStateFlow<List<Page>>(emptyList())
    val pages: StateFlow<List<Page>> = _pages

    private val _currentDocument = MutableStateFlow<Document?>(null)
    val currentDocument: StateFlow<Document?> = _currentDocument

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private var folderDocsJob: Job? = null
    private var pagesJob: Job? = null

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _barcodeResult = MutableStateFlow<List<String>?>(null)
    val barcodeResult: StateFlow<List<String>?> = _barcodeResult

    private val _cameraBatch = MutableStateFlow<List<File>>(emptyList())
    val cameraBatch: StateFlow<List<File>> = _cameraBatch

    private val _cropSource = MutableStateFlow<File?>(null)
    val cropSource: StateFlow<File?> = _cropSource

    private val _cropMode = MutableStateFlow("capture")
    val cropMode: StateFlow<String> = _cropMode

    private var cropPage: Page? = null

    fun consumeMessage() {
        _message.value = null
    }

    fun showMessage(msg: String) {
        _message.value = msg
    }

    // ---- Folders ----

    fun createFolder(name: String) {
        viewModelScope.launch {
            db.folderDao().insert(Folder(name = name))
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            db.documentDao().moveToRoot(folder.id)
            db.folderDao().delete(folder)
        }
    }

    fun renameFolder(folder: Folder, name: String) {
        viewModelScope.launch {
            db.folderDao().update(folder.copy(name = name))
        }
    }

    // ---- Documents ----

    fun openFolder(folderId: Long) {
        folderDocsJob?.cancel()
        _folderSummaries.value = emptyList()
        folderDocsJob = viewModelScope.launch {
            db.documentDao().summaries(folderId).collect {
                _folderSummaries.value = it
            }
        }
    }

    fun openDocument(docId: Long) {
        pagesJob?.cancel()
        _pages.value = emptyList()
        viewModelScope.launch {
            db.documentDao().byId(docId)?.let { _currentDocument.value = it }
        }
        pagesJob = viewModelScope.launch {
            db.pageDao().byDocument(docId).collect {
                _pages.value = it
            }
        }
    }

    fun clearDocument() {
        _currentDocument.value = null
        _pages.value = emptyList()
    }

    /** Import scanned page URIs as a new document; OCR + smart-name in background. */
    fun importPages(uris: List<Uri>, folderId: Long?) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _busy.value = true
            try {
                val docId = db.documentDao().insert(
                    Document(folderId = folderId, name = SmartNamer.defaultName())
                )
                var order = 0
                for (uri in uris) {
                    val fileName = Storage.importPage(context, uri)
                    db.pageDao().insert(
                        Page(documentId = docId, fileName = fileName, sortOrder = order++)
                    )
                }
                smartName(docId)
            } catch (e: Exception) {
                _message.value = "Could not save scan: ${e.message}"
            } finally {
                _busy.value = false
            }
        }
    }

    private suspend fun smartName(docId: Long) {
        val first = db.pageDao().byDocumentOnce(docId).firstOrNull() ?: return
        runCatching {
            val text = OcrEngine.recognize(Storage.pageFile(context, first.fileName))
            db.pageDao().setOcrText(first.id, text)
            SmartNamer.fromOcr(text)?.let { name ->
                val doc = db.documentDao().byId(docId) ?: return@let
                db.documentDao().update(doc.copy(name = name))
            }
        }
    }

    fun renameDocument(doc: Document, name: String) {
        viewModelScope.launch {
            db.documentDao().update(doc.copy(name = name, updatedAt = System.currentTimeMillis()))
        }
    }

    fun renameDocumentById(id: Long, name: String) {
        viewModelScope.launch {
            val doc = db.documentDao().byId(id) ?: return@launch
            db.documentDao().update(doc.copy(name = name, updatedAt = System.currentTimeMillis()))
        }
    }

    fun moveDocumentById(id: Long, folderId: Long?) {
        viewModelScope.launch {
            val doc = db.documentDao().byId(id) ?: return@launch
            db.documentDao().update(doc.copy(folderId = folderId))
        }
    }

    fun deleteDocumentById(id: Long) {
        viewModelScope.launch {
            val pages = db.pageDao().byDocumentOnce(id)
            Storage.deleteAll(context, pages.map { it.fileName })
            db.pageDao().deleteForDocument(id)
            db.documentDao().byId(id)?.let { db.documentDao().delete(it) }
        }
    }

    fun deleteDocumentsByIds(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { id ->
                val pages = db.pageDao().byDocumentOnce(id)
                Storage.deleteAll(context, pages.map { it.fileName })
                db.pageDao().deleteForDocument(id)
                db.documentDao().byId(id)?.let { db.documentDao().delete(it) }
            }
        }
    }

    fun deleteDocument(doc: Document) {
        viewModelScope.launch {
            val pages = db.pageDao().byDocumentOnce(doc.id)
            Storage.deleteAll(context, pages.map { it.fileName })
            db.pageDao().deleteForDocument(doc.id)
            db.documentDao().delete(doc)
        }
    }

    fun moveDocument(doc: Document, folderId: Long?) {
        viewModelScope.launch {
            db.documentDao().update(doc.copy(folderId = folderId))
        }
    }

    // ---- Pages ----

    fun addPages(docId: Long, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val maxOrder = db.pageDao().byDocumentOnce(docId).maxOfOrNull { it.sortOrder } ?: -1
            var order = maxOrder + 1
            for (uri in uris) {
                val fileName = Storage.importPage(context, uri)
                db.pageDao().insert(
                    Page(documentId = docId, fileName = fileName, sortOrder = order++)
                )
            }
        }
    }

    /** Save a batch of already-processed page files from the camera screen. */
    fun startCameraSession() {
        _cameraBatch.value = emptyList()
    }

    fun beginCrop(source: File, mode: String, page: Page?) {
        cropPage = page
        _cropMode.value = mode
        _cropSource.value = source
    }

    fun clearCrop() {
        cropPage = null
        _cropSource.value?.takeIf { it.name.startsWith("capture_") }?.delete()
        _cropSource.value = null
    }

    /** Apply a cropped/edited bitmap: capture -> append to batch; edit -> replace the page file. */
    fun finishCrop(result: Bitmap) {
        val mode = _cropMode.value
        val page = cropPage
        val source = _cropSource.value
        cropPage = null
        viewModelScope.launch(Dispatchers.IO) {
            if (mode == "edit" && page != null) {
                val oldFile = Storage.pageFile(context, page.fileName)
                val newFile = Storage.savePageBitmap(context, result)
                db.pageDao().update(page.copy(fileName = newFile.name))
                oldFile.delete()
            } else {
                val saved = Storage.savePageBitmap(context, result)
                _cameraBatch.value = _cameraBatch.value + saved
                source?.takeIf { it.name.startsWith("capture_") }?.delete()
            }
            _cropSource.value = null
        }
    }

    fun saveCameraBatch(folderId: Long?, appendTo: Long?) {
        val files = _cameraBatch.value
        if (files.isEmpty()) return
        viewModelScope.launch {
            _busy.value = true
            try {
                if (appendTo != null) {
                    appendFilesToDocument(appendTo, files)
                } else {
                    createDocumentFromFiles(files, folderId)
                }
                _cameraBatch.value = emptyList()
            } finally {
                _busy.value = false
            }
        }
    }

    private suspend fun createDocumentFromFiles(files: List<File>, folderId: Long?) {
        val docId = db.documentDao().insert(
            Document(folderId = folderId, name = SmartNamer.defaultName())
        )
        files.forEachIndexed { i, f ->
            db.pageDao().insert(Page(documentId = docId, fileName = f.name, sortOrder = i))
        }
        smartName(docId)
    }

    private suspend fun appendFilesToDocument(docId: Long, files: List<File>) {
        val maxOrder = db.pageDao().byDocumentOnce(docId).maxOfOrNull { it.sortOrder } ?: -1
        files.forEachIndexed { i, f ->
            db.pageDao().insert(
                Page(documentId = docId, fileName = f.name, sortOrder = maxOrder + 1 + i)
            )
        }
        db.documentDao().byId(docId)?.let {
            db.documentDao().update(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deletePage(page: Page) {
        viewModelScope.launch {
            Storage.deletePage(context, page.fileName)
            db.pageDao().delete(page)
        }
    }

    fun reorderPages(pages: List<Page>) {
        viewModelScope.launch {
            pages.forEachIndexed { index, page ->
                db.pageDao().setOrder(page.id, index)
            }
        }
    }

    fun pageFile(page: Page): File = Storage.pageFile(context, page.fileName)

    /** Rotate a page in place (persists into a new file so the UI cache is invalidated). */
    fun rotatePage(page: Page, degrees: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = Storage.pageFile(context, page.fileName)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@launch
            val matrix = Matrix().apply { postRotate(degrees) }
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            if (rotated !== bitmap) bitmap.recycle()
            val newFile = Storage.savePageBitmap(context, rotated)
            rotated.recycle()
            db.pageDao().update(page.copy(fileName = newFile.name))
            file.delete()
        }
    }

    /** Append all pages of [sourceId] onto the end of [targetId], then delete the source. */
    fun mergeDocuments(targetId: Long, sourceId: Long) {
        if (targetId == sourceId) return
        viewModelScope.launch {
            val targetMax = db.pageDao().byDocumentOnce(targetId).maxOfOrNull { it.sortOrder } ?: -1
            val sourcePages = db.pageDao().byDocumentOnce(sourceId).sortedBy { it.sortOrder }
            sourcePages.forEachIndexed { i, p ->
                db.pageDao().update(p.copy(documentId = targetId, sortOrder = targetMax + 1 + i))
            }
            db.documentDao().byId(sourceId)?.let { db.documentDao().delete(it) }
            db.documentDao().byId(targetId)?.let {
                db.documentDao().update(it.copy(updatedAt = System.currentTimeMillis()))
            }
        }
    }

    /** Split a document: pages from [fromIndex] onward move to a new document. */
    fun splitDocument(docId: Long, fromIndex: Int) {
        viewModelScope.launch {
            val pages = db.pageDao().byDocumentOnce(docId).sortedBy { it.sortOrder }
            if (fromIndex <= 0 || fromIndex >= pages.size) return@launch
            val doc = db.documentDao().byId(docId) ?: return@launch
            val newDocId = db.documentDao().insert(
                Document(folderId = doc.folderId, name = doc.name + " (2)")
            )
            pages.drop(fromIndex).forEachIndexed { i, p ->
                db.pageDao().update(p.copy(documentId = newDocId, sortOrder = i))
            }
            db.documentDao().update(doc.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    // ---- Export ----

    suspend fun exportPdf(docId: Long, password: String?): File = withContext(Dispatchers.IO) {
        val doc = db.documentDao().byId(docId) ?: error("Document not found")
        val files = db.pageDao().byDocumentOnce(docId).map { Storage.pageFile(context, it.fileName) }
        PdfExporter.export(context, files, doc.name, password)
    }

    suspend fun exportImages(docId: Long): File = withContext(Dispatchers.IO) {
        val doc = db.documentDao().byId(docId) ?: error("Document not found")
        val files = db.pageDao().byDocumentOnce(docId).map { Storage.pageFile(context, it.fileName) }
        ImageExporter.exportAsZip(context, files, doc.name)
    }

    suspend fun exportPng(docId: Long): File = withContext(Dispatchers.IO) {
        val doc = db.documentDao().byId(docId) ?: error("Document not found")
        val files = db.pageDao().byDocumentOnce(docId).map { Storage.pageFile(context, it.fileName) }
        ImageExporter.exportAsPngZip(context, files, doc.name)
    }

    fun shareUri(file: File): Uri = ImageExporter.shareUri(context, file)

    suspend fun allDocumentsExcept(excludeId: Long): List<Document> =
        db.documentDao().allExcept(excludeId)

    // ---- Barcode ----

    fun clearBarcodeResult() {
        _barcodeResult.value = null
    }

    /** Scan a captured page for barcodes and surface the values (does not save a document). */
    fun scanBarcode(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _busy.value = true
            try {
                val fileName = Storage.importPage(context, uris.first())
                val results = BarcodeScanner.recognize(Storage.pageFile(context, fileName))
                Storage.deletePage(context, fileName)
                _barcodeResult.value = results
            } catch (e: Exception) {
                _barcodeResult.value = emptyList()
            } finally {
                _busy.value = false
            }
        }
    }
}
