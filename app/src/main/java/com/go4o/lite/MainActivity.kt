package com.go4o.lite

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.go4o.lite.ui.screens.MainScreen
import com.go4o.lite.ui.theme.Go4OLiteTheme
import com.go4o.lite.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Go4OLiteTheme {
                val viewModel: MainViewModel = viewModel()
                viewModel.initStationManager(this)
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(uiState.settings.keepScreenOn) {
                    if (uiState.settings.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                MainScreen(
                    uiState = uiState,
                    onImportCourses = { uri -> viewModel.importCourses(this, uri) },
                    onConnect = { viewModel.connectStation() },
                    onDisconnect = { viewModel.disconnectStation() },
                    onDismissResult = { viewModel.dismissResult() },
                    onClearError = { viewModel.clearError() },
                    onClearHistory = { viewModel.clearHistory() },
                    onClearAll = { viewModel.clearAll() },
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onPreviewResult = { viewModel.previewResult(it) },
                    onDeleteCourse = { viewModel.deleteCourse(it) },
                    onUpdateCourse = { i, c -> viewModel.updateCourse(i, c) }
                )
            }
        }
    }
}
