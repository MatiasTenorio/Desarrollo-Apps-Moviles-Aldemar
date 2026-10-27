package com.duoc.aquasample.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.duoc.aquasample.ui.theme.AldemarSecundario
import com.duoc.aquasample.ui.theme.AldemarTexto

/**
 * Tarjeta de estadística simple (etiqueta + valor grande). Usada en
 * "Detalle de muestra" (Cantidad de choritos / Tamaño promedio) y en
 * "Perfil" (Muestras hoy / Muestras totales).
 *
 * Si SampleDetailScreen.kt todavía tiene su propia copia privada de
 * StatCard, bórrala y usa este import en su lugar para no duplicar código:
 * import com.duoc.aquasample.ui.components.StatCard
 */
@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AldemarSecundario)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AldemarTexto)
        }
    }
}
