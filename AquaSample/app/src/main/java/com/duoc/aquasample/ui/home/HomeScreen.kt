package com.duoc.aquasample.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquasample.ui.theme.AldemarPrincipal
import com.duoc.aquasample.ui.theme.AldemarSecundario
import com.duoc.aquasample.ui.theme.AldemarTexto

/**
 * Pantalla "Inicio de aplicación" (Home).
 *
 * Según la Evidencia de Clase 2 (actualizada): usa TopAppBar + NavigationBar inferior
 * + Cards como accesos directos a las 3 secciones principales (Nueva muestra, Historial, Perfil).
 *
 * Navegación: se expone mediante lambdas (onNavigateToX) en vez de depender de un
 * NavController o Screen.kt específico, para que sea fácil de enganchar a cualquier
 * NavGraph.kt que ya exista en el proyecto sin tener que coincidir en nombres de rutas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToNewSample: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onOpenMenu: () -> Unit = {} // TODO: definir qué abre el ícono de menú (¿Drawer con cerrar sesión / configuración?)
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(HomeTab.INICIO) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menú", tint = Color.White)
                    }
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // TODO: reemplazar por el logo real, p.ej.
                        // Image(painterResource(R.drawable.logo_aldemar), contentDescription = null, modifier = Modifier.size(28.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Aldemar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Muestras", color = Color.White, fontSize = 11.sp)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AldemarPrincipal)
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == HomeTab.INICIO,
                    onClick = { selectedTab = HomeTab.INICIO },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") }
                )
                NavigationBarItem(
                    selected = selectedTab == HomeTab.NUEVA_MUESTRA,
                    onClick = {
                        selectedTab = HomeTab.NUEVA_MUESTRA
                        onNavigateToNewSample()
                    },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Nueva muestra") },
                    label = { Text("Nueva muestra") }
                )
                NavigationBarItem(
                    selected = selectedTab == HomeTab.HISTORIAL,
                    onClick = {
                        selectedTab = HomeTab.HISTORIAL
                        onNavigateToHistory()
                    },
                    icon = { Icon(Icons.Default.History, contentDescription = "Historial") },
                    label = { Text("Historial") }
                )
                NavigationBarItem(
                    selected = selectedTab == HomeTab.PERFIL,
                    onClick = {
                        selectedTab = HomeTab.PERFIL
                        onNavigateToProfile()
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") }
                )
                // TODO (pendiente de decisión de equipo): si se confirma una 4ta opción
                // exclusiva para el rol Analista, agregar aquí un NavigationBarItem
                // condicionado a uiState.userRole == Rol.ANALISTA.
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Hola, ${uiState.userName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AldemarTexto
            )
            Text(
                text = "¿Qué quieres hacer hoy?",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Card principal: Nueva muestra
            Card(
                onClick = onNavigateToNewSample,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AldemarPrincipal)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Nueva muestra", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Registra una nueva línea de choritos",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fila: Historial + Perfil
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HomeSecondaryCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ListAlt,
                    title = "Historial",
                    subtitle = "Revisa tus muestras registradas",
                    onClick = onNavigateToHistory
                )
                HomeSecondaryCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Person,
                    title = "Perfil",
                    subtitle = "Gestiona tu cuenta y preferencias",
                    onClick = onNavigateToProfile
                )
            }
        }
    }
}

@Composable
private fun HomeSecondaryCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(150.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AldemarSecundario)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = AldemarTexto)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = AldemarTexto)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

private enum class HomeTab { INICIO, NUEVA_MUESTRA, HISTORIAL, PERFIL }

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(viewModel = HomeViewModel(userName = "Matías"))
}
