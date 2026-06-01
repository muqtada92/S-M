package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ChatScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.ChatViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: ChatViewModel = viewModel(
                    factory = ChatViewModelFactory(applicationContext)
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFFEF7FF) // Clean Minimalist light theme color
                ) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        ChatScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
