package com.example.aidocumentorganizer.Ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import com.example.aidocumentorganizer.ui.components.DocumentProcessingOverlay
import java.io.File

@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onImagesCaptured: (List<Uri>) -> Unit,
    isProcessing: Boolean = false,
    processingStage: String = "Extracting text..."
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    /*
     * CAMERA PERMISSION
     */

    var hasCameraPermission by remember {

        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    /*
     * CAPTURE STATE
     */

    var isCapturing by remember {
        mutableStateOf(false)
    }

    // Multi-page scanning: every capture is kept until the user taps Done.
    val capturedPages = remember { mutableStateListOf<Uri>() }

    /*
     * ERROR MESSAGE
     */

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * CAMERA PERMISSION LAUNCHER
     */

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            hasCameraPermission = granted

            if (granted) {
                errorMessage = null
            } else {

                errorMessage =
                    "Camera permission is required to scan documents."
            }
        }

    /*
     * REQUEST CAMERA PERMISSION
     */

    LaunchedEffect(Unit) {

        if (!hasCameraPermission) {

            permissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    /*
     * PERMISSION NOT GRANTED
     */

    if (!hasCameraPermission) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Text(
                    text = "Camera permission is required",
                    style = MaterialTheme.typography.titleMedium
                )

                Button(
                    onClick = {

                        permissionLauncher.launch(
                            Manifest.permission.CAMERA
                        )
                    }
                ) {

                    Text("Allow Camera")
                }

                Button(
                    onClick = onBack
                ) {

                    Text("Back")
                }
            }
        }

        return
    }

    /*
     * IMAGE CAPTURE
     */

    val imageCapture = remember {

        ImageCapture.Builder()
            // Text recognition needs sharp, full-resolution shots;
            // speed matters far less than detail for document scans.
            .setCaptureMode(
                ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
            )
            .build()
    }

    /*
     * CAMERA UI
     */

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        /*
         * LIVE CAMERA PREVIEW
         */

        AndroidView(

            factory = { ctx ->

                val previewView =
                    PreviewView(ctx)

                val cameraProviderFuture =
                    ProcessCameraProvider
                        .getInstance(ctx)

                cameraProviderFuture.addListener({

                    val cameraProvider =
                        cameraProviderFuture.get()

                    val preview =
                        Preview.Builder()
                            .build()
                            .also {

                                it.surfaceProvider =
                                    previewView.surfaceProvider
                            }

                    val cameraSelector =
                        CameraSelector.DEFAULT_BACK_CAMERA

                    try {

                        cameraProvider.unbindAll()

                        cameraProvider.bindToLifecycle(

                            lifecycleOwner,

                            cameraSelector,

                            preview,

                            imageCapture
                        )

                    } catch (exception: Exception) {

                        exception.printStackTrace()

                        errorMessage =
                            "Unable to start camera."
                    }

                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },

            modifier = Modifier.fillMaxSize()
        )


        /*
         * DARK OVERLAY
         */

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = 0.35f
                    )
                )
        )


        /*
         * TOP BACK BUTTON
         */

        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Below the status bar / camera cutout so it is tappable.
                .statusBarsPadding()
                .padding(
                    horizontal = 16.dp,
                    vertical = 20.dp
                ),
            horizontalArrangement = Arrangement.Start
        ) {

            Button(

                onClick = {

                    if (!isCapturing) {
                        onBack()
                    }
                },

                enabled = !isCapturing
            ) {

                Text("Back")
            }

            if (capturedPages.isNotEmpty()) {
                Text(
                    "${capturedPages.size} page${if (capturedPages.size == 1) "" else "s"}",
                    color = Color.White,
                    modifier = Modifier.padding(start = 12.dp, top = 12.dp)
                )
            }
        }


        /*
         * SCANNER FRAME
         */

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 150.dp,
                    bottom = 120.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = if (isCapturing) {
                    "Capturing document..."
                } else {
                    "Place your document inside the frame"
                },

                color = Color.White,

                style =
                    MaterialTheme.typography.bodyLarge
            )

            Box(

                modifier = Modifier
                    .padding(top = 20.dp)
                    .width(330.dp)
                    .height(430.dp)
                    .border(

                        border = BorderStroke(
                            width = 3.dp,
                            color = Color.White
                        ),

                        shape =
                            RoundedCornerShape(12.dp)
                    )
            )
        }


        /*
         * ERROR MESSAGE
         */

        errorMessage?.let { message ->

            Box(

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 100.dp
                    ),

                contentAlignment =
                    Alignment.TopCenter
            ) {

                Text(
                    text = message,
                    color = Color.White,
                    style =
                        MaterialTheme.typography.bodyMedium
                )
            }
        }


        /*
         * CAPTURE BUTTON / LOADING
         */

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (isCapturing) return@Button
                    isCapturing = true
                    errorMessage = null
                    val photoFile = File(context.cacheDir, "document_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                isCapturing = false
                                capturedPages.add(Uri.fromFile(photoFile))
                            }
                            override fun onError(exception: ImageCaptureException) {
                                isCapturing = false
                                errorMessage = "Failed to capture document. Please try again."
                            }
                        }
                    )
                },
                enabled = !isCapturing
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Text("Capturing...")
                } else {
                    Text("Capture Page")
                }
            }

            if (capturedPages.isNotEmpty()) {
                Button(
                    onClick = { onImagesCaptured(capturedPages.toList()) },
                    enabled = !isCapturing
                ) {
                    Text("Done (${capturedPages.size})")
                }
            }
        }

        if (isProcessing) {
            DocumentProcessingOverlay(
                stage = processingStage,
                pageCount = capturedPages.size.coerceAtLeast(1),
                previewUris = capturedPages.toList(),
                selectedName = if (capturedPages.size > 1) "Scanned document" else "Scanned page"
            )
        }
    }
}