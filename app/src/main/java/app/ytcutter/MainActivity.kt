package app.ytcutter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import app.ytcutter.data.YtDlpRepo
import app.ytcutter.ui.SettingsScreen
import app.ytcutter.ui.theme.YtCutterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YtCutterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LaunchedEffect(Unit) {
                        runCatching { YtDlpRepo.init(applicationContext) }
                    }
                    SettingsScreen(onBack = {})
                }
            }
        }
    }
}