package com.duoc.aquasample.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.duoc.aquasample.ui.theme.AldemarPrincipal
import java.io.File

/**
 * Cámara a pantalla completa con CameraX. La foto se escribe directo en un
 * archivo de la carpeta de la app (lo entrega crearArchivo) y se devuelve
 * su ruta con onFotoTomada.
 *
 * Usa LifecycleCameraController: se enciende y apaga solo con el ciclo de
 * vida de la pantalla, sin manejar a mano los "use cases" de CameraX.
 */
@Composable
fun CameraCaptureScreen(
    crearArchivo: () -> File,
    onFotoTomada: (String) -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current
    var permisoConcedido by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permisoPedido by remember { mutableStateOf(false) }

    val permisoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        permisoConcedido = concedido
        permisoPedido = true
    }

    LaunchedEffect(Unit) {
        if (!permisoConcedido) permisoLauncher.launch(Manifest.permission.CAMERA)
    }

    when {
        permisoConcedido -> VistaCamara(
            crearArchivo = crearArchivo,
            onFotoTomada = onFotoTomada,
            onCancelar = onCancelar
        )

        permisoPedido -> PermisoDenegado(
            onReintentar = { permisoLauncher.launch(Manifest.permission.CAMERA) },
            onCancelar = onCancelar
        )

        // Mientras el sistema muestra el diálogo de permiso.
        else -> Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
    }
}

@Composable
private fun VistaCamara(
    crearArchivo: () -> File,
    onFotoTomada: (String) -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var capturando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }
    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    this.controller = controller
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = onCancelar,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            error?.let {
                Text(it, color = Color.White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
            }

            // Botón disparador: círculo blanco con borde, como la cámara del sistema.
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(8.dp)
                    .background(if (capturando) Color.Gray else Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (capturando) {
                    CircularProgressIndicator(color = AldemarPrincipal, modifier = Modifier.size(32.dp))
                } else {
                    IconButton(
                        onClick = {
                            capturando = true
                            error = null
                            val archivo = crearArchivo()
                            controller.takePicture(
                                ImageCapture.OutputFileOptions.Builder(archivo).build(),
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(resultado: ImageCapture.OutputFileResults) {
                                        capturando = false
                                        onFotoTomada(archivo.absolutePath)
                                    }

                                    override fun onError(excepcion: ImageCaptureException) {
                                        archivo.delete()
                                        capturando = false
                                        error = "No se pudo tomar la foto. Intenta de nuevo."
                                    }
                                }
                            )
                        },
                        modifier = Modifier.size(60.dp)
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Tomar foto", tint = AldemarPrincipal)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermisoDenegado(
    onReintentar: () -> Unit,
    onCancelar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            "Para tomar la foto de la muestra, AquaSample necesita permiso para usar la cámara.",
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onReintentar,
            colors = ButtonDefaults.buttonColors(containerColor = AldemarPrincipal)
        ) {
            Text("Dar permiso")
        }
        TextButton(onClick = onCancelar) {
            Text("Volver y elegir de la galería")
        }
    }
}
