package com.lintas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.lintas.app.ui.navigation.LintasNavGraph
import com.lintas.app.ui.theme.LintasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LintasTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LintasNavGraph()
                }
            }
        }
    }
}
