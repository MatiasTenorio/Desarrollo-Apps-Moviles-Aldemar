package com.duoc.aquasample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.duoc.aquasample.ui.theme.AldemarPrincipal

/**
 * Indicador de pasos reutilizable para el asistente de "Nueva muestra" (3 pasos:
 * Línea y fotos / Conteo y revisión / Guardar). currentStep es 1-indexado.
 *
 * Pensado para reutilizarse tal cual en los 3 pasos del wizard; solo cambia
 * el valor de currentStep que le pases.
 */
@Composable
fun SampleWizardStepper(
    currentStep: Int,
    stepLabels: List<String>
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            stepLabels.forEachIndexed { index, _ ->
                val stepNumber = index + 1
                val isActiveOrDone = stepNumber <= currentStep

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (isActiveOrDone) AldemarPrincipal else Color.LightGray,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (index != stepLabels.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(if (stepNumber < currentStep) AldemarPrincipal else Color.LightGray)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            stepLabels.forEachIndexed { index, label ->
                val stepNumber = index + 1
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (stepNumber <= currentStep) AldemarPrincipal else Color.Gray,
                    fontWeight = if (stepNumber == currentStep) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
