package com.duoc.aquasample.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquasample.R
import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.ui.theme.AldemarFondo
import com.duoc.aquasample.ui.theme.AldemarPrincipal
import com.duoc.aquasample.ui.theme.AldemarTexto
import com.duoc.aquasample.ui.theme.AquaSampleTheme


// Forma del panel blanco: borde superior ondulado
private val OndaSuperiorShape = GenericShape { size, _ ->
    val amp = size.height * 0.05f
    moveTo(0f, amp)
    cubicTo(size.width * 0.30f, 0f, size.width * 0.55f, amp * 2f, size.width, amp)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

@Composable
fun LoginScreen(
    onLoginExitoso: (Rol) -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Cuando el login es válido, avisamos hacia afuera (navegación) y consumimos el evento
    LaunchedEffect(state.rolAutenticado) {
        state.rolAutenticado?.let { rol ->
            onLoginExitoso(rol)
            viewModel.loginConsumido()
        }
    }

    LoginContent(
        state = state,
        onUsuarioChange = viewModel::onUsuarioChange,
        onContrasenaChange = viewModel::onContrasenaChange,
        onToggleVisible = viewModel::toggleContrasenaVisible,
        onIniciarSesion = viewModel::iniciarSesion
    )
}

@Composable
private fun LoginContent(
    state: LoginUiState,
    onUsuarioChange: (String) -> Unit,
    onContrasenaChange: (String) -> Unit,
    onToggleVisible: () -> Unit,
    onIniciarSesion: () -> Unit
) {
    val colorCampo = AldemarFondo.copy(alpha = 0.3f)
    val campoColors = TextFieldDefaults.colors(
        focusedContainerColor = colorCampo,
        unfocusedContainerColor = colorCampo,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        focusedTextColor = AldemarTexto,
        unfocusedTextColor = AldemarTexto,
        focusedLeadingIconColor = AldemarTexto,
        unfocusedLeadingIconColor = AldemarTexto,
        focusedTrailingIconColor = AldemarTexto,
        unfocusedTrailingIconColor = AldemarTexto
    )
    val formaCampo = RoundedCornerShape(16.dp)

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {

        // ---------- Mitad superior: degradado azul + logo ----------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .background(
                    Brush.verticalGradient(
                        listOf(lerp(AldemarPrincipal, Color.Black, 0.25f), AldemarPrincipal)
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo Aldemar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(110.dp).clip(CircleShape)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Aldemar",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            Text(text = "Muestras", color = Color.White, fontSize = 20.sp)
        }

        // ---------- Mitad inferior: panel blanco con onda ----------
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.58f)
                .background(Color.White, OndaSuperiorShape)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 24.dp)
        ) {
            Text(
                text = "Bienvenido/a",
                color = AldemarTexto,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Inicia sesión para continuar",
                color = AldemarTexto.copy(alpha = 0.6f),
                fontSize = 14.sp
            )

            Spacer(Modifier.height(24.dp))

            TextField(
                value = state.usuario,
                onValueChange = onUsuarioChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Usuario") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                shape = formaCampo,
                colors = campoColors,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(Modifier.height(16.dp))

            TextField(
                value = state.contrasena,
                onValueChange = onContrasenaChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = onToggleVisible) {
                        Icon(
                            imageVector = if (state.contrasenaVisible)
                                Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (state.contrasenaVisible)
                                "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                },
                visualTransformation = if (state.contrasenaVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = formaCampo,
                colors = campoColors,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onIniciarSesion() })
            )

            // Mensaje de error (solo si existe)
            state.error?.let { mensaje ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = mensaje,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onIniciarSesion,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AldemarPrincipal,
                    contentColor = Color.White
                )
            ) {
                Text("Iniciar sesión", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = { /* TODO: pantalla de recuperación de contraseña */ },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    color = AldemarPrincipal,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginPreview() {
    AquaSampleTheme {
        LoginContent(
            state = LoginUiState(),
            onUsuarioChange = {},
            onContrasenaChange = {},
            onToggleVisible = {},
            onIniciarSesion = {}
        )
    }
}