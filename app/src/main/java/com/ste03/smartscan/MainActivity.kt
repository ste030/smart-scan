package com.ste03.smartscan

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.ste03.smartscan.ui.CameraScreen
import com.ste03.smartscan.ui.CropScreen
import com.ste03.smartscan.ui.DocumentScreen
import com.ste03.smartscan.ui.FolderScreen
import com.ste03.smartscan.ui.HomeScreen
import com.ste03.smartscan.ui.LockScreen
import com.ste03.smartscan.ui.SettingsScreen
import com.ste03.smartscan.ui.ThemeScreen
import com.ste03.smartscan.ui.theme.SmartScanTheme
import com.ste03.smartscan.ui.theme.Themes
import com.ste03.smartscan.vm.AppViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: AppViewModel by viewModels()
    private var locked by mutableStateOf(false)
    private var pendingBarcode = false
    private var relockOnResume = false

    private val scannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                val uris = scanResult?.pages?.map { it.imageUri } ?: emptyList()
                viewModel.scanBarcode(uris)
            }
            pendingBarcode = false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Force a dark theme: light status/nav bar icons on dark.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        locked = viewModel.lockManager.isLockEnabled() && !viewModel.lockManager.isUnlocked()

        setContent {
            val themeName by viewModel.themeName.collectAsStateWithLifecycle()
            SmartScanTheme(accent = Themes.colorFor(themeName)) {
                if (locked) {
                    LockScreen(
                        lockManager = viewModel.lockManager,
                        onUnlocked = {
                            viewModel.lockManager.markUnlocked()
                            locked = false
                        }
                    )
                } else {
                    val nav = rememberNavController()
                    NavHost(
                        navController = nav,
                        startDestination = "home",
                        enterTransition = { fadeIn(tween(240)) },
                        exitTransition = { fadeOut(tween(240)) },
                        popEnterTransition = { fadeIn(tween(240)) },
                        popExitTransition = { fadeOut(tween(240)) }
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onOpenFolder = { id -> nav.navigate("folder/$id") },
                                onOpenDocument = { id -> nav.navigate("document/$id") },
                                onOpenSettings = { nav.navigate("settings") },
                                onScan = { nav.navigate("camera") },
                                onScanBarcode = { launchBarcodeScan() }
                            )
                        }
                        composable("folder/{folderId}") { backStack ->
                            val folderId =
                                backStack.arguments?.getString("folderId")?.toLongOrNull() ?: 0L
                            FolderScreen(
                                viewModel = viewModel,
                                folderId = folderId,
                                onBack = { nav.popBackStack() },
                                onOpenDocument = { id -> nav.navigate("document/$id") },
                                onScan = { nav.navigate("camera?folderId=$folderId") }
                            )
                        }
                        composable("document/{docId}") { backStack ->
                            val docId =
                                backStack.arguments?.getString("docId")?.toLongOrNull() ?: 0L
                            DocumentScreen(
                                viewModel = viewModel,
                                docId = docId,
                                onBack = { nav.popBackStack() },
                                onAddPages = { nav.navigate("camera?appendTo=$docId") },
                                onEditPage = { page ->
                                    viewModel.beginCrop(
                                        viewModel.pageFile(page), "edit", page
                                    )
                                    nav.navigate("crop")
                                }
                            )
                        }
                        composable(
                            "camera?folderId={folderId}&appendTo={appendTo}",
                            arguments = listOf(
                                navArgument("folderId") {
                                    type = NavType.LongType
                                    defaultValue = -1L
                                },
                                navArgument("appendTo") {
                                    type = NavType.LongType
                                    defaultValue = -1L
                                }
                            )
                        ) { backStack ->
                            val folderId = backStack.arguments?.getLong("folderId") ?: -1L
                            val appendTo = backStack.arguments?.getLong("appendTo") ?: -1L
                            CameraScreen(
                                viewModel = viewModel,
                                folderId = if (folderId >= 0) folderId else null,
                                appendTo = if (appendTo >= 0) appendTo else null,
                                onCapture = { nav.navigate("crop") },
                                onDone = { nav.popBackStack() },
                                onCancel = { nav.popBackStack() }
                            )
                        }
                        composable("crop") {
                            CropScreen(
                                viewModel = viewModel,
                                onDone = { nav.popBackStack() },
                                onCancel = { viewModel.clearCrop(); nav.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { nav.popBackStack() },
                                onOpenTheme = { nav.navigate("theme") }
                            )
                        }
                        composable("theme") {
                            ThemeScreen(
                                viewModel = viewModel,
                                onBack = { nav.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun launchBarcodeScan() {
        pendingBarcode = true
        startScanner()
    }

    private fun startScanner() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(100)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        val scanner = GmsDocumentScanning.getClient(options)
        scanner.getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                pendingBarcode = false
                viewModel.showMessage("Scanner unavailable: ${e.message}")
            }
    }

    override fun onStop() {
        super.onStop()
        if (viewModel.lockManager.isLockEnabled()) {
            relockOnResume = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (relockOnResume) {
            relockOnResume = false
            locked = true
        }
    }
}
