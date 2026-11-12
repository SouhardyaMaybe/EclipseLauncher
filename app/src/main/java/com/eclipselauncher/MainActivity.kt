package com.eclipselauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.eclipselauncher.ui.navigation.AppNavHost
import com.eclipselauncher.ui.theme.EclipseLauncherTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EclipseLauncherTheme {
                AppNavHost()
            }
        }
    }
}