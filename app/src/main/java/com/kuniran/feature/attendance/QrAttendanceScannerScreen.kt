package com.kuniran.feature.attendance

import com.kuniran.core.common.asText

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.kuniran.R
import com.kuniran.core.ui.components.KuniranTopAppBar
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrAttendanceScannerScreen(
    viewModel: AttendanceViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { event ->
            snackbarHostState.showSnackbar("${context.getString(R.string.qr_scanner_success_title)} $event")
            viewModel.dismissSuccessMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(err.asText(context))
            viewModel.dismissError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            KuniranTopAppBar(
                title = stringResource(R.string.qr_scanner_title),
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("btn_back_qr_scanner")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.btn_cancel)
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.isAdmin) {
                        IconButton(
                            onClick = { viewModel.setShowGeneratorDialog(true) },
                            modifier = Modifier.testTag("btn_open_qr_generator")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = stringResource(R.string.qr_generator_title)
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.setShowManualDialog(true) },
                        modifier = Modifier.testTag("btn_manual_attendance")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = stringResource(R.string.qr_scanner_manual_entry)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (hasCameraPermission) {
                // CameraX Scanner Viewport with Viewfinder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(Color.Black)
                        .testTag("camera_scanner_viewport"),
                    contentAlignment = Alignment.Center
                ) {
                    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
                    val barcodeScanner = remember { BarcodeScanning.getClient() }

                    DisposableEffect(Unit) {
                        onDispose {
                            cameraExecutor.shutdown()
                            barcodeScanner.close()
                        }
                    }

                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )
                                        barcodeScanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                barcodes.firstOrNull()?.rawValue?.let { raw ->
                                                    viewModel.onQrCodeScanned(raw)
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (_: Exception) {
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Scanner Viewfinder Overlay
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        // Animated Scanning Line
                        val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
                        val scanProgress by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 200f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scan_laser_progress"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .offset(y = scanProgress.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }

                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            } else {
                // Permission Fallback Screen
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.qr_scanner_permission_needed),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier.testTag("btn_grant_camera_permission")
                        ) {
                            Text(stringResource(R.string.qr_scanner_btn_grant_permission))
                        }
                    }
                }
            }

            // Description / Instructions
            Text(
                text = stringResource(R.string.qr_scanner_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )

            // Section Header: Riwayat Kehadiran Kegiatan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.qr_scanner_history_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Attendance History List
            if (uiState.history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.qr_scanner_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("list_attendance_history"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.history, key = { it.id }) { log ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_attendance_${log.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF2E7D32).copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = log.eventTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (!log.eventLocation.isNullOrBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = log.eventLocation,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        text = log.timestamp,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // QR Generator Dialog (khusus pengurus RT)
    val generatorRtId = uiState.rtId
    if (uiState.showGeneratorDialog && uiState.isAdmin && generatorRtId != null) {
        AttendanceQrGeneratorDialog(
            rtId = generatorRtId,
            onDismiss = { viewModel.setShowGeneratorDialog(false) }
        )
    }

    // Manual Attendance Dialog
    if (uiState.showManualDialog) {
        var manualTitle by remember { mutableStateOf("") }
        var manualLocation by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { viewModel.setShowManualDialog(false) },
            title = { Text(stringResource(R.string.qr_scanner_manual_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.qr_scanner_manual_dialog_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = manualTitle,
                        onValueChange = { manualTitle = it },
                        label = { Text(stringResource(R.string.qr_scanner_event_name)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_manual_event_name")
                    )

                    OutlinedTextField(
                        value = manualLocation,
                        onValueChange = { manualLocation = it },
                        label = { Text(stringResource(R.string.qr_scanner_event_location)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_manual_event_location")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logManualAttendance(manualTitle, manualLocation)
                    },
                    enabled = manualTitle.isNotBlank(),
                    modifier = Modifier.testTag("btn_submit_manual_attendance")
                ) {
                    Text(stringResource(R.string.qr_scanner_btn_record))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowManualDialog(false) }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}
