package com.lan.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.lan.scanner.ui.ScanScreen
import com.lan.scanner.ui.theme.LanScannerTheme

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
