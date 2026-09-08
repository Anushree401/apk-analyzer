package com.example.android_manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.android_manager.navigation.AppNavigation
import com.example.android_manager.ui.theme.Android_ManagerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            Android_ManagerTheme {
                AppNavigation()
            }
        }
    }
}