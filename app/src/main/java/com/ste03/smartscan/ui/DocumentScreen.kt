package com.ste03.smartscan.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ste03.smartscan.core.Storage
import com.ste03.smartscan.data.Document
import com.ste03.smartscan.data.Page
import com.ste03.smartscan.vm.AppViewModel
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreen(
    viewModel: AppViewModel,
    docId: Long,
    onBack: () -> Unit,
    onAddPages: () -> Unit,
    onEditPage: (Page) -> Unit
) {
    val pages by viewModel.pages.collectAsStateWithLifecycle()
    val doc by viewModel.currentDocument.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showExport by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }
    var showMerge by remember { mutableStateOf(false) }
    var confirmSplit by remember { mutableStateOf(false) }

    LaunchedEffect(docId) { viewModel.openDocument(docId) }

    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

    fun shareFile(file: File, mimeType: String) {
        val uri = viewModel.shareUri(file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share"))
    }

    fun exportPdf(password: String?) {
        exporting = true
        scope.launch {
            try {
                shareFile(viewModel.exportPdf(docId, password), "application/pdf")
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                exporting = false
            }
        }
    }

    fun exportImages(png: Boolean) {
        exporting = true
        scope.launch {
            try {
                val file = if (png) viewModel.exportPng(docId) else viewModel.exportImages(docId)
                shareFile(file, "application/zip")
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                exporting = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(doc?.name ?: "Document", fontWeight = FontWeight.SemiBold, maxLines = 1)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { renaming = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Rename")
                    }
                    IconButton(onClick = { showText = true }) {
                        Icon(Icons.Filled.TextSnippet, contentDescription = "Text")
                    }
                    IconButton(onClick = { showExport = true }) {
                        Icon(Icons.Filled.Share, contentDescription = "Export")
                    }
                    var moreOpen by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { moreOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Merge with…") },
                                leadingIcon = { Icon(Icons.Filled.MergeType, null) },
                                onClick = { moreOpen = false; showMerge = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Split here") },
                                leadingIcon = { Icon(Icons.Filled.CallSplit, null) },
                                onClick = { moreOpen = false; confirmSplit = true }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (pages.isNotEmpty()) {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onAddPages) {
                            Icon(Icons.Filled.Add, contentDescription = "Add pages")
                        }
                        IconButton(
                            onClick = {
                                pages.getOrNull(pagerState.currentPage)?.let {
                                    viewModel.rotatePage(it, 90f)
                                }
                            }
                        ) {
                            Icon(Icons.Filled.RotateRight, contentDescription = "Rotate page")
                        }
                        IconButton(
                            onClick = {
                                pages.getOrNull(pagerState.currentPage)?.let { onEditPage(it) }
                            }
                        ) {
                            Icon(Icons.Filled.Crop, contentDescription = "Crop / edit page")
                        }
                        Text(
                            "${pagerState.currentPage + 1} / ${pages.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = {
                                pages.getOrNull(pagerState.currentPage)?.let {
                                    viewModel.deletePage(it)
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete page")
                        }
                        IconButton(
                            onClick = {
                                pages.getOrNull(pagerState.currentPage)?.let {
                                    shareFile(viewModel.pageFile(it), "image/jpeg")
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share page")
                        }
                    }
                    PageStrip(
                        pages = pages,
                        currentIndex = pagerState.currentPage,
                        onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                        onReorder = { reordered -> viewModel.reorderPages(reordered) }
                    )
                }
            }
        }
    ) { padding ->
        if (pages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No pages",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onAddPages) { Text("Add pages") }
                }
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().padding(padding)
            ) { index ->
                val page = pages[index]
                AsyncImage(
                    model = viewModel.pageFile(page),
                    contentDescription = "Page ${index + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    if (showExport) {
        AlertDialog(
            onDismissRequest = { showExport = false },
            title = { Text("Export document") },
            text = {
                Column {
                    MenuRow(Icons.Filled.PictureAsPdf, "PDF") {
                        showExport = false; exportPdf(null)
                    }
                    MenuRow(Icons.Filled.Lock, "PDF (password protected)") {
                        showExport = false; showPassword = true
                    }
                    MenuRow(Icons.Filled.Share, "Images — JPEG (ZIP)") {
                        showExport = false; exportImages(png = false)
                    }
                    MenuRow(Icons.Filled.Share, "Images — PNG (ZIP)") {
                        showExport = false; exportImages(png = true)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showExport = false }) { Text("Cancel") } }
        )
    }

    if (showPassword) {
        var pw by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPassword = false },
            title = { Text("Protect PDF with a password") },
            text = {
                OutlinedTextField(
                    value = pw,
                    onValueChange = { pw = it },
                    singleLine = true,
                    label = { Text("Password") }
                )
            },
            confirmButton = {
                Button(
                    onClick = { showPassword = false; exportPdf(pw.ifBlank { null }) },
                    enabled = !exporting
                ) { Text("Export") }
            },
            dismissButton = { TextButton(onClick = { showPassword = false }) { Text("Cancel") } }
        )
    }

    if (renaming) {
        NameDialog(
            title = "Rename document",
            initial = doc?.name ?: "",
            confirmLabel = "Rename",
            onDismiss = { renaming = false },
            onConfirm = { name ->
                if (name.isNotBlank()) viewModel.renameDocumentById(docId, name)
                renaming = false
            }
        )
    }

    if (showMerge) {
        MergeDialog(
            viewModel = viewModel,
            currentDocId = docId,
            onDismiss = { showMerge = false },
            onMerge = { otherId ->
                viewModel.mergeDocuments(docId, otherId)
                showMerge = false
            }
        )
    }

    if (confirmSplit) {
        AlertDialog(
            onDismissRequest = { confirmSplit = false },
            title = { Text("Split document?") },
            text = {
                Text(
                    "Pages from ${pagerState.currentPage + 1} onward will move to a new document. " +
                        "This document keeps pages 1–${pagerState.currentPage}."
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.splitDocument(docId, pagerState.currentPage)
                    confirmSplit = false
                }) { Text("Split") }
            },
            dismissButton = { TextButton(onClick = { confirmSplit = false }) { Text("Cancel") } }
        )
    }

    if (showText) {
        val text = pages.joinToString("\n\n") { it.ocrText ?: "" }.trim()
        AlertDialog(
            onDismissRequest = { showText = false },
            title = { Text("Recognised text") },
            text = {
                Text(
                    text.ifBlank { "No text recognised. Text recognition runs when you scan." },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as android.content.ClipboardManager
                        clipboard.setPrimaryClip(
                            android.content.ClipData.newPlainText("Smart Scan", text)
                        )
                        showText = false
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    enabled = text.isNotBlank()
                ) { Text("Copy") }
            },
            dismissButton = { TextButton(onClick = { showText = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun MergeDialog(
    viewModel: AppViewModel,
    currentDocId: Long,
    onDismiss: () -> Unit,
    onMerge: (Long) -> Unit
) {
    var others by remember { mutableStateOf<List<Document>>(emptyList()) }
    LaunchedEffect(Unit) {
        others = viewModel.allDocumentsExcept(currentDocId)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge with…") },
        text = {
            if (others.isEmpty()) {
                Text("No other documents to merge with.")
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    others.forEach { d ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMerge(d.id) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(d.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PageStrip(
    pages: List<Page>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onReorder: (List<Page>) -> Unit
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val itemWidthPx = with(density) { 48.dp.toPx() }
    val listState = rememberLazyListState()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        itemsIndexed(pages, key = { _, p -> p.id }) { index, page ->
            val isDragging = draggingIndex == index
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(48.dp)
                    .graphicsLayer {
                        if (isDragging) {
                            translationX = dragOffset
                            scaleX = 1.15f
                            scaleY = 1.15f
                        }
                    }
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = if (index == currentIndex) 2.dp else 1.dp,
                        color = if (index == currentIndex)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelect(index) }
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { draggingIndex = index; dragOffset = 0f },
                            onDragEnd = {
                                val target = (index + (dragOffset / itemWidthPx).roundToInt())
                                    .coerceIn(0, pages.size - 1)
                                if (target != index) {
                                    val newOrder = pages.toMutableList()
                                    val item = newOrder.removeAt(index)
                                    newOrder.add(target, item)
                                    onReorder(newOrder)
                                }
                                draggingIndex = null
                                dragOffset = 0f
                            },
                            onDragCancel = { draggingIndex = null; dragOffset = 0f },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.x
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = Storage.pageFile(context, page.fileName),
                    contentDescription = "Page ${index + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            RoundedCornerShape(3.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}
