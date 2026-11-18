package com.duoc.aquasample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.duoc.aquasample.ui.navigation.AquaSampleApp
import com.duoc.aquasample.ui.theme.AquaSampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AquaSampleTheme {
<<<<<<< HEAD
=======
                // Una sola navegación (antes había dos NavHost dibujados uno encima del otro).
>>>>>>> 8a704e0 (Feat (Develop: Backend y cambios Parcial 3))
                AquaSampleApp()
            }
        }
    }
}
