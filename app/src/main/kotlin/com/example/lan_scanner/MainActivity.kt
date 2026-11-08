package com.example.lan_scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.lan_scanner.ui.ScanScreen
import com.example.lan_scanner.ui.theme.LanScannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LanScannerTheme {
                ScanScreen()
            }
        }
    }
}
