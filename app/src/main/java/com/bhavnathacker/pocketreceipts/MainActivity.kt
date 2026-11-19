package com.bhavnathacker.pocketreceipts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bhavnathacker.pocketreceipts.core.ui.theme.PocketReceiptsTheme
import com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation.ModelSetupGate
import dagger.hilt.android.AndroidEntryPoint

/**
 * The app's only activity. The app shell is shown once the on-device model is
 * set up; until then [ModelSetupGate] shows model setup, and back closes the app.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketReceiptsTheme {
                ModelSetupGate {
                    PocketReceiptsApp()
                }
            }
        }
    }
}
