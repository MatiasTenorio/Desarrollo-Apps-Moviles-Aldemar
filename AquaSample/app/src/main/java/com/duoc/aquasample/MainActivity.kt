package com.duoc.aquasample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.duoc.aquasample.ui.login.LoginScreen
import com.duoc.aquasample.ui.theme.AquaSampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AquaSampleTheme {
                LoginScreen(
                    onLoginExitoso = { rol ->
                        //
                    }
                )
            }
        }
    }
}