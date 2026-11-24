package com.duoc.aquasample.ui.admin

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.repository.UsuarioAdministrado
import com.duoc.aquasample.ui.components.SelectorDesplegable
import com.duoc.aquasample.ui.components.Tinta
import com.duoc.aquasample.ui.profile.etiqueta
import com.duoc.aquasample.ui.theme.AldemarPrincipal
import com.duoc.aquasample.ui.theme.AldemarSecundario
import com.duoc.aquasample.ui.theme.AldemarTexto

@Composable
fun AdminUsuariosScreen(
    onBack: () -> Unit,
    viewModel: AdminUsuariosViewModel = viewModel(factory = AdminUsuariosViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    LaunchedEffect(uiState.mensaje) {
        uiState.mensaje?.let {
            Toast.makeText(contexto, it, Toast.LENGTH_SHORT).show()
            viewModel.onMensajeMostrado()
        }
    }

    AdminUsuariosContent(
        uiState = uiState,
        onBack = onBack,
        onRecargar = viewModel::cargar,
        onNuevo = viewModel::onNuevo,
        onEditar = viewModel::onEditar
    )

    uiState.formulario?.let { formulario ->
        DialogoUsuario(
            formulario = formulario,
            onCorreoChange = viewModel::onCorreoChange,
            onNombreChange = viewModel::onNombreChange,
            onContrasenaChange = viewModel::onContrasenaChange,
            onRolChange = viewModel::onRolChange,
            onGuardar = viewModel::guardar,
            onCerrar = viewModel::onCerrarFormulario,
            onPedirBaja = viewModel::onPedirBaja,
            onCancelarBaja = viewModel::onCancelarBaja,
            onConfirmarBaja = viewModel::darDeBaja
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsuariosContent(
    uiState: AdminUsuariosUiState,
    onBack: () -> Unit,
    onRecargar: () -> Unit,
    onNuevo: () -> Unit,
    onEditar: (UsuarioAdministrado) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Usuarios", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (uiState.permitido) {
                        IconButton(onClick = onRecargar) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Recargar usuarios")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.permitido) {
                ExtendedFloatingActionButton(
                    onClick = onNuevo,
                    containerColor = AldemarPrincipal,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
                    text = { Text("Nuevo usuario") },
                    modifier = Modifier.testTag("boton_nuevo_usuario")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when {
                !uiState.permitido -> Text(
                    "Solo el Administrador gestiona usuarios.",
                    color = Tinta.secundaria,
                    modifier = Modifier.padding(24.dp)
                )
                uiState.cargando && uiState.usuarios.isEmpty() -> CircularProgressIndicator()
                uiState.error != null && uiState.usuarios.isEmpty() -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(uiState.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRecargar) { Text("Reintentar") }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Espacio abajo para que el botón flotante no tape el último usuario.
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.usuarios, key = { it.id }) { usuario ->
                        TarjetaUsuario(usuario = usuario, onClick = { onEditar(usuario) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaUsuario(usuario: UsuarioAdministrado, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("usuario_${usuario.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AldemarSecundario, contentColor = AldemarTexto)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(usuario.nombre, fontWeight = FontWeight.Bold, color = AldemarTexto)
                Text(
                    usuario.correo,
                    fontSize = 13.sp,
                    color = AldemarTexto.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                usuario.rol.etiqueta(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AldemarPrincipal,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun DialogoUsuario(
    formulario: FormularioUsuario,
    onCorreoChange: (String) -> Unit,
    onNombreChange: (String) -> Unit,
    onContrasenaChange: (String) -> Unit,
    onRolChange: (Rol) -> Unit,
    onGuardar: () -> Unit,
    onCerrar: () -> Unit,
    onPedirBaja: () -> Unit,
    onCancelarBaja: () -> Unit,
    onConfirmarBaja: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!formulario.guardando) onCerrar() },
        title = { Text(if (formulario.esNuevo) "Nuevo usuario" else "Editar usuario") },
        text = {
            // Desliza si el teclado deja poco espacio.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = formulario.correo,
                    onValueChange = onCorreoChange,
                    label = { Text("Correo") },
                    singleLine = true,
                    // El correo identifica a la cuenta de Insforge: no se cambia al editar.
                    enabled = formulario.esNuevo,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().testTag("campo_correo_usuario")
                )
                OutlinedTextField(
                    value = formulario.nombre,
                    onValueChange = onNombreChange,
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("campo_nombre_usuario")
                )
                if (formulario.esNuevo) {
                    OutlinedTextField(
                        value = formulario.contrasena,
                        onValueChange = onContrasenaChange,
                        label = { Text("Contraseña inicial") },
                        supportingText = { Text("Mínimo $LARGO_MINIMO_CONTRASENA caracteres. Se guarda solo en Insforge.") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().testTag("campo_contrasena_usuario")
                    )
                }
                SelectorDesplegable(
                    etiqueta = "Rol",
                    opciones = Rol.entries.toList(),
                    seleccionada = formulario.rol,
                    onSeleccion = onRolChange,
                    textoDe = { it.etiqueta() }
                )
                formulario.errores.forEach { error ->
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                // Dar de baja: solo al editar, nunca la propia cuenta, y con confirmación.
                if (!formulario.esNuevo && formulario.puedeDarDeBaja) {
                    HorizontalDivider()
                    if (formulario.confirmandoBaja) {
                        Text(
                            "¿Dar de baja a ${formulario.correo}? Se borra su cuenta de Insforge y ya no podrá " +
                                "iniciar sesión. Sus muestras se conservan.",
                            fontSize = 13.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onCancelarBaja, enabled = !formulario.guardando) { Text("No") }
                            Button(
                                onClick = onConfirmarBaja,
                                enabled = !formulario.guardando,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("boton_confirmar_baja")
                            ) { Text("Sí, dar de baja") }
                        }
                    } else {
                        TextButton(
                            onClick = onPedirBaja,
                            enabled = !formulario.guardando,
                            modifier = Modifier.testTag("boton_dar_de_baja")
                        ) {
                            Text("Dar de baja", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onGuardar, enabled = !formulario.guardando, modifier = Modifier.testTag("boton_guardar_usuario")) {
                Text(if (formulario.guardando) "Guardando..." else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar, enabled = !formulario.guardando) { Text("Cancelar") }
        }
    )
}
