package com.ste03.smartscan.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ste03.smartscan.data.DocumentSummary
import com.ste03.smartscan.data.Folder
import com.ste03.smartscan.ui.components.GridDocumentCard
import com.ste03.smartscan.ui.components.EmptyState
import com.ste03.smartscan.ui.components.FolderCard
import com.ste03.smartscan.vm.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onOpenFolder: (Long) -> Unit,
    onOpenDocument: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onScan: () -> Unit,
    onScanBarcode: () -> Unit
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val summaries by viewModel.rootSummaries.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val barcodeResult by viewModel.barcodeResult.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showNewFolder by remember { mutableStateOf(false) }

    var docMenu by remember { mutableStateOf<DocumentSummary?>(null) }
    var folderMenu by remember { mutableStateOf<Folder?>(null) }

    var renamingDoc by remember { mutableStateOf<DocumentSummary?>(null) }
    var moveTarget by remember { mutableStateOf<DocumentSummary?>(null) }
    var deletingDoc by remember { mutableStateOf<DocumentSummary?>(null) }
    var deletingFolder by remember { mutableStateOf<Folder?>(null) }

    var selecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var confirmDeleteSelected by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
    }

    val filtered = if (query.isBlank()) summaries
    else summaries.filter { it.name.contains(query, ignoreCase = true) }

    var newestFirst by remember { mutableStateOf(true) }
    val sorted = if (newestFirst) filtered.sortedByDescending { it.updatedAt }
    else filtered.sortedBy { it.updatedAt }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                if (selecting) {
                    TopAppBar(
                        title = {
                            Text("${selectedIds.size} selected", fontWeight = FontWeight.SemiBold)
                        },
                        navigationIcon = {
                            IconButton(onClick = { selecting = false; selectedIds = emptySet() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
                            }
                        },
                        actions = {
                            val singleId = selectedIds.firstOrNull()
                            val singleDoc = summaries.firstOrNull { it.id == singleId }
                            if (selectedIds.size == 1 && singleDoc != null) {
                                IconButton(onClick = { renamingDoc = singleDoc }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Rename")
                                }
                                IconButton(onClick = { moveTarget = singleDoc }) {
                                    Icon(Icons.Filled.DriveFileMove, contentDescription = "Move")
                                }
                            }
                            IconButton(
                                onClick = { confirmDeleteSelected = true },
                                enabled = selectedIds.isNotEmpty()
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete selected")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                } else {
                    TopAppBar(
                        title = {
                            if (searching) {
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    placeholder = { Text("Search documents…") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text("Smart Scan", fontWeight = FontWeight.SemiBold)
                            }
                        },
                        navigationIcon = {
                            if (searching) {
                                IconButton(onClick = { searching = false; query = "" }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = onScanBarcode) {
                                Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan barcode")
                            }
                            IconButton(onClick = { searching = true }) {
                                Icon(Icons.Filled.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { showNewFolder = true }) {
                                Icon(Icons.Filled.CreateNewFolder, contentDescription = "New folder")
                            }
                            IconButton(onClick = onOpenSettings) {
                                Icon(Icons.Filled.Settings, contentDescription = "Settings")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                    SortBar(newestFirst = newestFirst, onToggle = { newestFirst = !newestFirst })
                }
            }
        },
        floatingActionButton = {
            if (!selecting) {
                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                val fabScale by animateFloatAsState(
                    targetValue = if (pressed) 0.88f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "fabScale"
                )
                FloatingActionButton(
                    onClick = onScan,
                    shape = RoundedCornerShape(24.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            scaleX = fabScale
                            scaleY = fabScale
                        }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Scan", modifier = Modifier.size(34.dp))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (folders.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Folders",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(folders, key = { it.id }) { folder ->
                            FolderCard(
                                folder = folder,
                                onClick = { onOpenFolder(folder.id) },
                                onLongClick = { folderMenu = folder }
                            )
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Documents",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            if (sorted.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        if (query.isNotBlank()) "No documents match your search"
                        else "No documents yet — tap + to scan"
                    )
                }
            } else {
                gridItems(sorted, key = { it.id }) { summary ->
                    GridDocumentCard(
                        summary = summary,
                        context = context,
                        selected = summary.id in selectedIds,
                        selecting = selecting,
                        onClick = {
                            if (selecting) {
                                val next = if (summary.id in selectedIds) selectedIds - summary.id
                                else selectedIds + summary.id
                                selectedIds = next
                                if (next.isEmpty()) selecting = false
                            } else {
                                onOpenDocument(summary.id)
                            }
                        },
                        onLongClick = {
                            if (!selecting) {
                                selecting = true
                                selectedIds = setOf(summary.id)
                            }
                        }
                    )
                }
            }
        }
    }

    // ---- New folder dialog ----
    if (showNewFolder) {
        NameDialog(
            title = "New folder",
            confirmLabel = "Create",
            onDismiss = { showNewFolder = false },
            onConfirm = { name ->
                if (name.isNotBlank()) viewModel.createFolder(name)
                showNewFolder = false
            }
        )
    }

    // ---- Document long-press menu ----
    docMenu?.let { doc ->
        AlertDialog(
            onDismissRequest = { docMenu = null },
            title = { Text(doc.name, maxLines = 1) },
            text = {
                Column {
                    MenuRow(Icons.Filled.Edit, "Rename") {
                        renamingDoc = doc; docMenu = null
                    }
                    MenuRow(Icons.Filled.DriveFileMove, "Move to…") {
                        moveTarget = doc; docMenu = null
                    }
                    MenuRow(Icons.Filled.Delete, "Delete") {
                        deletingDoc = doc; docMenu = null
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { docMenu = null }) { Text("Cancel") }
            }
        )
    }

    // ---- Folder long-press menu ----
    folderMenu?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderMenu = null },
            title = { Text(folder.name, maxLines = 1) },
            text = {
                Column {
                    MenuRow(Icons.Filled.Delete, "Delete folder") {
                        deletingFolder = folder; folderMenu = null
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { folderMenu = null }) { Text("Cancel") }
            }
        )
    }

    // ---- Rename doc ----
    renamingDoc?.let { doc ->
        NameDialog(
            title = "Rename document",
            initial = doc.name,
            confirmLabel = "Rename",
            onDismiss = { renamingDoc = null },
            onConfirm = { name ->
                if (name.isNotBlank()) {
                    viewModel.renameDocumentById(doc.id, name)
                }
                renamingDoc = null
            }
        )
    }

    // ---- Move doc ----
    moveTarget?.let { doc ->
        AlertDialog(
            onDismissRequest = { moveTarget = null },
            title = { Text("Move to") },
            text = {
                Column {
                    MenuRow(Icons.Filled.Description, "Documents (root)") {
                        viewModel.moveDocumentById(doc.id, null); moveTarget = null
                    }
                    folders.forEach { folder ->
                        MenuRow(Icons.Filled.Folder, folder.name) {
                            viewModel.moveDocumentById(doc.id, folder.id); moveTarget = null
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { moveTarget = null }) { Text("Cancel") }
            }
        )
    }

    // ---- Delete doc ----
    deletingDoc?.let { doc ->
        ConfirmDialog(
            title = "Delete document?",
            text = "\"${doc.name}\" and its ${doc.pageCount} page(s) will be permanently deleted.",
            onDismiss = { deletingDoc = null },
            onConfirm = {
                viewModel.deleteDocumentById(doc.id)
                deletingDoc = null
            }
        )
    }

    // ---- Delete folder ----
    deletingFolder?.let { folder ->
        ConfirmDialog(
            title = "Delete folder?",
            text = "\"${folder.name}\" will be deleted. Documents inside will move to the root.",
            onDismiss = { deletingFolder = null },
            onConfirm = {
                viewModel.deleteFolder(folder)
                deletingFolder = null
            }
        )
    }

    // ---- Delete selected (multi-select) ----
    if (confirmDeleteSelected) {
        ConfirmDialog(
            title = "Delete ${selectedIds.size} document${if (selectedIds.size == 1) "" else "s"}?",
            text = "The selected document(s) and their pages will be permanently deleted.",
            onDismiss = { confirmDeleteSelected = false },
            onConfirm = {
                viewModel.deleteDocumentsByIds(selectedIds.toList())
                selectedIds = emptySet()
                selecting = false
                confirmDeleteSelected = false
            }
        )
    }

    barcodeResult?.let { results ->
        BarcodeResultDialog(
            results = results,
            onDismiss = { viewModel.clearBarcodeResult() }
        )
    }
}

@Composable
fun MenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun NameDialog(
    title: String,
    initial: String = "",
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text("Name") }
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(value.trim()) }, enabled = value.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SortBar(newestFirst: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Filled.SwapVert,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            if (newestFirst) "Newest first" else "Oldest first",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BarcodeResultDialog(results: List<String>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (results.isEmpty()) "No barcode found" else "Barcode result") },
        text = {
            if (results.isEmpty()) {
                Text("No barcode was detected. Make sure it is well-lit and in focus.")
            } else {
                Column {
                    results.forEach { value ->
                        Text(
                            value,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (results.isNotEmpty()) {
                Button(onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText("Smart Scan", results.first())
                    )
                    onDismiss()
                }) { Text("Copy") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
