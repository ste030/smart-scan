package com.ste03.smartscan.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ste03.smartscan.data.DocumentSummary
import com.ste03.smartscan.ui.components.DocumentCard
import com.ste03.smartscan.ui.components.EmptyState
import com.ste03.smartscan.vm.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    viewModel: AppViewModel,
    folderId: Long,
    onBack: () -> Unit,
    onOpenDocument: (Long) -> Unit,
    onScan: () -> Unit
) {
    val summaries by viewModel.folderSummaries.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val folderName = folders.firstOrNull { it.id == folderId }?.name ?: "Folder"

    var menuDoc by remember { mutableStateOf<DocumentSummary?>(null) }
    var renamingDoc by remember { mutableStateOf<DocumentSummary?>(null) }
    var deletingDoc by remember { mutableStateOf<DocumentSummary?>(null) }

    LaunchedEffect(folderId) { viewModel.openFolder(folderId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folderName, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onScan) {
                Icon(Icons.Filled.Add, contentDescription = "Scan")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (summaries.isEmpty()) {
                item { EmptyState("No documents in this folder") }
            } else {
                items(summaries, key = { it.id }) { summary ->
                    DocumentCard(
                        summary = summary,
                        context = context,
                        onClick = { onOpenDocument(summary.id) },
                        onLongClick = { menuDoc = summary }
                    )
                }
            }
        }
    }

    menuDoc?.let { doc ->
        AlertDialog(
            onDismissRequest = { menuDoc = null },
            title = { Text(doc.name, maxLines = 1) },
            text = {
                Column {
                    MenuRow(Icons.Filled.Edit, "Rename") {
                        renamingDoc = doc; menuDoc = null
                    }
                    MenuRow(Icons.Filled.Delete, "Delete") {
                        deletingDoc = doc; menuDoc = null
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { menuDoc = null }) { Text("Cancel") }
            }
        )
    }

    renamingDoc?.let { doc ->
        NameDialog(
            title = "Rename document",
            initial = doc.name,
            confirmLabel = "Rename",
            onDismiss = { renamingDoc = null },
            onConfirm = { name ->
                if (name.isNotBlank()) viewModel.renameDocumentById(doc.id, name)
                renamingDoc = null
            }
        )
    }

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
}
