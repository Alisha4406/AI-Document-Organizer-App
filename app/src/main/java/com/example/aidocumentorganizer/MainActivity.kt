package com.example.aidocumentorganizer

import android.Manifest
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.aidocumentorganizer.navigation.AppNavGraph
import com.example.aidocumentorganizer.ui.components.DocumentActions
import com.example.aidocumentorganizer.ui.components.EditDetailsDialog
import com.example.aidocumentorganizer.ui.components.MainTab
import com.example.aidocumentorganizer.ui.home.HomeScreen
import com.example.aidocumentorganizer.ui.home.HomeViewModel
import com.example.aidocumentorganizer.ui.home.HomeViewModelFactory
import com.example.aidocumentorganizer.Ui.screens.AddDocumentScreen
import com.example.aidocumentorganizer.Ui.screens.CameraScreen
import com.example.aidocumentorganizer.Ui.screens.CategoriesScreen
import com.example.aidocumentorganizer.Ui.screens.DocumentDetailScreen
import com.example.aidocumentorganizer.Ui.screens.DocumentTextScreen
import com.example.aidocumentorganizer.Ui.screens.DocumentsScreen
import com.example.aidocumentorganizer.Ui.screens.SaveDocumentScreen
import com.example.aidocumentorganizer.Ui.screens.SettingsScreen
import com.example.aidocumentorganizer.Ui.screens.WelcomeScreen
import com.example.aidocumentorganizer.ui.screens.auth.LoginScreen
import com.example.aidocumentorganizer.ui.screens.auth.SignUpScreen
import com.example.aidocumentorganizer.data.authentication.AuthManager
import com.example.aidocumentorganizer.data.local.DocumentDatabase
import com.example.aidocumentorganizer.data.local.DocumentEntity
import com.example.aidocumentorganizer.data.repository.DocumentRepository
import com.example.aidocumentorganizer.data.reminders.ReminderScheduler
import com.example.aidocumentorganizer.data.remote.CloudBackupManager
import com.example.aidocumentorganizer.ui.theme.AIDocumentOrganizerTheme
import com.example.aidocumentorganizer.utils.AppPreferences
import com.example.aidocumentorganizer.navigation.AppRoutes
import com.example.aidocumentorganizer.utils.PdfExporter
import com.example.aidocumentorganizer.ocr.OcrOutcome
import com.example.aidocumentorganizer.utils.processImageWithOCR
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // The system splash (app icon on the brand background) is the only
        // launch screen; it hands over to Compose as soon as the first frame
        // is ready. Must run before super.onCreate().
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val preferences = AppPreferences(applicationContext)

        val initialDarkMode = runBlocking { preferences.darkMode.first() }
        val initialRemindersEnabled = runBlocking { preferences.remindersEnabled.first() }

        if (initialRemindersEnabled) {
            ReminderScheduler.schedule(applicationContext)
        }

        setContent {

            // null = follow system until the user flips the switch.
            var darkModeSetting by remember { mutableStateOf(initialDarkMode) }
            val systemDark = isSystemInDarkTheme()

            AIDocumentOrganizerTheme(darkTheme = darkModeSetting ?: systemDark) {

                val context = LocalContext.current

                // =====================================================
                // AUTH
                // =====================================================

                val authManager = remember { AuthManager() }

                val firebaseAuth = FirebaseAuth.getInstance()

                // Screens: welcome, login, signup,
                //          home, documents, categories, settings,
                //          add, camera, document, save, detail
                // Signed-in users go straight to home; auth state is available
                // synchronously, so no intermediate splash screen is needed.
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentScreen = backStackEntry?.destination?.route
                    ?: if (firebaseAuth.currentUser != null) AppRoutes.HOME else AppRoutes.WELCOME

                fun go(route: String) {
                    navController.navigate(route) { launchSingleTop = true }
                }

                val currentUserId = firebaseAuth.currentUser?.uid ?: ""

                // =====================================================
                // DOCUMENT STATES
                // =====================================================

                var capturedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
                var selectedDocumentUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
                var selectedDocumentName by rememberSaveable { mutableStateOf<String?>(null) }
                var savedImagePath by rememberSaveable { mutableStateOf("") }
                var scannedPageCount by rememberSaveable { mutableStateOf(1) }
                var extractedText by rememberSaveable { mutableStateOf("") }
                var selectedDocument by remember { mutableStateOf<DocumentEntity?>(null) }

                // OCR
                var isOcrProcessing by rememberSaveable { mutableStateOf(false) }
                var processingStage by rememberSaveable { mutableStateOf("Preparing document...") }
                var ocrErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
                // Set when the image was too small / blurry to read reliably.
                var ocrWarningMessage by rememberSaveable { mutableStateOf<String?>(null) }

                // Rename dialog
                var showRenameDialog by rememberSaveable { mutableStateOf(false) }
                var renameDocumentId by rememberSaveable { mutableStateOf<Int?>(null) }
                var renameDocumentName by rememberSaveable { mutableStateOf("") }

                // Category / expiry dialog
                var detailsDocument by remember { mutableStateOf<DocumentEntity?>(null) }

                // Set when the name typed on the Save screen already exists.
                var saveNameError by rememberSaveable { mutableStateOf<String?>(null) }
                var renameErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

                // Filters handed to the Documents tab from Categories / bell icon.
                var documentsCategoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
                var documentsExpiringOnly by rememberSaveable { mutableStateOf(false) }

                // Settings
                var remindersEnabled by remember { mutableStateOf(initialRemindersEnabled) }
                var backupInProgress by rememberSaveable { mutableStateOf(false) }

                // =====================================================
                // DATABASE + VIEWMODEL (scoped to the signed-in user)
                // =====================================================

                val database = DocumentDatabase.getDatabase(context)

                val repository = remember { DocumentRepository(database.documentDao()) }

                val homeViewModelFactory = remember(currentUserId) {
                    HomeViewModelFactory(repository = repository, userId = currentUserId)
                }

                val homeViewModel: HomeViewModel = viewModel(
                    key = "HomeViewModel_$currentUserId",
                    factory = homeViewModelFactory
                )

                val allDocuments by homeViewModel.documents.collectAsStateWithLifecycle()

                // =====================================================
                // NOTIFICATION PERMISSION (Android 13+)
                // =====================================================

                val notificationPermissionLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { /* If denied, reminders simply stay silent. */ }

                fun ensureNotificationPermission() {
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }
                }

                // =====================================================
                // COPY IMAGE TO INTERNAL STORAGE
                //
                // Returns the path of the file it just wrote (unique,
                // timestamped) so the caller attaches it to the right
                // in-flight document instead of a shared mutable var.
                // =====================================================

                suspend fun saveImagePermanently(uri: Uri): String? =
                    withContext(Dispatchers.IO) {
                        try {
                            val inputStream = when (uri.scheme?.lowercase()) {
                                "file" -> File(uri.path ?: return@withContext null).inputStream()
                                else -> contentResolver.openInputStream(uri)
                            } ?: return@withContext null

                            val imagesDirectory = File(filesDir, "documents")
                            if (!imagesDirectory.exists()) imagesDirectory.mkdirs()

                            val fileName =
                                "document_${System.currentTimeMillis()}_" +
                                        "${(0..9999).random()}.jpg"

                            val destinationFile = File(imagesDirectory, fileName)

                            inputStream.use { input ->
                                destinationFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }

                            destinationFile.absolutePath
                        } catch (e: Exception) {
                            null
                        }
                    }

                // OCR (preprocess -> recognize -> filter noise -> quality check)
                // as a suspend function so it can be awaited together with
                // saveImagePermanently for the SAME capture.
                suspend fun runOcr(uri: Uri): Result<OcrOutcome> =
                    try {
                        Result.success(processImageWithOCR(context, uri))
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (e: Exception) {
                        Result.failure(e)
                    } catch (e: OutOfMemoryError) {
                        Result.failure(e)
                    }

                fun displayNameForUri(uri: Uri): String? {
                    if (uri.scheme == "file") return File(uri.path ?: return null).name
                    var cursor: Cursor? = null
                    return try {
                        cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                        if (cursor?.moveToFirst() == true) {
                            cursor?.getString(0)
                        } else {
                            uri.lastPathSegment?.substringAfterLast('/')
                        }
                    } catch (_: Exception) {
                        uri.lastPathSegment?.substringAfterLast('/')
                    } finally {
                        cursor?.close()
                    }
                }

                fun isPdfUri(uri: Uri): Boolean {
                    val mime = try { contentResolver.getType(uri) } catch (_: Exception) { null }
                    return mime.equals("application/pdf", ignoreCase = true) ||
                            displayNameForUri(uri)?.endsWith(".pdf", ignoreCase = true) == true ||
                            uri.toString().contains(".pdf", ignoreCase = true)
                }

                suspend fun renderPdfToImages(uri: Uri): List<Uri> = withContext(Dispatchers.IO) {
                    val rendered = mutableListOf<Uri>()
                    val descriptor = if (uri.scheme == "file") {
                        ParcelFileDescriptor.open(File(uri.path ?: return@withContext emptyList()).let { it }, ParcelFileDescriptor.MODE_READ_ONLY)
                    } else {
                        contentResolver.openFileDescriptor(uri, "r")
                    } ?: return@withContext emptyList()

                    descriptor.use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            for (pageIndex in 0 until renderer.pageCount) {
                                renderer.openPage(pageIndex).use { page ->
                                    val targetWidth = 1600
                                    val targetHeight = (targetWidth.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                                    bitmap.eraseColor(Color.WHITE)
                                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                    val file = File(cacheDir, "pdf_${System.currentTimeMillis()}_${pageIndex}.jpg")
                                    FileOutputStream(file).use { output ->
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
                                    }
                                    bitmap.recycle()
                                    rendered += Uri.fromFile(file)
                                }
                            }
                        }
                    }
                    rendered
                }

                suspend fun prepareSelectedDocuments(uris: List<Uri>): List<Uri> = withContext(Dispatchers.IO) {
                    val prepared = mutableListOf<Uri>()
                    for (uri in uris) {
                        if (isPdfUri(uri)) {
                            prepared += renderPdfToImages(uri)
                        } else {
                            prepared += uri
                        }
                    }
                    prepared
                }

                fun processDocumentImages(uris: List<Uri>) {

                    if (uris.isEmpty()) return

                    selectedDocumentUris = uris
                    val firstName = displayNameForUri(uris.first()) ?: "Selected document"
                    selectedDocumentName = if (uris.size > 1) {
                        "$firstName + ${uris.size - 1} more"
                    } else {
                        firstName
                    }
                    capturedImageUri = uris.first()
                    extractedText = ""
                    ocrErrorMessage = null
                    ocrWarningMessage = null
                    isOcrProcessing = true
                    processingStage = "Preparing document..."
                    savedImagePath = ""
                    scannedPageCount = uris.size

                    lifecycleScope.launch {
                        val startedAt = System.currentTimeMillis()
                        val preparedUris = try {
                            prepareSelectedDocuments(uris)
                        } catch (e: Exception) {
                            emptyList()
                        }

                        if (preparedUris.isEmpty()) {
                            isOcrProcessing = false
                            ocrErrorMessage = "We couldn't open the selected document. Please choose an image or PDF file you can access."
                            return@launch
                        }

                        scannedPageCount = preparedUris.size
                        processingStage = if (preparedUris.size > 1) {
                            "Extracting text from ${preparedUris.size} pages..."
                        } else {
                            "Extracting text..."
                        }

                        val imagePathsDeferred = async {
                            preparedUris.mapNotNull { saveImagePermanently(it) }
                        }

                        val ocrResults = preparedUris.mapIndexed { index, uri ->
                            processingStage = if (preparedUris.size > 1) {
                                "Extracting text • Page ${index + 1} of ${preparedUris.size}"
                            } else {
                                "Extracting text..."
                            }
                            index to runOcr(uri)
                        }
                        val imagePaths = imagePathsDeferred.await()
                        savedImagePath = imagePaths.joinToString("|")

                        val successful = ocrResults.mapNotNull { (index, result) ->
                            result.getOrNull()?.let { index to it }
                        }
                        val failures = ocrResults.count { it.second.isFailure }

                        if (successful.isNotEmpty()) {
                            processingStage = "Analyzing document..."
                            extractedText = successful.joinToString("\n\n") { (index, outcome) ->
                                "--- Page ${index + 1} ---\n${outcome.text}"
                            }
                            ocrWarningMessage = buildString {
                                successful.mapNotNull { it.second.warning }
                                    .filter { it.isNotBlank() }
                                    .distinct()
                                    .forEach { append(it).append(" ") }
                                if (failures > 0) append("$failures page(s) could not be read.")
                            }.trim().ifBlank { null }
                            processingStage = "Finishing up..."
                            val minimumVisibleTime = 650L
                            val elapsed = System.currentTimeMillis() - startedAt
                            delay(max(0L, minimumVisibleTime - elapsed))
                            isOcrProcessing = false
                            go(AppRoutes.DOCUMENT)
                        } else {
                            extractedText = ""
                            ocrWarningMessage = null
                            processingStage = "Could not read the document"
                            delay(650L)
                            isOcrProcessing = false
                            ocrErrorMessage = "Unable to read text from the scanned pages."
                            go(AppRoutes.DOCUMENT)
                        }
                    }
                }

                fun processDocumentImage(uri: Uri) = processDocumentImages(listOf(uri))

                val galleryLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.OpenMultipleDocuments()
                    ) { uris ->
                        // The system picker now exposes real document sources:
                        // images and PDFs. PDFs are rendered to pages before OCR.
                        processDocumentImages(uris)
                    }

                // =====================================================
                // SHARED HELPERS
                // =====================================================

                fun clearWorkingState() {
                    selectedDocument = null
                    selectedDocumentUris = emptyList()
                    selectedDocumentName = null
                    capturedImageUri = null
                    savedImagePath = ""
                    scannedPageCount = 1
                    extractedText = ""
                    isOcrProcessing = false
                    processingStage = "Preparing document..."
                    ocrErrorMessage = null
                    ocrWarningMessage = null
                }

                fun openTab(tab: MainTab) {
                    when (tab) {
                        MainTab.Home -> go(AppRoutes.HOME)
                        MainTab.Documents -> {
                            documentsCategoryFilter = null
                            documentsExpiringOnly = false
                            go(AppRoutes.DOCUMENTS)
                        }
                        MainTab.Categories -> go(AppRoutes.CATEGORIES)
                        MainTab.Settings -> go(AppRoutes.SETTINGS)
                    }
                }

                fun logout() {
                    authManager.logout()
                    clearWorkingState()
                    documentsCategoryFilter = null
                    documentsExpiringOnly = false
                    go(AppRoutes.LOGIN)
                }

                // Actions for every ⋮ menu on a document row.
                val documentActions = remember(homeViewModel) {
                    DocumentActions(
                        onOpen = { document ->
                            selectedDocument = document
                            go(AppRoutes.DETAIL)
                        },
                        onRename = { document ->
                            renameDocumentId = document.id
                            renameDocumentName = document.title
                            renameErrorMessage = null
                            showRenameDialog = true
                        },
                        onEditDetails = { document -> detailsDocument = document },
                        onShare = { document -> shareDocumentText(document) },
                        onExportPdf = { document -> exportDocumentPdf(document) },
                        onDelete = { document -> homeViewModel.deleteDocument(document) },
                        onToggleFavorite = { document -> homeViewModel.toggleFavorite(document) }
                    )
                }

                // =====================================================
                // SYSTEM BACK BUTTON
                // =====================================================

                BackHandler(enabled = isOcrProcessing) {
                    // Keep the OCR flow stable while the document is being processed.
                }

                BackHandler(
                    enabled = !isOcrProcessing &&
                            currentScreen !in listOf(AppRoutes.WELCOME, AppRoutes.HOME)
                            && navController.previousBackStackEntry != null
                ) {
                    when (currentScreen) {
                        AppRoutes.SAVE -> {
                            saveNameError = null
                            navController.navigateUp()
                        }
                        AppRoutes.DOCUMENT -> {
                            isOcrProcessing = false
                            ocrErrorMessage = null
                            clearWorkingState()
                            navController.navigateUp()
                        }
                        AppRoutes.DETAIL -> {
                            selectedDocument = null
                            navController.navigateUp()
                        }
                        else -> navController.navigateUp()
                    }
                }

                // =====================================================
                // SCREENS
                // =====================================================

                AppNavGraph(
                    navController = navController,
                    startDestination = if (firebaseAuth.currentUser != null) AppRoutes.HOME else AppRoutes.WELCOME
                ) { currentScreen ->

                    when (currentScreen) {

                    AppRoutes.WELCOME -> WelcomeScreen(
                        onGetStarted = { go(AppRoutes.SIGNUP) },
                        onLogin = { go(AppRoutes.LOGIN) }
                    )

                    AppRoutes.LOGIN -> LoginScreen(
                        authManager = authManager,
                        onLoginSuccess = {
                            if (FirebaseAuth.getInstance().currentUser != null) {
                                clearWorkingState()
                                go(AppRoutes.HOME)
                            }
                        },
                        onNavigateToSignUp = { go(AppRoutes.SIGNUP) }
                    )

                    AppRoutes.SIGNUP -> SignUpScreen(
                        authManager = authManager,
                        onSignUpSuccess = {
                            clearWorkingState()
                            go(AppRoutes.HOME)
                        },
                        onNavigateToLogin = { go(AppRoutes.LOGIN) }
                    )

                    // ------------------- MAIN TABS -------------------

                    AppRoutes.HOME -> HomeScreen(
                        viewModel = homeViewModel,
                        userName = authManager.currentUserName(),
                        actions = documentActions,
                        onTabSelected = { openTab(it) },
                        onAddDocumentClick = { go(AppRoutes.ADD) },
                        onScanClick = { go(AppRoutes.CAMERA) },
                        onGalleryClick = { galleryLauncher.launch(arrayOf("image/*", "application/pdf")) },
                        onSearchClick = { openTab(MainTab.Documents) },
                        onSeeAllClick = { openTab(MainTab.Documents) },
                        onExpiringClick = {
                            documentsCategoryFilter = null
                            documentsExpiringOnly = true
                            go(AppRoutes.DOCUMENTS)
                        },
                        onCategoryClick = { label ->
                            documentsCategoryFilter = label
                            documentsExpiringOnly = false
                            go(AppRoutes.DOCUMENTS)
                        },
                        onLogoutClick = { logout() }
                    )

                    AppRoutes.DOCUMENTS -> DocumentsScreen(
                        viewModel = homeViewModel,
                        initialCategory = documentsCategoryFilter,
                        initialExpiringOnly = documentsExpiringOnly,
                        actions = documentActions,
                        onTabSelected = { openTab(it) },
                        onAddClick = { go(AppRoutes.ADD) }
                    )

                    AppRoutes.CATEGORIES -> CategoriesScreen(
                        viewModel = homeViewModel,
                        onCategoryClick = { label ->
                            documentsCategoryFilter = label
                            documentsExpiringOnly = false
                            go(AppRoutes.DOCUMENTS)
                        },
                        onAllDocumentsClick = { openTab(MainTab.Documents) },
                        onExpiringClick = {
                            documentsCategoryFilter = null
                            documentsExpiringOnly = true
                            go(AppRoutes.DOCUMENTS)
                        },
                        onTabSelected = { openTab(it) },
                        onAddClick = { go(AppRoutes.ADD) }
                    )

                    AppRoutes.SETTINGS -> SettingsScreen(
                        userName = authManager.currentUserName(),
                        userEmail = authManager.currentUserEmail(),
                        documentCount = allDocuments.size,
                        darkMode = darkModeSetting,
                        remindersEnabled = remindersEnabled,
                        backupInProgress = backupInProgress,
                        onBackupNow = {
                            if (!backupInProgress && currentUserId.isNotBlank()) {
                                backupInProgress = true
                                lifecycleScope.launch {
                                    val result = runCatching {
                                        CloudBackupManager().backupDocuments(
                                            userId = currentUserId,
                                            documents = allDocuments
                                        )
                                    }
                                    backupInProgress = false
                                    result.onSuccess {
                                        Toast.makeText(
                                            context,
                                            "Backup complete: ${it.documents} documents, ${it.pages} pages.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }.onFailure {
                                        Toast.makeText(
                                            context,
                                            "Backup failed: ${it.message ?: "Please try again."}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        },
                        onDarkModeChange = { enabled ->
                            darkModeSetting = enabled
                            lifecycleScope.launch {
                                preferences.setDarkMode(enabled)
                            }
                        },
                        onRemindersChange = { enabled ->
                            remindersEnabled = enabled
                            lifecycleScope.launch {
                                preferences.setRemindersEnabled(enabled)
                            }
                            if (enabled) {
                                ensureNotificationPermission()
                                ReminderScheduler.schedule(applicationContext)
                            } else {
                                ReminderScheduler.cancel(applicationContext)
                            }
                        },
                        onChangePassword = {
                            authManager.sendPasswordReset { _, message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        },
                        onLogout = { logout() },
                        onTabSelected = { openTab(it) },
                        onAddClick = { go(AppRoutes.ADD) }
                    )

                    // ------------------- ADD FLOW -------------------

                    AppRoutes.ADD -> AddDocumentScreen(
                        onCameraClick = { go(AppRoutes.CAMERA) },
                        onGalleryClick = { galleryLauncher.launch(arrayOf("image/*", "application/pdf")) },
                        onBack = { go(AppRoutes.HOME) },
                        isProcessing = isOcrProcessing,
                        processingStage = processingStage,
                        pageCount = scannedPageCount,
                        previewUris = selectedDocumentUris,
                        selectedName = selectedDocumentName
                    )

                    AppRoutes.CAMERA -> CameraScreen(
                        onBack = { go(AppRoutes.ADD) },
                        onImagesCaptured = { uris -> processDocumentImages(uris) },
                        isProcessing = isOcrProcessing,
                        processingStage = processingStage
                    )

                    AppRoutes.DOCUMENT -> DocumentTextScreen(
                        extractedText = extractedText,
                        isProcessing = isOcrProcessing,
                        errorMessage = ocrErrorMessage,
                        warningMessage = ocrWarningMessage,
                        onSaveDocumentClick = {
                            saveNameError = null
                            go(AppRoutes.SAVE)
                        },
                        onBack = {
                            isOcrProcessing = false
                            ocrErrorMessage = null
                            ocrWarningMessage = null
                            go(AppRoutes.HOME)
                        },
                        // Replaces the OCR text with the translated version
                        // so the document is saved in the new language.
                        onTextTranslated = { translatedText ->
                            extractedText = translatedText
                        }
                    )

                    AppRoutes.SAVE -> SaveDocumentScreen(
                        extractedText = extractedText,
                        errorMessage = saveNameError,
                        onNameChanged = { saveNameError = null },
                        onSave = { documentName, category, expiryDate ->

                            val user = FirebaseAuth.getInstance().currentUser

                            // Don't save without a logged-in user
                            if (user == null) {
                                go(AppRoutes.LOGIN)
                                return@SaveDocumentScreen
                            }

                            val trimmedName = documentName.trim()

                            lifecycleScope.launch {

                                if (homeViewModel.isDocumentNameTaken(trimmedName)) {
                                    saveNameError =
                                        "A document named \"$trimmedName\" already exists."
                                    return@launch
                                }

                                saveNameError = null

                                homeViewModel.insertDocument(
                                    DocumentEntity(
                                        id = 0,
                                        title = trimmedName,
                                        type = "Image",
                                        extractedText = extractedText,
                                        imagePath = savedImagePath,
                                        dateAdded = System.currentTimeMillis(),
                                        pageCount = scannedPageCount,
                                        userId = user.uid,
                                        category = category,
                                        expiryDate = expiryDate
                                    )
                                )

                                // Reminders need the notification permission.
                                if (expiryDate != null && remindersEnabled) {
                                    ensureNotificationPermission()
                                }

                                // Nothing from this document may leak into the next.
                                capturedImageUri = null
                                savedImagePath = ""
                                extractedText = ""
                                ocrErrorMessage = null
                                ocrWarningMessage = null
                                isOcrProcessing = false
                                processingStage = "Preparing document..."

                                go(AppRoutes.HOME)
                            }
                        },
                        onBack = {
                            saveNameError = null
                            go(AppRoutes.DOCUMENT)
                        }
                    )

                    AppRoutes.DETAIL -> selectedDocument?.let { document ->

                        DocumentDetailScreen(
                            document = document,

                            onBack = {
                                selectedDocument = null
                                go(AppRoutes.HOME)
                            },

                            onRename = { newName, onFinished ->
                                lifecycleScope.launch {
                                    val trimmedName = newName.trim()

                                    if (trimmedName.isEmpty()) {
                                        onFinished(false)
                                        return@launch
                                    }

                                    val taken = homeViewModel.isDocumentNameTaken(
                                        title = trimmedName,
                                        excludingDocumentId = document.id
                                    )

                                    if (taken) {
                                        onFinished(false)
                                    } else {
                                        homeViewModel.renameDocument(
                                            id = document.id,
                                            newTitle = trimmedName
                                        )
                                        selectedDocument = document.copy(title = trimmedName)
                                        onFinished(true)
                                    }
                                }
                            },

                            onEditExtractedText = { newText ->
                                homeViewModel.updateExtractedText(
                                    id = document.id,
                                    newText = newText
                                )
                                selectedDocument = document.copy(extractedText = newText)
                            },

                            onDelete = {
                                homeViewModel.deleteDocument(document)
                                selectedDocument = null
                                go(AppRoutes.HOME)
                            },

                            onToggleFavorite = {
                                homeViewModel.toggleFavorite(document)
                                selectedDocument = document.copy(isFavorite = !document.isFavorite)
                            },

                            onUpdateTags = { tags ->
                                homeViewModel.updateTags(document.id, tags)
                                selectedDocument = document.copy(tags = tags)
                            },

                            onSaveTranslatedCopy = { translatedText, languageName ->

                                lifecycleScope.launch {

                                    // Strip any "(Language)" tag(s) the title already
                                    // carries so translating a translated copy doesn't
                                    // stack tags forever.
                                    val untaggedTitle =
                                        document.title
                                            .replace(Regex("""(\s*\([^()]*\))+$"""), "")
                                            .trim()
                                            .ifBlank { document.title }

                                    val baseName = "$untaggedTitle ($languageName)"

                                    var candidateName = baseName
                                    var suffix = 2

                                    while (homeViewModel.isDocumentNameTaken(candidateName)) {
                                        candidateName = "$baseName ($suffix)"
                                        suffix++
                                    }

                                    homeViewModel.insertDocument(
                                        document.copy(
                                            id = 0,
                                            title = candidateName,
                                            extractedText = translatedText,
                                            dateAdded = System.currentTimeMillis()
                                        )
                                    )

                                    selectedDocument = null
                                    go(AppRoutes.HOME)
                                }
                            }
                        )
                    }
                    }
                }

                // =====================================================
                // RENAME DIALOG
                // =====================================================

                if (showRenameDialog) {

                    AlertDialog(
                        onDismissRequest = {
                            showRenameDialog = false
                            renameErrorMessage = null
                        },
                        title = { Text("Rename Document") },
                        text = {
                            OutlinedTextField(
                                value = renameDocumentName,
                                onValueChange = {
                                    renameDocumentName = it
                                    renameErrorMessage = null
                                },
                                label = { Text("Document Name") },
                                singleLine = true,
                                isError = renameErrorMessage != null,
                                supportingText = {
                                    renameErrorMessage?.let { Text(it) }
                                }
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val id = renameDocumentId
                                    val newName = renameDocumentName.trim()

                                    if (id != null && newName.isNotEmpty()) {
                                        lifecycleScope.launch {
                                            if (homeViewModel.isDocumentNameTaken(
                                                    title = newName,
                                                    excludingDocumentId = id
                                                )
                                            ) {
                                                renameErrorMessage =
                                                    "Another document already uses this name."
                                            } else {
                                                homeViewModel.renameDocument(
                                                    id = id,
                                                    newTitle = newName
                                                )
                                                renameErrorMessage = null
                                                showRenameDialog = false
                                            }
                                        }
                                    } else {
                                        renameErrorMessage = "Document name cannot be empty."
                                    }
                                }
                            ) { Text("Save") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRenameDialog = false; renameErrorMessage = null }) { Text("Cancel") }
                        }
                    )
                }

                // =====================================================
                // CATEGORY / EXPIRY DIALOG
                // =====================================================

                detailsDocument?.let { document ->

                    EditDetailsDialog(
                        document = document,
                        onDismiss = { detailsDocument = null },
                        onSave = { category, expiryDate ->
                            homeViewModel.updateDetails(
                                id = document.id,
                                category = category,
                                expiryDate = expiryDate
                            )
                            if (expiryDate != null && remindersEnabled) {
                                ensureNotificationPermission()
                            }
                            detailsDocument = null
                        }
                    )
                }
            }
        }
    }

    // =========================================================
    // SHARE TEXT
    // =========================================================

    private fun shareDocumentText(document: DocumentEntity) {

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, document.title)
            putExtra(Intent.EXTRA_TEXT, "${document.title}\n\n${document.extractedText}")
        }

        startActivity(Intent.createChooser(shareIntent, "Share Document"))
    }

    // =========================================================
    // EXPORT / SHARE AS PDF
    // =========================================================

    private fun exportDocumentPdf(document: DocumentEntity) {

        Toast.makeText(this, "Creating PDF...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {

            val file = withContext(Dispatchers.IO) {
                PdfExporter.export(
                    context = applicationContext,
                    title = document.title,
                    imagePath = document.imagePath,
                    text = document.extractedText
                )
            }

            if (file == null) {
                Toast.makeText(
                    this@MainActivity,
                    "Couldn't create the PDF.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            val uri = FileProvider.getUriForFile(
                this@MainActivity,
                "$packageName.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, document.title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share PDF"))
        }
    }
}
